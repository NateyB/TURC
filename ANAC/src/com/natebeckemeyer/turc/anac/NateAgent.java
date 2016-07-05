package com.natebeckemeyer.turc.anac;

import javafx.util.Pair;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Deadline;
import negotiator.DiscreteTimeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.*;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.session.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;

import java.util.*;

/**
 * @author Nate Beckemeyer
 *         <p>
 *         This class is my ANAC agent implementation.
 */
public class NateAgent extends AbstractNegotiationParty
{
    /**
     * This flag determines whether, at certain points, output is necessary. Error messages are always generated,
     * but setting this flag to true silences intermediary noise.
     */
    private static final boolean verbose = false;

    /**
     * The history of each partner's agent coded to its ID.
     */
    private HashMap<AgentID, Opponent> opponents = new HashMap<>();

    /**
     * The agent that this agent interacted with most recently.
     */
    private AgentID lastAgent;

    /**
     * The mapping from each objective being discussed to a pair of its weight and a hashmap of its possible values to
     * their weights.
     */
    private HashMap<Objective, Pair<Double, HashMap<Value, Double>>> utilities = new HashMap<>();

    /**
     * The utility space for this agent.
     */
    private AdditiveUtilitySpace mainUtilitySpace;

    /**
     * The minimum utility acceptable for the random bid.
     */
    private double minUtilityRandom = 0.0;

    /**
     * init is called when a next session starts with the same opponent.
     * In the case of this agent, init calculates the utilities of all of the
     */
    public void init(AbstractUtilitySpace util, Deadline deadline, TimeLineInfo info, long randomSeed, AgentID id)
    {
        super.init(util, deadline, info, randomSeed, id);

        HashMap<Objective, Evaluator> itemUtilities = new HashMap<>();
        mainUtilitySpace = ((AdditiveUtilitySpace) utilitySpace);

        if (mainUtilitySpace != null && mainUtilitySpace.getNrOfEvaluators() > 0)
            mainUtilitySpace.getEvaluators().forEach(entry -> itemUtilities.put(entry.getKey(), entry.getValue()));
        else
        {
            System.err.println("Cannot use " + getName() + getVersion() + " with non-linear utility space.");
            System.exit(16);
        }

        for (Map.Entry<Objective, Evaluator> entryVal : itemUtilities.entrySet())
        {
            Objective issue = entryVal.getKey();
            Evaluator issueEval = entryVal.getValue();
            utilities.put(issue, new Pair<>(issueEval.getWeight(), new HashMap<>()));

            switch (issueEval.getType())
            {

                case DISCRETE:
                    EvaluatorDiscrete evaluatorDiscrete = ((EvaluatorDiscrete) issueEval);
                    evaluatorDiscrete.getValues().forEach(valueDiscrete -> utilities.get(issue).getValue()
                            .put(valueDiscrete, evaluatorDiscrete.getValue(valueDiscrete).doubleValue()));
                    break;

                case REAL:
                case INTEGER:
                default:
                    System.err.println(getName() + getVersion() + " requires exclusively discrete evaluators.");
                    break;
            }
            for (HashMap.Entry<Objective, Pair<Double, HashMap<Value, Double>>> map : utilities.entrySet())
                utilities.put(map.getKey(), new Pair<>(map.getValue().getKey(), normalize(map.getValue().getValue())));
        }

        if (verbose)
            for (HashMap.Entry<Objective, Pair<Double, HashMap<Value, Double>>> entry : utilities.entrySet())
            {
                System.out.printf("The values for item %s, weighted %f, are as follows:%n", entry.getKey().getName(),
                        entry.getValue().getKey());
                for (Map.Entry<Value, Double> value : entry.getValue().getValue().entrySet())
                    System.out.printf("%5sSub-item: %25s has value %10f%n", "", value.getKey().toString(),
                            value.getValue());
                System.out.println();
            }
    }

    public String getVersion()
    {
        return "2.0";
    }


    public String getName()
    {
        return "MeanBot";
    }

    /**
     * Wrapper for getRandomBid, for convenience.
     *
     * @return new Action(Bid(..)), with bid utility > MINIMUM_BID_UTIL. If a
     * problem occurs, it returns an Accept() action.
     */
    private Action chooseRandomBidAction()
    {
        Bid nextBid = null;
        try
        {
            nextBid = getRandomBid();
        } catch (Exception e)
        {
            System.err.println("Problem with received bid:" + e.getMessage()
                    + ". cancelling bidding");
        }
        if (nextBid == null)
            return (new Accept());
        return (new Offer(nextBid));
    }

    /**
     * @return a random bid with high enough utility value.
     * @throws Exception if we can't compute the utility (eg no evaluators have been
     *                   set) or when other evaluators than a DiscreteEvaluator are
     *                   present in the util space.
     */
    private Bid getRandomBid() throws Exception
    {
        HashMap<Integer, Value> values = new HashMap<>(); // pairs
        // <issuenumber,chosen, value string>
        ArrayList<Issue> issues = utilitySpace.getDomain().getIssues();
        Random randomnr = new Random();

        // create a random bid with utility>MINIMUM_BID_UTIL.
        // note that this may never succeed if you set MINIMUM too high!!!
        // in that case we will search for a bid till the time is up (3 minutes)
        // but this is just a simple agent.
        Bid bid;
        do
        {
            for (Issue lIssue : issues)
            {
                switch (lIssue.getType())
                {
                    case DISCRETE:
                        IssueDiscrete lIssueDiscrete = (IssueDiscrete) lIssue;
                        int optionIndex = randomnr.nextInt(lIssueDiscrete.getNumberOfValues());
                        values.put(lIssue.getNumber(), lIssueDiscrete.getValue(optionIndex));
                        break;

                    case REAL:
                        IssueReal lIssueReal = (IssueReal) lIssue;
                        int optionInd = randomnr.nextInt(lIssueReal.getNumberOfDiscretizationSteps() - 1);
                        double realValue = lIssueReal.getLowerBound() +
                                (lIssueReal.getUpperBound() - lIssueReal.getLowerBound()) *
                                        (optionInd / (double) lIssueReal.getNumberOfDiscretizationSteps());
                        values.put(lIssueReal.getNumber(), new ValueReal(realValue));
                        break;

                    case INTEGER:
                        IssueInteger lIssueInteger = (IssueInteger) lIssue;
                        int optionIndex2 = lIssueInteger.getLowerBound() + randomnr.nextInt(
                                lIssueInteger.getUpperBound() - lIssueInteger.getLowerBound());
                        values.put(lIssueInteger.getNumber(), new ValueInteger(optionIndex2));
                        break;


                    default:
                        throw new Exception("issue type " + lIssue.getType()
                                + " not supported by SimpleAgent2");
                }
            }
            bid = new Bid(utilitySpace.getDomain(), values);

        } while (getUtility(bid) < minUtilityRandom);

        return bid;
    }

    static <K> HashMap<K, Double> normalize(Map<K, Double> doubleMap)
    {
        double sum = 0;
        for (double val : doubleMap.values())
            sum += val;

        HashMap<K, Double> map = new HashMap<>(doubleMap.size());
        final double total = sum;

        if (total != 0)
            doubleMap.keySet().stream().forEach(k -> map.put(k, doubleMap.get(k) / total));
        else
            doubleMap.keySet().stream().forEach(k -> map.put(k, 1. / doubleMap.entrySet().size()));

        return map;
    }

    /**
     * This method assumes that, as time goes on, my assessment of the true utilities of my opponents becomes more
     * accurate; however, this assumption is likely not well-founded (because the exploration side of my agent is
     * lacking).
     *
     * @return The perceived upper-end probability of constructing a deal at this point in time.
     */
    private double getDealProbability()
    {
        return timeline.getTime();
    }

    /**
     * Calculates utility as a function of social welfare.
     *
     * @return The best known utility for the current timestep.
     */
    private double getUtility()
    {
        double util = 0;
        try
        {
            HashMap<Objective, Value> valueMapping = maximizeSocialWelfare();
            for (Objective issue : valueMapping.keySet())
                for (Opponent opponent : opponents.values())
                    util += opponent.getEstimatedUtilities().get(issue).getValue().get(valueMapping.get(issue));
        } catch (NullPointerException e)
        {
            System.err.println("Could not get utility at time " + timeline.getTime());
            return 0;
        }

        return util;
    }

    private HashMap<Objective, Value> maximizeSocialWelfare()
    {

        HashMap<Objective, Value> mapping = new HashMap<>();

        for (Objective issue : utilities.keySet())
        {
            Value argmax = null;
            double soWel = 0;

            for (Value current : utilities.get(issue).getValue().keySet())
            {
                double welfare = 0;

                for (Opponent opponent : opponents.values())
                    welfare += opponent.getEstimatedUtilities().get(issue).getValue().getOrDefault(current, 0.);

                if (welfare > soWel || argmax == null)
                {
                    argmax = current;
                    soWel = welfare;
                }
            }

            mapping.put(issue, argmax);
        }

        return mapping;
    }

    private double getDecay(double time)
    {
        return Math.exp(-5 * time);
    }

    private double getUpperDealValue()
    {
        double probability = getDealProbability();
        double upperUtility = getUtility();

        return upperUtility * probability;
    }

    private double getUpperNextDeal()
    {
        double difference = getDecay((timeline.getCurrentTime() + 1) / timeline.getTotalTime());

        double utility = getUtility() + difference;
        double nextProbability = getDealProbability() - timeline.getTotalTime() / (timeline.getCurrentTime() + 1);
        double discount = utilitySpace.getDiscountFactor();

        switch (timeline.getType())
        {
            case Time:
                if (timeline.getCurrentTime() >= .99)
                    return 0;
                break;

            case Rounds:
                if (((DiscreteTimeline) timeline).getOwnRoundsLeft() <= 0)
                    return 0;
                break;

            default:
                System.err.println(
                        getName() + getVersion() + " is not compatible with a timeline of type " + timeline.getType());
                System.exit(16);
                return 0;
        }

        return discount * nextProbability * utility;
    }

    private Bid maximizeSocialWelfareBid()
    {
        HashMap<Objective, Value> soWelMap = maximizeSocialWelfare();

        HashMap<Integer, Value> bidMap = new HashMap<>();
        soWelMap.forEach((objective, value) -> bidMap.put(objective.getNumber(), value));

        return new Bid(utilitySpace.getDomain(), bidMap);
    }

    @Override
    public void receiveMessage(AgentID sender, Action arguments)
    {
        lastAgent = sender;

        opponents.putIfAbsent(sender, new Opponent(sender, mainUtilitySpace));
        opponents.get(sender).addAction(arguments);
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> list)
    {
        Action action = null;
        try
        {
            double EUDeal = getUpperDealValue();
            double EUNeal = getUpperNextDeal();
            minUtilityRandom = EUNeal;

            if (lastAgent == null || opponents.get(lastAgent).getLastAction() == null)
                if (EUDeal > EUNeal)
                    action = new Offer(maximizeSocialWelfareBid());
                else
                    action = new Offer(getRandomBid());

            else if (opponents.get(lastAgent).getLastAction() instanceof Offer)
            {
                Bid partnerBid = ((Offer) opponents.get(lastAgent).getLastAction()).getBid();
                double offeredUtilFromOpponent = getUtility(partnerBid);

                if (offeredUtilFromOpponent > EUNeal)
                    return new Accept();
                else
                    return new Offer(getRandomBid());
            }

            if (EUDeal > EUNeal)
                action = new Offer(maximizeSocialWelfareBid());
            else
                action = new Offer(getRandomBid());

        } catch (Exception e)
        {
            System.err.println("Exception in ChooseAction:" + e.getMessage());
            action = new Accept(); // best guess if things go wrong.
        }
        return action;
    }
}

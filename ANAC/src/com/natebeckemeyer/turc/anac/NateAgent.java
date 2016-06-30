package com.natebeckemeyer.turc.anac;

import javafx.util.Pair;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Deadline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Inform;
import negotiator.actions.Offer;
import negotiator.issue.*;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.session.TimeLineInfo;
import negotiator.utility.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author W.Pasman Some improvements over the standard SimpleAgent.
 *         <p>
 *         Random Walker, Zero Intelligence Agent
 *         Being modified slowly by Nate.
 */
public class NateAgent extends AbstractNegotiationParty
{
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
     * The utility space for this agent across preference profiles.
     */
    private static AdditiveUtilitySpace mainUtilitySpace;

    /**
     * init is called when a next session starts with the same opponent.
     * In the case of this agent, init calculates the utilities of all of the
     */
    public void init(AbstractUtilitySpace util, Deadline deadline, TimeLineInfo info, long randomSeed, AgentID id)
    {
        super.init(util, deadline, info, randomSeed, id);
        HashMap<Objective, Evaluator> itemUtilities = new HashMap<>();
        mainUtilitySpace = ((AdditiveUtilitySpace) utilitySpace);

        if (mainUtilitySpace != null && mainUtilitySpace.getNrOfEvaluators() >= 0 &&
                mainUtilitySpace.getEvaluators() != null)
            mainUtilitySpace.getEvaluators().forEach(entry -> itemUtilities.put(entry.getKey(), entry.getValue()));

        for (Map.Entry<Objective, Evaluator> entryVal : itemUtilities.entrySet())
        {
            Objective key = entryVal.getKey();
            Evaluator value = entryVal.getValue();
            utilities.put(key, new Pair<>(value.getWeight(), new HashMap<>()));

            switch (value.getType())
            {
                case REAL:
                    EvaluatorReal evaluatorReal = ((EvaluatorReal) value);
                    switch (evaluatorReal.getFuncType())
                    {
                        case LINEAR:
                        case TRIANGULAR:
                        case TRIANGULAR_VARIABLE_TOP:
                        case CONSTANT:
                        case FARATIN:
                    }
                    System.err.println(getName() + getVersion() + " is unprepared for real valuation functions.");
                    break;

                case INTEGER:
                    EvaluatorInteger evaluatorInteger = ((EvaluatorInteger) value);
                    switch (evaluatorInteger.getFuncType())
                    {
                        case LINEAR:
                        case TRIANGULAR:
                        case TRIANGULAR_VARIABLE_TOP:
                        case CONSTANT:
                        case FARATIN:
                    }
                    System.err.println(getName() + getVersion() + " is unprepared for integer valuation functions.");
                    break;

                case DISCRETE:
                    EvaluatorDiscrete evaluatorDiscrete = ((EvaluatorDiscrete) value);
                    evaluatorDiscrete.getValues().forEach(valueDiscrete -> utilities.get(key).getValue()
                            .put(valueDiscrete, evaluatorDiscrete.getValue(valueDiscrete).doubleValue()));
                    break;

                default:
                    System.err.println(
                            "Nate wasn't prepared for this moment. Value neither real, integer, nor discrete.");
                    break;
            }
            for (HashMap.Entry<Objective, Pair<Double, HashMap<Value, Double>>> map : utilities.entrySet())
                utilities.put(map.getKey(), new Pair<>(map.getValue().getKey(), normalize(map.getValue().getValue())));
        }

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

    private boolean isAcceptable(double offeredUtilFromOpponent,
                                 double myOfferedUtil, double time) throws Exception
    {
        double P = Paccept(offeredUtilFromOpponent, time);
        if (P > Math.random())
            return true;
        return false;
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
            System.out.println("Problem with received bid:" + e.getMessage()
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
        HashMap<Integer, Value> values = new HashMap<Integer, Value>(); // pairs
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

        } while (getUtility(bid) < 0.0);

        return bid;
    }

    /**
     * This function determines the accept probability for an offer. At t=0 it
     * will prefer high-utility offers. As t gets closer to 1, it will accept
     * lower utility offers with increasing probability. it will never accept
     * offers with utility 0.
     *
     * @param u  is the utility
     * @param t1 is the time as fraction of the total available time (t=0 at
     *           start, and t=1 at end time)
     * @return the probability of an accept at time t
     * @throws Exception if you use wrong values for u or t.
     */
    double Paccept(double u, double t1) throws Exception
    {
        double t = t1 * t1 * t1; // steeper increase when deadline approaches.
        if (u < 0 || u > 1.05)
            throw new Exception("utility " + u + " outside [0,1]");
        // normalization may be slightly off, therefore we have a broad boundary
        // up to 1.05
        if (t < 0 || t > 1)
            throw new Exception("time " + t + " outside [0,1]");
        if (u > 1.)
            u = 1;
        if (t == 0.5)
            return u;
        return (u - 2. * u * t + 2. * (-1. + t + Math.sqrt(sq(-1. + t) + u
                * (-1. + 2 * t))))
                / (-1. + 2 * t);
    }

    double sq(double x)
    {
        return x * x;
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
     * @return A mapping from the objectives to their possible values.
     */
    HashMap<Objective, ArrayList<String>> getPreferenceDomain()
    {
        HashMap<Objective, ArrayList<String>> mapping = new HashMap<>();
        for (Map.Entry<Objective, Evaluator> entry : mainUtilitySpace.getEvaluators())
            mapping.put(entry.getKey(), new ArrayList<>(
                    ((EvaluatorDiscrete) entry.getValue()).getValues().stream().map(ValueDiscrete::getValue)
                            .collect(Collectors.toList())));

        return mapping;
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> list)
    {
        Action action = null;
        try
        {
            if (lastAgent == null || opponents.get(lastAgent).getLastAction() == null)
                action = new Offer(utilitySpace.getMaxUtilityBid());
            else if (opponents.get(lastAgent).getLastAction() instanceof Offer)
            {
                Bid partnerBid = ((Offer) opponents.get(lastAgent).getLastAction()).getBid();
                double offeredUtilFromOpponent = getUtility(partnerBid);

                // get current time
                double time = timeline.getTime();
                action = chooseRandomBidAction();

                Bid myBid = ((Offer) action).getBid();
                double myOfferedUtil = getUtility(myBid);

                // accept under certain circumstances
                if (isAcceptable(offeredUtilFromOpponent, myOfferedUtil, time))
                    action = new Accept();

            }
        } catch (Exception e)
        {
            System.out.println("Exception in ChooseAction:" + e.getMessage());
            action = new Accept(); // best guess if things go wrong.
        }
        return action;

    }

    public void receiveMessage(AgentID sender, Action arguments) {
        lastAgent = sender;

        opponents.putIfAbsent(sender, new Opponent(sender, mainUtilitySpace));
        opponents.get(sender).addAction(arguments);

    }
}

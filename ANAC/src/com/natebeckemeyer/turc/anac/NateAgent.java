package com.natebeckemeyer.turc.anac;

import javafx.util.Pair;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Deadline;
import negotiator.DiscreteTimeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Inform;
import negotiator.actions.Offer;
import negotiator.issue.Objective;
import negotiator.issue.Value;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.session.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;

import java.util.*;
import java.util.stream.Collectors;

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
     * Defines the social welfare-maximizing strategy that this agent will use.
     */
    private static final BidStrategy thisStrategy = BidStrategy.PRODUCT;

    /**
     * The history of each partner's agent coded to its ID.
     */
    private HashMap<AgentID, Opponent> opponents = new HashMap<>();

    /**
     * The agent that this agent interacted with most recently.
     */
    private AgentID lastAgent;

    /**
     * The number of parties involved in the negotiation.
     */
    private int numberOfParties;

    /**
     * The mapping from each objective being discussed to a pair of its weight and a hashmap of its possible values to
     * their weights. The structure of this map is as follows:
     * <pre>
     * {@code Objective --> Pair}
     *
     * {@code Pair --> (Weight, SubIssueMap)} where
     * {@code Weight} is the weight of the issue
     *
     * {@code SubIssueMap --> (Value, Utility)} where
     * {@code Value} is the possible discrete value and
     * {@code Utility} is the utility gained from its selection
     * </pre>
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

    private ArrayList<Bid> bidList;

    public int getNumberOfParties()
    {
        if (verbose)
            System.out.println(numberOfParties);

        return numberOfParties > 0 ? numberOfParties : 3;//numberOfParties;
    }

    private void initializeUtilities()
    {
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

    /**
     * init is called when a next session starts with the same opponent.
     * In the case of this agent, init calculates the utilities of all of the
     */
    public void init(AbstractUtilitySpace util, Deadline deadline, TimeLineInfo info, long randomSeed, AgentID id)
    {
        super.init(util, deadline, info, randomSeed, id);
        initializeUtilities();
        initializeBidList();
    }

    private void initializeBidList()
    {
        bidList = new ArrayList<>();
        bidList.addAll(performPermutations());
    }

    private List<Bid> performPermutations()
    {
        return performPermutations(new HashMap<Integer, Value>(), new LinkedList<>(), 1);
    }

    private List<Bid> performPermutations(HashMap<Integer, Value> progress, List<Bid> others, Integer issue)
    {
        if (issue > utilities.keySet().size())
        {
            others.add(new Bid(utilitySpace.getDomain(), progress));
            return others;
        }

        EvaluatorDiscrete evaluator = (EvaluatorDiscrete) mainUtilitySpace.getEvaluator(issue);
        for (Value valueDiscrete : evaluator.getValues())
        {
            progress.put(issue, valueDiscrete);
            others = performPermutations(new HashMap<>(progress), others, issue + 1);
        }

        return others;
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
     * @return a random bid with high enough utility value.
     * @throws Exception if we can't compute the utility (eg no evaluators have been
     *                   set) or when other evaluators than a DiscreteEvaluator are
     *                   present in the util space.
     */
    private Bid getRandomBid() throws Exception
    {
        List<Bid> plausibleBids = bidList.stream().filter(
                bid -> getUtility(bid) > minUtilityRandom).collect(
                Collectors.toList());

        return plausibleBids.size() > 0 ? plausibleBids.get(rand.nextInt(plausibleBids.size())) : utilitySpace
                .getMaxUtilityBid();

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
        return Math.pow(timeline.getTime(), 3);
    }

    private double getDecay(double time)
    {
        return Math.pow(1 - time, 3);
    }

    /**
     * Calculates utility of a bid as a function of social welfare. Returns a number in the range [0,1].
     *
     * @return The best known utility for the current timestep.
     */
    private double calculateActualUtility(Bid bid)
    {
        // Get my utility
        double util = getUtility(bid);
        try
        {
            HashMap<Objective, Value> valueMapping = new HashMap<>();
            mainUtilitySpace.getEvaluators().forEach(
                    entry -> valueMapping.put(entry.getKey(), bid.getValue(entry.getKey().getNumber())));

            for (Objective issue : valueMapping.keySet())
                for (Opponent opponent : opponents.values())
                {
                    double opponentUtil = opponent.getEstimatedUtilities().get(issue).getValue().get(
                            valueMapping.get(issue));
                    switch (thisStrategy)
                    {
                        case PRODUCT:
                            util *= opponentUtil;
                            break;

                        case SUM:
                            util += opponentUtil;
                            break;
                    }
                }
        } catch (NullPointerException e)
        {
            System.err.println("Could not get utility at time " + timeline.getTime());
            return 0;
        }

        // Average social welfare
        switch (thisStrategy)
        {
            case SUM:
                return util / getNumberOfParties();

            case PRODUCT:
                double newUtil = Math.pow(util, 1. / getNumberOfParties());
                return newUtil;

            default:
                System.err.printf("%s could not handle strategy of type %s.%n", getName() + getVersion(), thisStrategy);
                return 0;
        }
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
                // My utility for this value of this issue.
                double welfare = utilities.get(issue).getValue().get(current);

                for (Opponent opponent : opponents.values())
                {
                    double opponentUtil = opponent.getEstimatedUtilities().get(issue).getValue().getOrDefault(current,
                            0.);
                    switch (thisStrategy)
                    {
                        case PRODUCT:
                            welfare *= opponentUtil;
                            break;

                        case SUM:
                            welfare += opponentUtil;
                            break;
                    }
                }

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

    private double getUpperDealValue()
    {
        double probability = getDealProbability();
        double upperUtility = calculateActualUtility(maximizeSocialWelfareBid());

        return upperUtility * probability;
    }

    private double getUpperNextDeal()
    {
        double difference = getDecay((timeline.getCurrentTime() + 1) / timeline.getTotalTime());

        double upperBoundValue = calculateActualUtility(maximizeSocialWelfareBid()) + difference;
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

        return discount * upperBoundValue;
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
        if (arguments instanceof Inform)
            this.numberOfParties = (int) ((Inform) arguments).getValue();
        if (sender == null)
            return;

        lastAgent = sender;

        opponents.putIfAbsent(sender, new Opponent(sender, mainUtilitySpace));
        opponents.get(sender).addAction(arguments);
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> list)
    {
        double EUDeal = getUpperDealValue();
        double EUNeal = getUpperNextDeal();
        minUtilityRandom = EUNeal;

        switch (timeline.getType())
        {
            case Time:
                if (timeline.getTime() >= .99)
                    return new Accept();
                break;

            case Rounds:
                if (((DiscreteTimeline) timeline).getOwnRoundsLeft() <= 0)
                    return new Accept();
                break;

            default:
                System.err.println(
                        getName() + getVersion() + " is not compatible with a timeline of type " + timeline.getType());
                System.exit(16);
                return new Accept();
        }

        try
        {
            if (lastAgent != null && opponents.get(lastAgent).getLastAction() != null &&
                    opponents.get(lastAgent).getLastAction() instanceof Offer)
            {
                Bid partnerBid = ((Offer) opponents.get(lastAgent).getLastAction()).getBid();
                double offeredUtilFromOpponent = calculateActualUtility(partnerBid);

                return (offeredUtilFromOpponent > EUNeal) ? new Accept() : new Offer(getRandomBid());
            }

        } catch (Exception e)
        {
            System.err.println(getName() + getVersion() + " threw exception in chooseAction: " + e.getMessage());
        }

        try
        {
            return new Offer((EUDeal > EUNeal) ? maximizeSocialWelfareBid() : getRandomBid());
        } catch (Exception e)
        {
            return new Accept();
        }
    }
}

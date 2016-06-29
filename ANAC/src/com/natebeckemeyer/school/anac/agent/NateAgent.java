package com.natebeckemeyer.school.anac.agent;

import agents.SimpleAgent;
import javafx.util.Pair;
import negotiator.Agent;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.*;
import negotiator.session.Timeline;
import negotiator.utility.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author W.Pasman Some improvements over the standard SimpleAgent.
 *         <p>
 *         Random Walker, Zero Intelligence Agent
 */
public class NateAgent extends Agent
{
    private Action actionOfPartner = null;
    /**
     * Note: {@link SimpleAgent} does not account for the discount factor in its
     * computations
     */
    private static double MINIMUM_BID_UTILITY = 0.0;

    private HashMap<Issue, Number> valuations = new HashMap<>();

    /**
     * The maximum bid for this agent
     */
    private Bid maxBid;

    private HashMap<Objective, Pair<Double, HashMap<Value, Double>>> utilities = new HashMap<>();

    /**
     * init is called when a next session starts with the same opponent.
     * In the case of this agent, init calculates the utilities of all of the
     */
    @Override
    public void init()
    {
        MINIMUM_BID_UTILITY = utilitySpace.getReservationValueUndiscounted();

        HashMap<Objective, Evaluator> itemUtilities = new HashMap<>();
        AdditiveUtilitySpace space = ((AdditiveUtilitySpace) utilitySpace);

        if (space != null && space.getNrOfEvaluators() >= 0 && space.getEvaluators() != null)
            space.getEvaluators().forEach(entry -> itemUtilities.put(entry.getKey(), entry.getValue()));

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
                    break;

                case DISCRETE:
                    EvaluatorDiscrete evaluatorDiscrete = ((EvaluatorDiscrete) value);
                    evaluatorDiscrete.getValues().forEach(valueDiscrete -> utilities.get(key).getValue()
                            .put(valueDiscrete, evaluatorDiscrete.getValue(valueDiscrete) * value.getWeight()));
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

    @Override
    public String getVersion()
    {
        return "1.0";
    }

    @Override
    public String getName()
    {
        return "Nate's Agent";
    }

    @Override
    public void ReceiveMessage(Action opponentAction)
    {
        actionOfPartner = opponentAction;
    }

    @Override
    public Action chooseAction()
    {
        Action action = null;
        try
        {
            if (actionOfPartner == null)
                action = chooseRandomBidAction();
            if (actionOfPartner instanceof Offer)
            {
                Bid partnerBid = ((Offer) actionOfPartner).getBid();
                double offeredUtilFromOpponent = getUtility(partnerBid);
                // get current time
                double time = timeline.getTime();
                action = chooseRandomBidAction();

                Bid myBid = ((Offer) action).getBid();
                double myOfferedUtil = getUtility(myBid);

                // accept under certain circumstances
                if (isAcceptable(offeredUtilFromOpponent, myOfferedUtil, time))
                    action = new Accept(getAgentID());

            }
            if (timeline.getType().equals(Timeline.Type.Time))
            {
                sleep(0.005); // just for fun
            }
        } catch (Exception e)
        {
            System.out.println("Exception in ChooseAction:" + e.getMessage());
            action = new Accept(getAgentID()); // best guess if things go wrong.
        }
        return action;
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
            return (new Accept(getAgentID()));
        return (new Offer(getAgentID(), nextBid));
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
        Bid bid = null;
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

        } while (getUtility(bid) < MINIMUM_BID_UTILITY);

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

    private <K> HashMap<K, Double> normalize(Map<K, Double> doubleMap)
    {
        double sum = doubleMap.keySet().stream().mapToDouble(k -> doubleMap.get(k).doubleValue()).sum();
        HashMap<K, Double> map = new HashMap<>(doubleMap.size());

        if (sum != 0)
            doubleMap.keySet().stream().forEach(k -> map.put(k, (doubleMap.get(k).doubleValue() / sum)));
        else
            doubleMap.keySet().stream().forEach(k -> map.put(k, 1. / doubleMap.entrySet().size()));

        return map;
    }
}

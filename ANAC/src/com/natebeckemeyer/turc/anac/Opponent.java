package com.natebeckemeyer.turc.anac;

import javafx.util.Pair;
import negotiator.AgentID;
import negotiator.actions.Action;
import negotiator.issue.Objective;
import negotiator.issue.Value;
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created for TURC by @author Nate Beckemeyer on 2016-06-30.
 * <p>
 * The model of the opponent that I use to estimate it's actual utilities.
 */
class Opponent
{
    /**
     * The history of moves that this opponent has made.
     */
    private final ArrayList<Action> history;

    /**
     * The ID of the opponent.
     */
    private final AgentID id;

    /**
     * The estimated utilities of the agent. The structure of this map is as follows:
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
    private final HashMap<Objective, Pair<Double, HashMap<Value, Double>>> estimatedUtilities = new HashMap<>();

    /**
     * The actual utilitySpace of the agent.
     */
    private final AdditiveUtilitySpace utilitySpace;

    /**
     * The strategy used to estimate the utilities of "Opponent."
     */
    private final Updater estimationStrategy = new FrequencyEstimation();

    /**
     * @return The ID of the agent.
     */
    public AgentID getID()
    {
        return id;
    }

    /**
     * @return All of the actions performed by this opponent.
     */
    ArrayList<Action> getHistory()
    {
        return history;
    }

    /**
     * Construct a new agent opponent with an empty history.
     *
     * @param id The ID of the new agent.
     */
    Opponent(AgentID id, AdditiveUtilitySpace utilitySpace)
    {
        this(id, utilitySpace, new ArrayList<>());
    }

    /**
     * Construct a new agent opponent with a copy of {@code possibleHistory}
     *
     * @param id              The ID of this agent
     * @param utilitySpace    The utilitySpace of the utility space
     * @param possibleHistory The history to copy.
     */
    Opponent(AgentID id, AdditiveUtilitySpace utilitySpace, ArrayList<Action> possibleHistory)
    {
        this.id = id;
        this.utilitySpace = utilitySpace;
        this.history = new ArrayList<>(possibleHistory);

        initializeEstimatedUtilities();
    }

    /**
     * The copy constructor for opponents.
     *
     * @param opponent The opponent's fields to copy.
     */
    Opponent(Opponent opponent)
    {
        this(opponent.id, opponent.utilitySpace, opponent.history);
    }

    /**
     * Adds an action to this opponent's history.
     *
     * @param offer The offer made by the opponent to add to the history.
     */
    public void addAction(Action offer)
    {
        history.add(offer);
        estimationStrategy.updateUtilities(Action.getBidFromAction(offer), estimatedUtilities);
    }

    /**
     * @return The last action performed by this opponent.
     */
    public Action getLastAction()
    {
        return history.get(history.size() - 1);
    }

    /**
     * @return The utilities estimated for this opponent.
     */
    public HashMap<Objective, Pair<Double, HashMap<Value, Double>>> getEstimatedUtilities()
    {
        return estimatedUtilities;
    }

    /**
     * Populates the estimated utilities field, with 0 for everything.
     */
    private void initializeEstimatedUtilities()
    {
        for (Map.Entry<Objective, Evaluator> entry : utilitySpace.getEvaluators())
        {
            Objective key = entry.getKey();
            EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) entry.getValue();

            estimatedUtilities.put(key, new Pair<>(0., new HashMap<>()));
            evaluatorDiscrete.getValues().forEach(discreteVal -> estimatedUtilities.get(key).getValue()
                    .put(discreteVal, 0.));
        }
    }

}

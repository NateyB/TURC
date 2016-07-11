package com.natebeckemeyer.turc.anac;

import javafx.util.Pair;
import negotiator.actions.Action;
import negotiator.issue.Objective;
import negotiator.issue.Value;

import java.util.HashMap;

/**
 * Created for TURC by @author Nate Beckemeyer on 2016-06-30.
 */
interface Updater
{
    /**
     * @param offer       The bid offered by the opponent
     * @param utilities The set of utilities known for the agent
     */
    void updateUtilities(Action offer, HashMap<Objective, Pair<Double, HashMap<Value, Double>>> utilities);
}

package com.natebeckemeyer.turc.anac;

import javafx.util.Pair;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.Issue;
import negotiator.issue.Objective;
import negotiator.issue.Value;

import java.util.HashMap;

/**
 * Created for TURC by @author Nate Beckemeyer on 2016-06-30.
 */
class FrequencyEstimation implements Updater
{
    @Override
    public void updateUtilities(Action offer, HashMap<Objective, Pair<Double, HashMap<Value, Double>>> utilities)
    {
        if (offer instanceof Offer)
        {
            Bid bid = ((Offer) offer).getBid();

            for (Issue entry : bid.getIssues())
            {
                HashMap<Value, Double> issueMap = utilities.get(entry).getValue();
                Value number = bid.getValue(entry.getNumber());

                issueMap.put(number, issueMap.get(number) + 1);
                NateAgent.normalize(issueMap);
            }
        }
    }

    @Override public String toString()
    {
        return "Frequency-based";
    }
}
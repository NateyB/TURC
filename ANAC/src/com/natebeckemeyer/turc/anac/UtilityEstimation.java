package com.natebeckemeyer.turc.anac;

import javafx.util.Pair;
import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.issue.Objective;
import negotiator.issue.Value;

import java.util.HashMap;

/**
 * Created for TURC by @author Nate Beckemeyer on 2016-06-30.
 */
enum UtilityEstimation implements Updater
{
    FREQUENCY("Frequency-based")
            {
                @Override
                public void updateUtilities(Bid bid, HashMap<Objective, Pair<Double, HashMap<Value, Double>>> utilities)
                {
                    for (Issue entry : bid.getIssues())
                        utilities.get(entry).getValue().put(bid.getValue(entry.getNumber()), 1 + utilities.get(entry)
                                .getValue().get(bid.getValue(entry.getNumber())));
                }
            };

    private String name;

    @Override public String toString()
    {
        return name;
    }

    UtilityEstimation(String name)
    {
        this.name = name;
    }
}

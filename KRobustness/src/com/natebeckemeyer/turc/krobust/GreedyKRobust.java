package com.natebeckemeyer.turc.krobust;

import k.robust.Agent;
import k.robust.Task;
import k.robust.TeamFinderInterface;
import k.robust.TeamInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

/**
 * Created for TURC by @author Nate Beckemeyer on 2016-06-20.
 */
public class GreedyKRobust implements TeamFinderInterface
{
    private int numAgents;
    private int numTasks;
    private double[] taskCosts;
    private int[] needs;
    private HashMap<Integer, Agent> agents;

    private double calculateScore(Agent agent)
    {
        double score = 0;
        for (Task task : agent.getTasks())
            score += needs[task.ID] * taskCosts[task.ID];

        return (score * agent.getTasks().length) / agent.cost;
    }

    private void initializeTaskCosts()
    {
        HashMap<Task, Double> costs = new HashMap<>();
        HashMap<Task, Integer> num = new HashMap<>();
        for (int i = 0; i < numAgents; i++)
            for (Task task : agents.get(i).getTasks())
            {
                costs.put(task, costs.getOrDefault(task, (double) 0) + agents.get(i).cost / agents.get(i)
                        .getTasks().length);
                num.put(task, num.getOrDefault(task, 0) + 1);
            }

        double[] averageCosts = new double[numTasks];

        for (Task key : costs.keySet())
            averageCosts[key.ID] = costs.get(key) / num.get(key);

        this.taskCosts = averageCosts;
    }

    private Optional<Agent> selectNextAgent()
    {
        HashMap<Agent, Double> scores = new HashMap<>();
        agents.values().forEach(agent -> scores.put(agent, calculateScore(agent)));

        return scores.keySet().stream().max((agent1, agent2) -> scores.get(agent1).compareTo(scores.get(agent2)));
    }

    private boolean problemSatisfied()
    {
        for (int need : needs)
            if (need > 0)
                return false;

        return true;
    }

    public GreedyKRobust(Agent[] agents, Task[] tasks)
    {
        this.numTasks = tasks.length;
        this.needs = new int[numTasks];

        this.numAgents = agents.length;
        this.agents = new HashMap<>(numAgents);
        for (Agent agent : agents)
            this.agents.put(agent.ID, agent);

        initializeTaskCosts();
    }

    /**
     * Finds a team with the specified robustness.
     *
     * @param k The robustness of the team.
     * @return A k-robust team of agents.
     */
    @Override public TeamInterface findTeam(int k)
    {
        for (int i = 0; i < needs.length; i++)
            needs[i] = k + 1;

        ArrayList<Agent> team = new ArrayList<>();

        while (!problemSatisfied())
        {
            Agent selected;

            Optional<Agent> value = selectNextAgent();
            if (value.isPresent())
                selected = value.get();
            else
            {
                System.err.printf("No team exists that satisfies the %d-robustness requirement.%n", k);
                return ArrayList::new;
            }

            for (Task performable : selected.getTasks())
                needs[performable.ID] = Math.max(this.needs[performable.ID] - 1, 0);
            this.agents.remove(selected.ID);
            team.add(selected);
        }

        return () -> team;
    }

    public static void main(String[] args)
    {
    }
}

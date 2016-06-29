package com.natebeckemeyer.turc;

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

    private static Agent[] initializeAgents(double[] agentCosts, int[][] agentTasks)
    {
        Agent[] agents = new Agent[agentCosts.length];
        for (int i = 0; i < agents.length; i++)
        {
            Task[] taskList = new Task[agentTasks[i].length];
            for (int j = 0; j < taskList.length; j++)
                taskList[j] = new Task(agentTasks[i][j]);

            agents[i] = new Agent(i, taskList, agentCosts[i]);
        }

        return agents;
    }

    private static Task[] initializeTasks(int[][] agentTasks)
    {
        HashMap<Integer, Task> known = new HashMap<>();
        for (int[] tasks : agentTasks)
            for (int task : tasks)
                known.putIfAbsent(task, new Task(task));

        Task[] result = new Task[known.size()];
        known.keySet().forEach(integer -> result[integer] = known.get(integer));

        return result;
    }

    public static void main(String[] args)
    {
        double[] agentCosts = new double[]{
                2.071719871081833, 4.706542649533772, 4.166127048052811, 3.9153604713566414, 5.704169162641977,
                2.51610809611182, 2.472978668040221, 2.1135353655943088, 2.4641983482475576, 1.2341704293872697,
                1.975497246895587, 4.815312313516383, 1, 3.803797882419481, 2.7657969204949833, 1, 2.798006949035023,
                3.4239233061016674, 1, 1, 4.025213298163626, 1.1018878799661298, 1.9474837921509005, 1,
                1.3249077416092327, 5.362892321307562, 1, 1, 3.1002350418757674, 1, 2.5034821827509948, 1, 1,
                1.6425723923753992, 1, 3.208927392655804, 3.0074668102459663, 2.65744676142116, 1, 1.1797670201286306,
                1, 1, 1, 1.4053485702802972, 2.592891892372461, 1.2843137795285233, 4.6795204819053335,
                3.21525661550953, 1.7918116127074017, 2.1645017231749115, 3.747321375863933, 1.0461316792801065,
                2.014225810310151, 2.447087664890731, 3.9111800883334507, 2.978574444332433, 1, 7.29727495265273,
                2.571195943074377, 2.0061578070187958
        };

        int[][] agentTasks = new int[][]{new int[]{11}, new int[]{4, 7, 2, 17, 15, 6, 0, 18}, new int[]{13}, new
                int[]{5}, new int[]{14, 19, 9, 2, 7, 11, 16, 18, 6, 13, 12, 17, 4, 1, 10}, new int[]{17, 19, 4, 7, 9,
                18}, new int[]{19}, new int[]{6}, new int[]{9, 10, 5, 3, 8, 0, 18, 17, 6, 13, 2, 4}, new int[]{1, 8,
                15}, new int[]{15, 4}, new int[]{1}, new int[]{18}, new int[]{15, 17, 10, 7, 16, 9, 11}, new int[]{4,
                1, 19, 12, 18}, new int[]{3, 4, 1, 7, 8, 18, 12, 2, 19, 13, 0}, new int[]{0, 3, 9, 16, 15}, new
                int[]{4, 0, 7}, new int[]{4, 13, 11, 18, 2, 14, 6, 3}, new int[]{15, 18, 8, 19}, new int[]{14, 5, 15,
                19, 7, 13, 1, 8, 16, 10}, new int[]{0, 12, 9, 4, 7, 11, 14, 6, 1, 16, 5}, new int[]{12}, new
                int[]{10, 2, 4}, new int[]{14, 6, 19}, new int[]{19, 3, 7, 17, 10, 11, 4}, new int[]{0, 8, 5, 16, 14,
                13, 19, 12}, new int[]{18, 12, 6, 7, 8, 13, 0, 1, 4, 3, 17, 10}, new int[]{1, 3, 7, 15, 19, 13, 4,
                17, 14, 16, 8, 6, 10}, new int[]{4, 1}, new int[]{16, 4, 12, 9, 17, 0, 15, 11, 19, 18, 3, 6, 2}, new
                int[]{15, 0, 14, 16}, new int[]{11, 9, 3, 17, 10, 12, 7, 5, 18, 4, 1, 2}, new int[]{8, 1, 6, 5, 9,
                13, 17, 12}, new int[]{12, 17, 16, 5, 0, 1}, new int[]{15, 13, 9, 19, 2, 10}, new int[]{11, 6, 3, 8},
                new int[]{11}, new int[]{17}, new int[]{7}, new int[]{8}, new int[]{5, 19, 6, 12, 2, 14, 4}, new
                int[]{4, 10, 5, 8, 3, 15, 9, 6}, new int[]{12, 14, 8}, new int[]{14, 2, 9, 16, 8, 15, 5, 3, 6, 19,
                18, 1, 0}, new int[]{8, 2, 16, 13, 5, 10, 1}, new int[]{6, 15, 5, 8, 14, 4}, new int[]{4, 11, 14, 2,
                0, 16, 15, 1, 7, 12, 10, 18, 3}, new int[]{3}, new int[]{19, 2, 3, 13, 15, 5, 18}, new int[]{16, 10,
                11, 2}, new int[]{17, 6, 10, 11, 19, 13, 16, 1, 12, 8}, new int[]{19, 18, 10, 6}, new int[]{9, 13,
                19, 5, 14, 16, 15, 10, 7, 0, 2, 3, 12}, new int[]{17, 2, 6, 19, 3, 18, 12, 14, 5}, new int[]{12}, new
                int[]{13, 17, 19, 18, 14, 5, 2, 9, 16, 15, 6}, new int[]{0, 16, 4, 18, 3, 13}, new int[]{15, 8, 6,
                17}, new int[]{0, 14}};

        Agent[] possibleAgents = initializeAgents(agentCosts, agentTasks);
        Task[] knownTasks = initializeTasks(agentTasks);
        for (int k = 0; k < 14; k++)
        {
            GreedyKRobust mine = new GreedyKRobust(possibleAgents, knownTasks);
            TeamInterface team = mine.findTeam(k);

            System.out.printf("Simulation for k=%d:%n", k);
            team.getAgents().forEach(agent -> System.out.printf("%d, ", agent.ID));
            System.out.printf("Cost: %f%n%n", team.getAgents().stream().mapToDouble(agent -> agent.cost).sum());

        }
    }
}

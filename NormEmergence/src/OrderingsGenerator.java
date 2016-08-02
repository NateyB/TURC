import javafx.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created for TURC by @author Nate Beckemeyer on 2016-07-29.
 */
public class OrderingsGenerator
{
    private static boolean simpleNetwork = true;
    private HashMap<String, HashMap<Character, HashSet<Character>>> initialTopology = new HashMap<>();

    private static List<List<DependencyNode>> getValidOrderings(DependencyGraph g)
    {
        return getValidOrderings(g, new LinkedList<>(), new LinkedList<>());
    }

    private static List<List<DependencyNode>> getValidOrderings(DependencyGraph g,
                                                                LinkedList<DependencyNode> currentPath,
                                                                List<List<DependencyNode>> possibleOptions)
    {
        List<DependencyNode> choices = g.getFreeNodes();
        if (choices.size() == 0)
        {
            possibleOptions.add(currentPath);
            return possibleOptions;
        }

        for (DependencyNode n : choices)
        {
            currentPath.addLast(n);
            g.markVisited(n, true);
            possibleOptions = getValidOrderings(g, new LinkedList<>(currentPath), possibleOptions);
            g.markVisited(n, false);
            currentPath.removeLast();
        }

        return possibleOptions;
    }

    /**
     * Returns a pair; the key of the pair is the prefix, and the value of the pair is the suffix
     *
     * @param orderings
     * @return
     */
    private static Set<Pair<Set<String>, Set<String>>> splitAtLinkingCosts(List<List<DependencyNode>> orderings)
    {
        HashMap<Pair<HashSet<String>, HashSet<String>>, Boolean> exists = new HashMap<>();
        Set<Pair<Set<String>, Set<String>>> combinations = new HashSet<>();
        for (List<DependencyNode> choice : orderings)
        {
            for (int j = 0; j <= choice.size(); j++)
            {
                LinkedHashSet<String> prefix = new LinkedHashSet<>();
                for (int i = 0; i < j; i++)
                    prefix.add(choice.get(i).toString());

                LinkedHashSet<String> suffix = new LinkedHashSet<>();
                for (int i = j; i < choice.size(); i++)
                    suffix.add(choice.get(i).toString());

                Pair<HashSet<String>, HashSet<String>> currentPair = new Pair<>(prefix, suffix);

                if (!exists.getOrDefault(currentPair, false))
                    combinations.add(new Pair<>(prefix, suffix));
                exists.put(currentPair, true);
            }
        }
        return combinations;
    }

    private static String pairToString(Pair<Set<String>, Set<String>> split)
    {
        StringBuilder out = new StringBuilder();
        split.getKey().forEach(s ->
        {
            out.append(s);
            out.append(" > ");
        });
        out.append("r > ");
        split.getValue().forEach(s ->
        {
            out.append(s);
            out.append(" > ");
        });

        return out.substring(0, out.length() - 3);
    }

    private static Set<String> orderingToString(Set<Pair<Set<String>, Set<String>>> orderings)
    {
        HashSet<String> results = new HashSet<>();
        orderings.forEach(pair -> results.add(pairToString(pair)));

        return results;
    }

    private DependencyGraph initializeGraph(String filename)
    {
        HashSet<DependencyNode> nodes = new HashSet<>();
        Scanner parser;
        try
        {
            parser = new Scanner(new File(filename));
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            System.exit(10);
            return null;
        }

        while (parser.hasNextLine())
        {
            Scanner line = new Scanner(parser.nextLine());
            while (line.hasNext())
            {
                String identifier = line.next();
                HashSet<DependencyNode> dependencies = new HashSet<>();
                while (!line.hasNext(":"))
                    dependencies.add(DependencyNode.get(line.next()));
                nodes.add(DependencyNode.get(identifier, dependencies));
                initialTopology.put(identifier, new HashMap<>());
                line.skip(" :");

                // Now come the edges, so there should be at least two more if there is one more; they should be chars.
                while (line.hasNext())
                {
                    Character start = line.next().charAt(0);
                    Character end = line.next().charAt(0);
                    initialTopology.get(identifier).putIfAbsent(start, new HashSet<>());
                    initialTopology.get(identifier).get(start).add(end);
                }

            }
        }
        return new DependencyGraph(nodes);
    }

    private static HashMap<TopologyBuilder, HashSet<String>> seizeUniqueResults(HashMap<String, TopologyBuilder> all)
    {
        HashMap<TopologyBuilder, HashSet<String>> uniques = new HashMap<>();
        all.forEach((s, topology) ->
        {
            HashSet<String> configurations = uniques.getOrDefault(topology, new HashSet<>());
            configurations.add(s);
            uniques.putIfAbsent(topology, configurations);
        });
        return uniques;
    }

    public HashMap<String, TopologyBuilder> getAllConfigurations(DependencyGraph graph)
    {
        List<List<DependencyNode>> choices = getValidOrderings(graph);
        Set<Pair<Set<String>, Set<String>>> combinations = splitAtLinkingCosts(choices);
        HashMap<String, TopologyBuilder> allConfigurations = new HashMap<>();
        combinations.forEach(pair ->
        {
            TopologyBuilder topology = new TopologyBuilder(initialTopology, simpleNetwork);
            topology.cutEdges(pair.getValue());
            allConfigurations.put(pairToString(pair), topology);
        });

        return allConfigurations;
    }

    public static void main(String[] args)
    {
        String delimiter = new String(new char[38]).replace("\0", "-");
        System.out.printf("Running with simple networks %s.%n", (simpleNetwork ? "activated" : "deactivated"));

        Scanner console = new Scanner(System.in);
        System.out.printf("Please enter the name of the dataset: ");
        do
        {
            String input = console.nextLine().trim();
            if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit"))
                return;

            OrderingsGenerator generator = new OrderingsGenerator();
            DependencyGraph graph = generator.initializeGraph("NormEmergence/datasets/" + input);

            HashMap<String, TopologyBuilder> allConfigurations = generator.getAllConfigurations(graph);
            System.out.println("Number of meaningful linking cost placements: " + allConfigurations.size());

            HashMap<TopologyBuilder, HashSet<String>> uniqueResults = seizeUniqueResults(allConfigurations);
            System.out.println("Number of unique topologies generated by this dataset: " + uniqueResults.size());
            System.out.println(delimiter);
            uniqueResults.forEach((key, value) ->
            {
                value.forEach(System.out::println);
                System.out.println(key);
                System.out.println(delimiter);
            });
            System.out.println();
            System.out.printf("Please enter the name of the dataset or quit to exit: ");
        } while (console.hasNextLine());
    }
}

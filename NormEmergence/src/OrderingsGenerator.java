import javafx.util.Pair;

import java.util.*;

/**
 * Created for TURC by @author Nate Beckemeyer on 2016-07-29.
 */
public class OrderingsGenerator
{
    static boolean punishment = true;
    static boolean metapunishment = false;

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
            possibleOptions.add(currentPath);

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

    private static DependencyGraph initializeGraph()
    {
        HashSet<DependencyNode> nodes = new HashSet<>();
        DependencyNode a = DependencyNode.get("a");
        DependencyNode b = DependencyNode.get("b", new HashSet<>(Arrays.asList(a)));
        DependencyNode d = DependencyNode.get("d", new HashSet<>(Arrays.asList(a)));
        DependencyNode c = DependencyNode.get("c", new HashSet<>(Arrays.asList(b, d)));
        nodes.addAll(Arrays.asList(a, b, c, d));
        if (punishment)
        {
            DependencyNode h = DependencyNode.get("h", new HashSet<>(Arrays.asList(d)));
            DependencyNode f = DependencyNode.get("f", new HashSet<>(Arrays.asList(c, h)));
            DependencyNode e = DependencyNode.get("e", new HashSet<>(Arrays.asList(f)));
            DependencyNode g = DependencyNode.get("g", new HashSet<>(Arrays.asList(e)));
            DependencyNode i = DependencyNode.get("i", new HashSet<>(Arrays.asList(g)));
            nodes.addAll(Arrays.asList(e, f, g, h, i));
        }

        return new DependencyGraph(nodes);
    }


    public static void main(String[] args)
    {
        if (metapunishment && !punishment)
        {
            punishment = true;
            System.out.println(
                    "Attempted to use metapunishment without allowing for punishment. The punishment flag has been " +
                            "set to true.");
        }
        DependencyGraph graph = initializeGraph();

        List<List<DependencyNode>> choices = getValidOrderings(graph);
        Set<Pair<Set<String>, Set<String>>> combinations = splitAtLinkingCosts(choices);

        System.out.println(combinations.size());
        String delimiter = new String(new char[38]).replace("\0", "-");
        combinations.forEach(pair ->
        {
            System.out.println(pairToString(pair) + ": ");
            TopologyBuilder topologies = new TopologyBuilder();
            topologies.cutEdges(pair.getValue());
            System.out.printf("%s%n%s%n", topologies, delimiter);
        });
    }
}

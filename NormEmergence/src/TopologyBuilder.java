import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created for TURC by @author Nate Beckemeyer on 2016-08-01.
 * <p>
 * Beginning with a fully connected network, cut the pieces that aren't beneficial.
 */
public class TopologyBuilder
{
    private HashMap<String, HashMap<Character, HashSet<Character>>> network = initializeConnections();

    private static HashMap<String, HashMap<Character, HashSet<Character>>> initializeConnections()
    {
        HashMap<String, HashMap<Character, HashSet<Character>>> payoff = new HashMap<>();
        DependencyNode a = DependencyNode.get("a");
        DependencyNode b = DependencyNode.get("b", new HashSet<>(Arrays.asList(a)));
        DependencyNode d = DependencyNode.get("d", new HashSet<>(Arrays.asList(a)));
        DependencyNode c = DependencyNode.get("c", new HashSet<>(Arrays.asList(b, d)));

        HashMap<Character, HashSet<Character>> aMap = new HashMap<>();
        HashSet<Character> azSet = new HashSet<>();
        azSet.add('x');
        aMap.put('z', azSet);

        payoff.put(a.toString(), aMap);


        HashMap<Character, HashSet<Character>> bMap = new HashMap<>();
        HashSet<Character> bxSet = new HashSet<>();
        bxSet.add('x');
        bMap.put('x', bxSet);

        payoff.put(b.toString(), bMap);


        HashMap<Character, HashSet<Character>> cMap = new HashMap<>();
        HashSet<Character> cxSet = new HashSet<>();
        cxSet.add('z');
        cMap.put('x', cxSet);
        payoff.put(c.toString(), cMap);


        HashMap<Character, HashSet<Character>> dMap = new HashMap<>();
        HashSet<Character> dzSet = new HashSet<>();
        dzSet.add('z');
        dMap.put('z', dzSet);
        payoff.put(d.toString(), dMap);


        if (OrderingsGenerator.punishment)
        {
            HashSet<Character> awSet = new HashSet<>();
            awSet.add('x');
            aMap.put('w', awSet);

            bxSet.add('y');
            HashSet<Character> bySet = new HashSet<>();
            bySet.add('x');
            bySet.add('y');
            bMap.put('y', bySet);


            cxSet.add('w');

            DependencyNode h = DependencyNode.get("h", new HashSet<>(Arrays.asList(d)));
            DependencyNode f = DependencyNode.get("f", new HashSet<>(Arrays.asList(c, h)));
            DependencyNode e = DependencyNode.get("e", new HashSet<>(Arrays.asList(f)));
            DependencyNode g = DependencyNode.get("g", new HashSet<>(Arrays.asList(e)));
            DependencyNode i = DependencyNode.get("i", new HashSet<>(Arrays.asList(g)));


            HashMap<Character, HashSet<Character>> eMap = new HashMap<>();
            HashSet<Character> ezSet = new HashSet<>();
            ezSet.add('y');
            eMap.put('z', ezSet);

            HashSet<Character> ewSet = new HashSet<>();
            ewSet.add('y');
            eMap.put('w', ewSet);
            payoff.put(e.toString(), eMap);


            HashMap<Character, HashSet<Character>> fMap = new HashMap<>();
            HashSet<Character> fySet = new HashSet<>();
            fySet.add('z');
            fySet.add('w');
            fMap.put('y', fySet);
            payoff.put(f.toString(), fMap);


            HashMap<Character, HashSet<Character>> gMap = new HashMap<>();
            HashSet<Character> gzSet = new HashSet<>();
            gzSet.add('w');
            gMap.put('z', gzSet);
            payoff.put(g.toString(), gMap);


            HashMap<Character, HashSet<Character>> hMap = new HashMap<>();
            HashSet<Character> hwSet = new HashSet<>();
            hwSet.add('z');
            hMap.put('w', hwSet);
            payoff.put(h.toString(), hMap);


            HashMap<Character, HashSet<Character>> iMap = new HashMap<>();
            HashSet<Character> iwSet = new HashSet<>();
            iwSet.add('w');
            iMap.put('w', iwSet);
            payoff.put(i.toString(), iMap);
        }

        return payoff;
    }

    void cutEdge(String edge)
    {
        network.remove(edge);
    }

    void cutEdges(Set<String> edges)
    {
        edges.forEach(this::cutEdge);
    }

    HashMap<Character, HashSet<Character>> getTopology()
    {
        HashMap<Character, HashSet<Character>> results = new HashMap<>();
        network.values().forEach(map ->
                map.entrySet().forEach(characterEntry ->
                {
                    HashSet<Character> known = results.getOrDefault(characterEntry.getKey(), new HashSet<>());
                    characterEntry.getValue().forEach(known::add);
                    results.put(characterEntry.getKey(), known);
                }));

        return results;
    }

    @Override public String toString()
    {
        StringBuilder builder = new StringBuilder();
        HashMap<Character, HashSet<Character>> topology = getTopology();
        topology.entrySet().forEach(entry ->
        {
            entry.getValue().forEach(character ->
                    {
                        builder.append(entry.getKey());
                        builder.append(" -> ");
                        builder.append(character);
                        builder.append("\n");
                    }
            );
            builder.append("\n");
        });

        return builder.toString().trim();
    }

}

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

    private HashMap<String, HashMap<Character, HashSet<Character>>> network;

    private void initializeConnections(HashMap<String, HashMap<Character, HashSet<Character>>> initial)
    {
        initial.forEach((str, map) ->
        {
            HashMap<Character, HashSet<Character>> copy = new HashMap<>();
            map.forEach((character, charSet) -> copy.put(character, new HashSet<>(charSet)));
            network.put(str, copy);
        });
    }

    TopologyBuilder(HashMap<String, HashMap<Character, HashSet<Character>>> initialNetwork)
    {
        network = new HashMap<>();
        initializeConnections(initialNetwork);
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

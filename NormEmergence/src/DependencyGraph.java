import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created for TURC by @author Nate Beckemeyer on 2016-07-29.
 */
public class DependencyGraph
{
    private HashSet<DependencyNode> nodes;

    List<DependencyNode> getFreeNodes()
    {
        return nodes.stream().filter(DependencyNode::isIndependent).collect(Collectors.toList());
    }

    void markVisited(DependencyNode n, boolean visited)
    {
        n.setVisited(visited);
    }

    DependencyGraph(HashSet<DependencyNode> nodes)
    {
        this.nodes = nodes;
    }

}

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Note that distinct nodes must have distinct names.
 */
final class DependencyNode
{
    private static HashMap<String, DependencyNode> nodes = new HashMap<>();

    private String name;
    private boolean visited;
    private Set<DependencyNode> dependencies;

    private boolean isVisited()
    {
        return visited;
    }

    boolean isIndependent()
    {
        if (visited)
            return false;
        for (DependencyNode node : dependencies)
        {
            if (!node.isVisited())
                return false;
        }
        return true;
    }

    void setVisited(boolean newValue)
    {
        visited = newValue;
    }

    private DependencyNode(Set<DependencyNode> dependencies, String name)
    {
        this.dependencies = dependencies;
        this.name = name;
    }

    /**
     * Gets the {@link DependencyNode} of name {@code name}; if it doesn't exist, constructs it with no dependencies.
     *
     * @name The name of the node.
     */
    static DependencyNode get(String name)
    {
        return get(name, new HashSet<>());
    }

    /**
     * Gets the {@link DependencyNode} of name {@code name}; if it doesn't exist, constructs it with the
     * specified dependencies.
     *
     * @param name         The name of the node to get.
     * @param dependencies The dependencies to give the node if it does not exist.
     * @return The node
     */
    static DependencyNode get(String name, Set<DependencyNode> dependencies)
    {
        if (nodes.getOrDefault(name, null) == null)
            nodes.put(name, new DependencyNode(dependencies, name));
        return nodes.get(name);
    }

    @Override public String toString()
    {
        return name;
    }

    @Override public boolean equals(Object other)
    {
        boolean result = false;
        if (other != null && other instanceof DependencyNode)
        {
            DependencyNode that = (DependencyNode) other;
            result = this.name.equals(that.name);
        }
        return result;
    }

    @Override public int hashCode()
    {
        return name.hashCode();
    }
}

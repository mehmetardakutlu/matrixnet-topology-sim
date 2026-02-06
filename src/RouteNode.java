/**
 * Represents a candidate path in the Dijkstra priority queue. By linking back to the previous node,
 * we can reconstruct the path via backtracking only when the destination is reached.
 */
public class RouteNode{
    private final Host currentHost;
    // Used to reconstruct the route string
    private final RouteNode parent;
    // Accumulated dynamic latency. Primary sorting criterion for the Min-Heap.
    private final int totalLatency;
    // Number of steps taken. Used for calculating dynamic latency penalties (lambda)
    // and as the secondary sorting criterion.
    private final int hopCount;

    public RouteNode(Host currentHost, RouteNode parent, int totalLatency, int hopCount){
        this.currentHost = currentHost;
        this.parent = parent;
        this.totalLatency = totalLatency;
        this.hopCount = hopCount;
    }

    public Host getCurrentHost(){
        return currentHost;
    }
    /**
     * Returns the previous node in the path history.
     * Used for backtracking from destination to source to generate the output string.
     */
    public RouteNode getParent(){
        return parent;
    }
    public int getTotalLatency(){
        return totalLatency;
    }
    public int getHopCount(){
        return hopCount;
    }
}
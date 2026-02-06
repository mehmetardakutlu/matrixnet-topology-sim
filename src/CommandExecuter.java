import java.util.ArrayList;
import java.util.LinkedList;

/**
 * The core engine of MatrixNet, responsible for executing all console commands. This class handles
 * spawning hosts, linking/sealing backdoors,
 * routing algorithms (hybrid Dijkstra implementation for static and dynamic latency) and
 * network analysis (connectivity checks, articulation point/bridge detection).
 */
public class CommandExecuter {
    // The main graph data structure storing all hosts by ID.
    private final MyHashMap<Host> hostMap = new MyHashMap<>();

    // Calculating connected components is expensive (O(V+E)). Since the network topology
    // changes infrequently compared to analysis requests, we record the initial component count.
    private int recordedInitialComponents = -1; // -1 indicates the record is invalid (dirty).
    private boolean isRecordDirty = true; // Flag to track topology changes.

    CommandExecuter(){
    }

    /**
     * Creates a new node in the network. Ensures the ID does not contain banned characters,
     * there is no existing host with the same ID and clearance level is valid.
     * Invalidates the analysis record since the topology changes.
     */
    public String spawnHost(String hostID, int clearanceLevel){
        // Clearance level check
        if(clearanceLevel < 1 || clearanceLevel > 5){
            return "Some error occurred in spawn_host.";
        }
        // Banned character check
        for(int i = 0; i < hostID.length(); i++){
            if(!((65 <= hostID.charAt(i) && hostID.charAt(i) <= 90) ||
                 (48 <= hostID.charAt(i) && hostID.charAt(i) <= 57) ||
                 (hostID.charAt(i) == 95))){
                return "Some error occurred in spawn_host.";
            }
        }
        // Duplicate check
        if(!hostMap.find(hostID)){
            hostMap.insert(new Host(hostID, clearanceLevel));
            isRecordDirty = true; // Change record state
            return "Spawned host " + hostID + " with clearance level " + clearanceLevel + ".";
        }
        return "Some error occurred in spawn_host.";
    }

    /**
     * Creates a bidirectional link between two hosts.
     * Invalidates the analysis records.
     */
    public String linkBackdoor(String hostID1, String hostID2, int latency, int bandwidth,
                               int firewallLevel){
        // Self-link check
        if(hostID1.equals(hostID2)){
            return "Some error occurred in link_backdoor.";
        }
        Host host1 = hostMap.search(hostID1);
        Host host2 = hostMap.search(hostID2);
        // Latency validity, bandwidth validity and host existence check
        if(host1 == null || host2 == null || latency < 1 || bandwidth < 1 || firewallLevel < 1 ||
           firewallLevel > 5){
            return "Some error occurred in link_backdoor.";
        }
        // Check if the link already exists
        Backdoor backdoor = host1.getBackdoor(hostID2);
        if(backdoor != null){
            return "Some error occurred in link_backdoor.";
        }
        // Create bidirectional edges
        host1.addNeighbor(new Backdoor(host2, latency, bandwidth, firewallLevel));
        host2.addNeighbor(new Backdoor(host1, latency, bandwidth, firewallLevel));
        isRecordDirty = true; // Topology changed
        return "Linked " + hostID1 + " <-> " + hostID2 + " with latency " + latency + "ms, bandwidth " +
               bandwidth + "Mbps, firewall " + firewallLevel + ".";
    }

    /**
     * Toggles the operational status of a link (open - sealed).
     * Invalidates the analysis record because connectivity might change.
     */
    public String sealBackdoor(String hostID1, String hostID2){
        Host host1 = hostMap.search(hostID1);
        Host host2 = hostMap.search(hostID2);
        // Host existence check
        if(host1 == null || host2 == null){
            return "Some error occurred in seal_backdoor.";
        }
        // Seal/unseal operations must be reflected on both sides of the bidirectional link.
        int status = host1.sealBackdoor(hostID2);
        if(status == 0){ // Link not found
            return "Some error occurred in seal_backdoor.";
        }
        host2.sealBackdoor(hostID1); // Sync the other side
        isRecordDirty = true; // Connectivity might change.
        if(status == 1){ // Seal
            return "Backdoor " + hostID1 + " <-> " + hostID2 + " sealed.";
        }
        else{ // Unseal
            return "Backdoor " + hostID1 + " <-> " + hostID2 + " unsealed.";
        }
    }

    /**
     * Helper method to reconstruct the path string from the destination node back to the source.
     * Uses the "parent" pointers stored in RouteNode to avoid O(N^2) string concatenation during search.
     */
    private String reconstructPath(RouteNode endNode){
        StringBuilder stringBuilder = new StringBuilder();

        LinkedList<String> path = new LinkedList<>();
        RouteNode curr = endNode;
        while(curr != null){
            path.addFirst(curr.getCurrentHost().getID());
            curr = curr.getParent();
        }

        boolean first = true;
        for(String ID : path){
            if(!first){
                stringBuilder.append(" -> ");
            }
            stringBuilder.append(ID);
            first = false;
        }
        return stringBuilder.toString();
    }

    /**
     * Finds the optimal path between two hosts using a Hybrid Dijkstra Algorithm.
     * Strategy:
     * Case 1 (lambda = 0): Standard Dijkstra. We strictly visit each node once based on lowest latency.
     * Case 2 (lambda > 0): Dynamic Latency. A path with higher latency but fewer hops might become
     * optimal later due to step penalties. We use HostHistory class to keep
     * track of all non-dominated (Hops, Latency) pairs for each node.
     */
    public String traceRoute(String srcID, String destID, int minBandwidth, int lambda){
        Host srcHost = hostMap.search(srcID);
        Host destHost = hostMap.search(destID);

        // Host existence check
        if(srcHost == null || destHost == null){
            return "Some error occurred in trace_route.";
        }
        // Checks if source and destination are the same
        if(srcID.equals(destID)){
            return "Optimal route " + srcID + " -> " + destID + ": " + srcID + " (Latency = 0ms)";
        }

        MyMinHeap queue = new MyMinHeap();
        // Start with source node: Latency 0, Hops 0, Parent null
        queue.insert(new RouteNode(srcHost, null, 0, 0));

        // Data structures for visited state management based on the strategy (lambda)
        MyHashMap<GraphEl> visitedMap = null; // Strategy 1: Simple Visited Set
        MyHashMap<HostHistory> historyMap = null; // Strategy 2: HostHistory

        if(lambda == 0){
            visitedMap = new MyHashMap<>();
        }
        else{
            historyMap = new MyHashMap<>();
            HostHistory srcHist = new HostHistory(srcID);
            srcHist.addVisit(0, 0);
            historyMap.insert(srcHist);
        }

        while(!queue.isEmpty()){
            RouteNode curr = queue.deleteMin();
            Host currentHost = curr.getCurrentHost();
            String currentID = currentHost.getID();

            // Strategy 1 (Lambda = 0): Pruning
            // If we've visited this node before, we found the absolute shortest path already.
            if(lambda == 0){
                if (visitedMap.find(currentID)) continue;
                visitedMap.insert(currentHost);
            }

            // Target Reached
            if(currentID.equals(destID)){
                String pathStr = reconstructPath(curr);
                return "Optimal route " + srcID + " -> " + destID + ": " +
                        pathStr + " (Latency = " + curr.getTotalLatency() + "ms)";
            }

            // Explore Neighbors
            ArrayList<Backdoor> neighbors = currentHost.getNeighbors();
            for(Backdoor edge : neighbors){
                // Check edge constraints
                if(edge.isSealed()){
                    continue;
                }
                if(edge.getBandwidth() < minBandwidth){
                    continue;
                }
                if(currentHost.getClearanceLevel() < edge.getFirewallLevel()){
                    continue;
                }
                // Calculate Dynamic Cost: Base + (Lambda * Steps_Taken)
                int extraCost = lambda * curr.getHopCount();
                int newTotalLatency = curr.getTotalLatency() + edge.getLatency() + extraCost;
                int newHopCount = curr.getHopCount() + 1;

                Host targetHost = edge.getTargetHost();
                String targetID = targetHost.getID();

                if(lambda == 0){
                    // Strategy 1: Standard Dijkstra relaxation
                    if(!visitedMap.find(targetID)){
                        queue.insert(new RouteNode(targetHost, curr, newTotalLatency, newHopCount));
                    }
                }
                else{
                    // Strategy 2: Domination check
                    // Only expand if this path is not strictly worse (higher hops and higher latency)
                    // than any previously found path to this node.
                    HostHistory targetHistory = historyMap.search(targetID);
                    if(targetHistory == null){
                        targetHistory = new HostHistory(targetID);
                        targetHistory.addVisit(newHopCount, newTotalLatency);
                        historyMap.insert(targetHistory);
                        queue.insert(new RouteNode(targetHost, curr, newTotalLatency, newHopCount));
                    }
                    else{
                        if(!targetHistory.isDominated(newHopCount, newTotalLatency)){
                            targetHistory.addVisit(newHopCount, newTotalLatency);
                            queue.insert(new RouteNode(targetHost, curr, newTotalLatency, newHopCount));
                        }
                    }
                }
            }
        }
        return "No route found from " + srcID + " to " + destID;
    }

    /**
     * Returns the connected component count of the current network state.
     * Uses caching to avoid re-running BFS if the network topology hasn't changed.
     */
    private int getInitialComponentCount(){
        if(isRecordDirty || recordedInitialComponents == -1){
            recordedInitialComponents = countComponents(null, null, null);
            isRecordDirty = false;
        }
        return recordedInitialComponents;
    }

    /**
     * Calculates the number of connected components in the graph using BFS.
     * Supports "Simulation Mode" where specific hosts or edges can be ignored
     * to test for Articulation Points or Bridges without modifying the actual graph.
     * @param ignoredHost The host to ignore (simulate removal).
     * @param ignoredU Start node of the edge to ignore.
     * @param ignoredV End node of the edge to ignore.
     */
    private int countComponents(Host ignoredHost, Host ignoredU, Host ignoredV){
        int components = 0;
        MyHashMap<GraphEl> visited = new MyHashMap<>();
        // If simulating host removal, mark it as visited immediately so it's skipped.
        if(ignoredHost != null){
            visited.insert(ignoredHost);
        }

        int capacity = hostMap.getCapacity();
        // Traversing the HashMap
        for(int i = 0; i < capacity; i++){
            LinkedList<Host> bucket = hostMap.getEl(i);
            if(bucket == null){
                continue;
            }
            for(Host startNode : bucket){
                if(startNode == ignoredHost){
                    continue;
                }
                if(!visited.find(startNode.getID())){
                    // Found a new unvisited component
                    components += 1;
                    // Run BFS to mark all reachable nodes in this component
                    LinkedList<Host> queue = new LinkedList<>();
                    visited.insert(startNode);
                    queue.add(startNode);

                    while(!queue.isEmpty()){
                        Host u = queue.poll();

                        for(Backdoor edge : u.getNeighbors()){
                            if(edge.isSealed()){
                                continue;
                            }
                            Host v = edge.getTargetHost();

                            // Check if this edge is the one being simulated as "broken"
                            if(ignoredU != null && ignoredV != null){
                                boolean matchDirection1 = (u == ignoredU && v == ignoredV);
                                boolean matchDirection2 = (u == ignoredV && v == ignoredU);

                                if(matchDirection1 || matchDirection2){
                                    continue;
                                }
                            }
                            if(!visited.find(v.getID())){
                                visited.insert(v);
                                queue.add(v);
                            }
                        }
                    }
                }
            }
        }
        return components;
    }

    public String scanConnectivity(){
        int components = getInitialComponentCount(); // Uses the record
        if(components <= 1){
            return "Network is fully connected.";
        }
        else{
            return "Network has " + components + " disconnected components.";
        }
    }

    /**
     * Simulates the removal of a host to check if it's an articulation point.
     * Uses the recorded component count as a base for comparison.
     */
    public String simulateBreach(String hostID){
        Host host = hostMap.search(hostID);
        if(host == null){
            return "Some error occurred in simulate_breach.";
        }
        // Use recorded value for the baseline to avoid redundant BFS
        int initialComponents = getInitialComponentCount();
        // Recalculate components assuming 'host' is removed
        int newComponents = countComponents(host, null, null);
        // If components increase, the host was critical.
        if(newComponents > initialComponents){
            return "Host " + hostID + " IS an articulation point.\n" +
                   "Failure results in " + newComponents + " disconnected components.";
        }
        else{
            return "Host " + hostID + " is NOT an articulation point. Network remains the same.";
        }
    }

    /**
     * Checks if a backdoor (edge) is a bridge.
     * Logic: If removing the edge increases the number of connected components, it is critical.
     */
    public String simulateBreach(String hostID1, String hostID2){
        Host h1 = hostMap.search(hostID1);
        Host h2 = hostMap.search(hostID2);

        if(h1 == null || h2 == null){
            return "Some error occurred in simulate_breach.";
        }
        Backdoor backdoor = h1.getBackdoor(hostID2);
        if(backdoor == null){
            return "Some error occurred in simulate_breach.";
        }
        if(backdoor.isSealed()){
            return "Some error occurred in simulate_breach.";
        }
        int initialComponents = getInitialComponentCount();
        // Recalculate components assuming the link h1-h2 is broken
        int newComponents = countComponents(null, h1, h2);
        if(newComponents > initialComponents){
            return "Backdoor " + hostID1 + " <-> " + hostID2 + " IS a bridge.\n" +
                   "Failure results in " + newComponents + " disconnected components.";
        }
        else{
            return "Backdoor " + hostID1 + " <-> " + hostID2 + " is NOT a bridge. Network remains the same.";
        }
    }

    /**
     * Generates a comprehensive report of the network status, including
     * statistics, connectivity, and cycle detection.
     */
    public String oracleReport(){
        int totalHosts = 0;
        int totalUnsealed = 0;
        double totalBandwidth = 0;
        double totalClearance = 0;

        // Iterate through all hosts to gather statistics
        int capacity = hostMap.getCapacity();
        for(int i = 0; i < capacity; i++){
            LinkedList<Host> bucket = hostMap.getEl(i);
            if(bucket == null){
                continue;
            }
            for(Host h : bucket){
                totalHosts += 1;
                totalClearance += h.getClearanceLevel();
                for(Backdoor backdoor : h.getNeighbors()){
                    if(!backdoor.isSealed()){
                        totalUnsealed++;
                        totalBandwidth += backdoor.getBandwidth();
                    }
                }
            }
        }
        // Each undirected edge is stored as 2 directed edges, so divide by 2.
        totalUnsealed /= 2;
        totalBandwidth /= 2;

        int components = getInitialComponentCount(); // Uses the record
        String connStatus;
        if(components <= 1){
            connStatus = "Connected";
        }
        else{
            connStatus = "Disconnected";
        }
        boolean cycle = hasCycle();

        String cycleStatus;
        if(cycle){
            cycleStatus = "Yes";
        }
        else{
            cycleStatus = "No";
        }

        // Calculate averages safely avoiding division by zero
        double avgBandwidth;
        if(totalUnsealed == 0){
            avgBandwidth = 0;
        }
        else{
            avgBandwidth = totalBandwidth / totalUnsealed;
        }

        double avgClearance;
        if(totalHosts == 0){
            avgClearance = 0;
        }
        else{
            avgClearance = totalClearance / totalHosts;
        }

        String bandwidthStr = String.format("%.1f", avgBandwidth);
        String clearanceStr = String.format("%.1f", avgClearance);

        return "--- Resistance Network Report ---\n" +
                "Total Hosts: " + totalHosts + "\n" +
                "Total Unsealed Backdoors: " + totalUnsealed + "\n" +
                "Network Connectivity: " + connStatus + "\n" +
                "Connected Components: " + components + "\n" +
                "Contains Cycles: " + cycleStatus + "\n" +
                "Average Bandwidth: " + bandwidthStr + "Mbps\n" +
                "Average Clearance Level: " + clearanceStr;
    }

    /**
     * A helper class used to emulate the recursion stack for the Iterative DFS.
     * Stores the current node being visited and its parent (the node we came from).
     * This is necessary to detect back-edges in an undirected graph without recursion.
     */
    private static class StackFrame{
        Host node;
        Host parent;

        StackFrame(Host node, Host parent){
            this.node = node;
            this.parent = parent;
        }
    }

    /**
     * Detects whether the network graph contains any cycles.
     * This method uses an iterative DFS approach instead of recursion.
     * This ensures the algorithm is robust against StackOverflowError even on very large,
     * deep graphs where recursive approaches would fail.
     * @return true if a cycle is detected in any connected component, false otherwise.
     */
    private boolean hasCycle(){
        // Keeps track of visited nodes to avoid infinite loops and reprocessing.
        MyHashMap<GraphEl> visited = new MyHashMap<>();
        int capacity = hostMap.getCapacity();

        // Iterate through all buckets in the HashMap to ensure we check every node.
        for(int i = 0; i < capacity; i++){
            LinkedList<Host> bucket = hostMap.getEl(i);
            if (bucket == null) continue;

            for(Host startNode : bucket){
                // If the node is already visited, it belongs to a component we have already checked.
                if(visited.find(startNode.getID())){
                    continue;
                }
                LinkedList<StackFrame> stack = new LinkedList<>();
                // Push the starting node. Since it's the root of this traversal, parent is null.
                stack.push(new StackFrame(startNode, null));
                visited.insert(startNode);

                while(!stack.isEmpty()){
                    StackFrame current = stack.pop();
                    Host u = current.node;
                    Host parent = current.parent;

                    // Explore all adjacent neighbors (backdoors)
                    for(Backdoor edge : u.getNeighbors()){
                        if(edge.isSealed()){
                            continue;
                        }
                        Host v = edge.getTargetHost();
                        // Case 1: Neighbor v has not been visited yet.
                        if(!visited.find(v.getID())){
                            visited.insert(v);
                            stack.push(new StackFrame(v, u));
                        }
                        // Case 2: Neighbor v is already visited, and it is not our parent -> CYCLE.
                        else if(v != parent){
                            return true;
                        }
                    }
                }
            }
        }
        // If we traverse the entire graph without finding a back-edge, no cycle exists.
        return false;
    }
}

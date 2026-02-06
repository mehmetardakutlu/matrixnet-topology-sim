/**
 * Represents a "Backdoor" tunnel (a directed edge) in the MatrixNet graph.
 * Stores connection properties (latency, bandwidth, firewall) used as weights
 * and constraints in the pathfinding algorithms. While the network links are
 * logically bidirectional, this object represents the connection from a specific
 * origin to the targetHost.
 */
public class Backdoor{
    // The destination node of this directed edge.
    private final Host targetHost;
    // Edge properties used for routing costs and constraints.
    private final int latency;
    private final int bandwidth;
    private final int firewallLevel;

    // If true, this edge is blocked and cannot be traversed.
    private boolean isSealed;

    /**
     * Initializes a new tunnel.
     * By default, all new backdoors are active (unsealed).
     */
    Backdoor(Host targetHost, int latency, int bandwidth, int firewallLevel){
        this.targetHost = targetHost;
        this.latency = latency;
        this.bandwidth = bandwidth;
        this.firewallLevel = firewallLevel;
        isSealed = false;
    }

    /**
     * Checks if the tunnel is currently blocked by agents.
     * Sealed tunnels are ignored during route tracing and connectivity checks.
     */
    public boolean isSealed(){
        return isSealed;
    }

    // Toggles the operational state (open - closed).
    public void changeSeal(){
        isSealed = !isSealed;
    }

    // Getters used by Dijkstra and analysis algorithms
    public int getLatency(){
        return latency;
    }

    public int getBandwidth(){
        return bandwidth;
    }

    public int getFirewallLevel(){
        return firewallLevel;
    }

    public Host getTargetHost(){
        return targetHost;
    }

    public String getHostID(){
        return targetHost.getID();
    }
}

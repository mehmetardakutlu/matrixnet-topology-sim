/**
 * A simple data carrier representing a specific arrival state at a Host.
 * We store both hops and latency because a path with higher latency
 * but fewer hops cannot be discarded immediately; it might become optimal later due
 * to dynamic latency penalties (lambda).
 */
public class Visit{
    // Number of steps taken. Critical for calculating future lambda penalties.
    private final int hops;
    // Total accumulated latency. The primary cost metric.
    private final int latency;

    public Visit(int hops, int latency){
        this.hops = hops;
        this.latency = latency;
    }

    public int getHops(){
        return hops;
    }

    public int getLatency(){
        return latency;
    }
}

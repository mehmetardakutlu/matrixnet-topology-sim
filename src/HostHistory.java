import java.util.ArrayList;

/**
 * Maintains a record of visits to a specific Host.
 * Used specifically in type 2.2 scenarios. Unlike standard Dijkstra
 * which keeps only the single best cost, this class keeps a list of valid (hops, latency) pairs.
 * It allows us to prune paths that are "dominated" (strictly worse in both metrics) by previous visits,
 * reducing the number of candidate paths remarkably while ensuring the optimal path is found.
 */
public class HostHistory extends GraphEl{
    // Stores the list of non-dominated arrival states found so far.
    private final ArrayList<Visit> visits;

    public HostHistory(String ID){
        super(ID);
        this.visits = new ArrayList<>();
    }

    /**
     * Checks if a new arrival state is "dominated" by any historical visit.
     * Logic: If we have previously reached this host with both:
     * 1. Fewer (or equal) hops and
     * 2. Lower (or equal) latency,
     * then the new path offers no future advantage and should be pruned.
     * @return true if the new path is inefficient and should be discarded.
     */
    public boolean isDominated(int newHops, int newLatency){
        for(Visit v : visits){
            // If an existing visit is better in every way, the new one is useless.
            if(v.getHops() <= newHops && v.getLatency() <= newLatency){
                return true;
            }
        }
        return false;
    }

    // Records a new valid (non-dominated) visit state.
    public void addVisit(int hops, int latency){
        visits.add(new Visit(hops, latency));
    }
}
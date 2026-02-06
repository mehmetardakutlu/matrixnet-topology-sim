import java.util.ArrayList;

/**
 * Represents a "Host" (node) in the MatrixNet graph.
 * Each host has a specific security clearance level and maintains a list of
 * outgoing connections (Backdoors). This class extends GraphEl to inherit
 * the unique ID required for hashing.
 */
public class Host extends GraphEl{
    // Security level (1-5) used to check against tunnel firewall constraints.
    private final int clearanceLevel;
    // Adjacency list storing outgoing connections.
    private final ArrayList<Backdoor> adjacentHosts;

    Host(String ID, int clearanceLevel){
        super(ID);
        this.clearanceLevel = clearanceLevel;
        adjacentHosts = new ArrayList<>();
    }

    // Adds a directed edge to the adjacency list.
    public void addNeighbor(Backdoor backdoor){
        adjacentHosts.add(backdoor);
    }

    /**
     * Searches for a specific connection to the given target ID.
     */
    public Backdoor getBackdoor(String ID){
        for(Backdoor backdoor : adjacentHosts){
            if(backdoor.getHostID().equals(ID)){
                return backdoor;
            }
        }
        return null;
    }

    /**
     * Toggles the sealed state of a connection.
     * @return 0 if not found, 1 if the backdoor is sealed, 2 if the backdoor is unsealed.
     */
    public int sealBackdoor(String ID){
        Backdoor backdoor = getBackdoor(ID);
        if(backdoor == null){
            return 0;
        }
        if(!backdoor.isSealed()){
            backdoor.changeSeal();
            return 1;
        }
        else{
            backdoor.changeSeal();
            return 2;
        }
    }

    public int getClearanceLevel(){
        return clearanceLevel;
    }

    public ArrayList<Backdoor> getNeighbors(){
        return adjacentHosts;
    }
}

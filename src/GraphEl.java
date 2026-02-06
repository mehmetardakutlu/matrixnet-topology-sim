/**
 * Base class for all entities stored in MyHashMap. Ensures every element has a unique ID.
 */
public class GraphEl {
    private final String ID; // Immutable unique ID.

    GraphEl(String ID){
        this.ID = ID;
    }

    /**
     * Returns the unique ID.
     * Primarily used by MyHashMap to calculate hash codes and bucket indices.
     */
    protected String getID(){
        return ID;
    }
}

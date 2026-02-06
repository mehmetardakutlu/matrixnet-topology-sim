import java.util.LinkedList;

/**
 * A custom hash table implementation using separate chaining for collision resolution.
 * Designed to store objects extending GraphEl.
 * @param <T> The type of elements stored, must extend GraphEl (provides an ID).
 */
public class MyHashMap<T extends GraphEl>{
    private final int capacity;
    private final LinkedList<T>[] array;

    MyHashMap(){
        // Fixed capacity chosen as a large prime number (150001).
        capacity = 150001;
        array = (LinkedList<T>[]) new LinkedList[capacity];
    }

    /**
     * Computes the bucket index for a given key string.
     */
    public int hash(String key){
        int hashVal = key.hashCode();
        hashVal = hashVal & 0x7FFFFFFF;
        return hashVal % capacity;
    }

    public LinkedList<T> getEl(int index){
        return array[index];
    }

    public int getCapacity(){
        return capacity;
    }

    /**
     * Inserts an element into the map based on its ID.
     * If the bucket is empty, a new LinkedList is initialized.
     */
    public void insert(T element){
        int index = hash(element.getID());
        if(array[index] == null){
            array[index] = new LinkedList<>();
        }
        array[index].add(element);
    }

    /**
     * Returns an element by its unique string key (ID), or null if the element is not found.
     */
    public T search(String key){
        int index = hash(key);
        if(array[index] != null){
            for(T element : array[index]){
                if(element.getID().equals(key)){
                    return element;
                }
            }
        }
        return null;
    }

    /**
     * Checks if an element with the given key exists in the map.
     */
    public boolean find(String key){
        int index = hash(key);
        if(array[index] == null){
            return false;
        }
        for(T element : array[index]){
            if(element.getID().equals(key)){
                return true;
            }
        }
        return false;
    }
}

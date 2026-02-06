/**
 * A Min-Heap implementation for RouteNode objects.
 * Optimized for the large case scenarios of the MatrixNet project.
 */
public class MyMinHeap{
    private RouteNode[] heap;
    // Index 0 is left empty
    private int size = 0;

    /**
     * Initializes the heap with a large capacity.
     * 200000 is chosen to minimize expensive resize operations during tests.
     */
    public MyMinHeap(){
        this.heap = new RouteNode[200000];
    }

    /**
     * Determines the priority order based on project specifications.
     * Order of precedence:
     * 1. Total latency (lower is better)
     * 2. Hop count (lower is better)
     * 3. Host ID (lexicographically smaller is better)
     */
    private boolean isPrioritized(RouteNode node1, RouteNode node2){
        // Primary criterion: Latency
        if(node1.getTotalLatency() < node2.getTotalLatency()){
            return true;
        }
        if(node1.getTotalLatency() > node2.getTotalLatency()){
            return false;
        }

        // Secondary criterion: Hop Count
        if(node1.getHopCount() < node2.getHopCount()){
            return true;
        }
        if(node1.getHopCount() > node2.getHopCount()){
            return false;
        }

        // Last criterion: Lexicographical Order
        return node1.getCurrentHost().getID().compareTo(node2.getCurrentHost().getID()) < 0;
    }

    /**
     * Inserts a new node into the heap.
     * Uses percolate up strategy to conserve min heap property.
     */
    public void insert(RouteNode node){
        if(size >= heap.length - 1){
            resize();
        }

        size++;
        int hole = size;

        // Uses "Hole" percolation to reduce swap operations.
        while(hole > 1){
            int parentIndex = hole / 2;
            if(isPrioritized(node, heap[parentIndex])){
                heap[hole] = heap[parentIndex]; // Push parent down
                hole = parentIndex;
            }
            else{
                break;
            }
        }
        heap[hole] = node; // Place new node in the correct hole
    }

    /**
     * Removes and returns the highest priority (minimum cost) node.
     * Uses percolate down strategy.
     */
    public RouteNode deleteMin(){
        if(size == 0){
            return null;
        }

        RouteNode minNode = heap[1];
        RouteNode lastNode = heap[size];
        heap[size] = null;
        size--;

        if(size == 0){
            return minNode;
        }

        int hole = 1;
        while(hole * 2 <= size){
            int leftChildIndex = hole * 2;
            int rightChildIndex = hole * 2 + 1;
            int smallerChildIndex = leftChildIndex;

            // Find the higher priority child
            if(rightChildIndex <= size && isPrioritized(heap[rightChildIndex], heap[leftChildIndex])){
                smallerChildIndex = rightChildIndex;
            }

            // If lastNode is prioritized over the smaller child, we found the correct place
            if(isPrioritized(lastNode, heap[smallerChildIndex])){
                break;
            }

            heap[hole] = heap[smallerChildIndex]; // Move child up
            hole = smallerChildIndex;
        }

        heap[hole] = lastNode;
        return minNode;
    }

    public boolean isEmpty(){
        return size == 0;
    }

    // Doubles array capacity if the heap becomes full.
    private void resize(){
        RouteNode[] newHeap = new RouteNode[heap.length * 2];
        System.arraycopy(heap, 1, newHeap, 1, size);
        heap = newHeap;
    }
}
# MatrixNet: Advanced Network Topology Simulator
## 📖 Overview

**MatrixNet** is a robust simulation engine designed to model and analyze secure network topologies. Unlike standard routing simulators, MatrixNet implements a **Hybrid Dijkstra Algorithm** that adapts to both static latency and dynamic hop-based penalties, solving complex routing problems where the "shortest path" depends on the accumulated network load (Lambda factor).

The project emphasizes algorithmic efficiency and memory management, utilizing **custom-engineered data structures** (HashMaps, MinHeaps) and **Iterative DFS** traversals to handle large-scale graphs without hitting JVM recursion limits.

## 🚀 Key Features

* **Hybrid Routing Engine:** Calculates optimal paths using two distinct strategies:
    * *Static Mode ($\lambda=0$):* Standard Dijkstra for lowest latency.
    * *Dynamic Mode ($\lambda>0$):* A modified pathfinding algorithm that penalizes hop counts, using a **Pareto-Optimal Domination Check** to explore paths that appear more expensive initially but may yield lower total costs due to step penalties.
* **Critical Infrastructure Analysis:** Identifies **Articulation Points** (Hosts) and **Bridges** (Backdoors) whose failure would partition the network, using optimized connectivity simulations.
* **Stack-Safe Cycle Detection:** Implements an **Iterative DFS** using explicit `StackFrame` objects to detect cycles in deep graph structures, preventing `StackOverflowError` common in recursive solutions.
* **Lazy Connectivity Caching:** Tracks topology changes (`isRecordDirty` flag) to cache connected component counts, avoiding redundant $O(V+E)$ BFS operations during repetitive status checks.

## 🛠 Technical Architecture

### 1. Hybrid Dijkstra & Domination Pruning
In dynamic routing scenarios, a path with higher latency but fewer hops might eventually become optimal. To solve this, I implemented a **HostHistory** system instead of a simple "Visited" set.
* **Logic:** A new path to a node is added to the priority queue *only if* it is not "dominated" by a previous visit.
* **Domination Rule:** A path $P_{new}$ is dominated if there exists a $P_{old}$ such that:
    $$Latency(P_{old}) \le Latency(P_{new}) \quad \text{AND} \quad Hops(P_{old}) \le Hops(P_{new})$$
    This drastically reduces the search space while guaranteeing the optimal solution.

### 2. Custom Data Structures
To demonstrate low-level understanding of data organization:
* **MyHashMap:** A custom implementation using **Separate Chaining** with a prime capacity ($150,001$) to minimize collisions via Horner's Method hashing.
* **MyMinHeap:** A binary heap optimized for `RouteNode` objects with a "hole-filling" percolation strategy to reduce swap operations during insertion and deletion.

## 💻 Commands & Usage

The system accepts a script file containing the following commands:

| Command | Description |
| :--- | :--- |
| `spawn_host <ID> <Clearance>` | Creates a new node with security clearance. |
| `link_backdoor <ID1> <ID2> <Lat> <BW> <FW>` | Creates a bidirectional edge with Latency, Bandwidth, and Firewall constraints. |
| `seal_backdoor <ID1> <ID2>` | Blocks/Unblocks a connection (simulating failure). |
| `trace_route <ID1> <ID2> <BW> <Lambda>` | Finds the optimal path. If $\lambda > 0$, hop penalties are applied. |
| `simulate_breach <ID>` | Checks if removing a host disconnects the network (Articulation Point). |
| `oracle_report` | Generates a full statistical report of the network health. |

## 📂 Project Structure

```text
src/
├── CommandExecuter.java  # Main Logic (Dijkstra, DFS, Simulation)
├── Main.java             # Entry point & File I/O
├── HostHistory.java      # State tracking
├── Visit.java            # State object (Hops vs Latency)
├── RouteNode.java        # Dijkstra search node
├── MyHashMap.java        # Custom Hash Table
├── MyMinHeap.java        # Custom Priority Queue
├── Host.java             # Graph Node
├── Backdoor.java         # Graph Edge
└── GraphEl.java          # Base Identity Class
testcase_inputs/          # Sample input files
testcase_output/          # Sample output files
```
## ⚙️ Compilation & Execution Logic

Per the assignment requirements, the project is designed to be lightweight and compile directly from the source without build tools like Maven or Gradle.

### 1. Compile
Navigate to the source directory and compile all java files using the wildcard operator:

```bash
javac *.java
```

### 2. Run
Execute the program by running the Main class. You must provide exactly two arguments: the input file path and the output file path.

```bash
java Main <input_file> <output_file>
```

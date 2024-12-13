/* ***************************************************
 * Graph class used in the BFS, DFS, and Hybrid solvers
 *************************************************** */

import java.util.*;

// Represents a graph of Sudoku boards using an adjacency list
public class SolverGraph {
    private final Map<SudokuBoard, List<SudokuBoard>> adjacencyList;

    // Constructor to initialize the graph with the starting board
    public SolverGraph(SudokuBoard startBoard) {
        adjacencyList = new HashMap<>();
        // Add the start board to the graph as the first vertex (no edges yet)
        adjacencyList.put(startBoard, new ArrayList<>());
    }

    // Adds a vertex (SudokuBoard) to the graph.
    public void addVertex(SudokuBoard board) {
        // Ensure no duplicate boards in the graph
        if (!adjacencyList.containsKey(board)) {
            adjacencyList.put(board, new ArrayList<>());
        }
    }

    // Adds an edge between two vertices in the graph
    public void addEdge(SudokuBoard fromBoard, SudokuBoard toBoard) {
        // Ensure both vertices exist
        addVertex(fromBoard);
        addVertex(toBoard);
        // Add the edge
        adjacencyList.get(fromBoard).add(toBoard);
    }

    // Gets the next possible states (out neighbors) of a given Sudoku board
    public List<SudokuBoard> getOutNeighbors(SudokuBoard currentBoard) {
        return adjacencyList.getOrDefault(currentBoard, new ArrayList<>());
    }

    // Returns the number of vertices in the graph.
    public int getVertexCount() {
        return adjacencyList.size();
    }

    // Returns the number of edges in the graph.
    public int getEdgeCount() {
        int count = 0;
        for (List<SudokuBoard> neighbors : adjacencyList.values()) {
            count += neighbors.size();
        }
        return count;
    }

    // Checks if a given Sudoku board is in the graph.
    public boolean contains(SudokuBoard board) {
        return adjacencyList.containsKey(board);
    }

    // Prints the graph for debugging purposes.
    public void printGraph() {
        for (Map.Entry<SudokuBoard, List<SudokuBoard>> entry : adjacencyList.entrySet()) {
            System.out.println("Vertex: ");
            PuzzleSolver.printBoard(entry.getKey());
            System.out.println("Neighbors: ");
            for (SudokuBoard neighbor : entry.getValue()) {
                PuzzleSolver.printBoard(neighbor);
                System.out.println("\n");
            }
            System.out.println();
        }
    }

}

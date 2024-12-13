/* ***************************************************
 * Anthony J. Mercure
 *
 * Sudoku puzzle solvers (BFS, DLS, and a hybrid of the two)
 * (Note: The graph class is implemented separately as SolverGraph)
 *
 * Inspiration for this project came from the following paper:
 * Lina, T. N., & Rumetna, M. S. (2021). Comparison Analysis of Breadth
 *          First Search and Depth Limited Search Algorithms in Sudoku
 *          Game. Bulletin of Computer Science and Electrical Engineering,
 *          2(2), 74–83. https://doi.org/10.25008/bcsee.v2i2.1146
 *
 * solvePuzzleBFS() and solvePuzzleDFS() were developed based on the descriptions provided in the paper
 * solvePuzzleHybrid() was developed from some of the suggested improvements mentioned in the conclusion of the paper
 *
 * Sudoku puzzles were taken from https://sudokutodo.com/generator and typed into txt files
 *************************************************** */

/*
Summary of solvePuzzleHybrid():
The hybrid solver integrates a heuristic to rank the most constrained cells (cells with the most limited choices for
valid numbers in the puzzle) and combines the depth-focused approach of Depth-Limited Search (DLS) with the broad state
exploration of Breadth-First Search (BFS). The solver limits its search to the most impactful moves by concentrating on
cells with fewer viable possibilities, preventing pointless memory use and repeated searches. By using Iterative
Deepening A* (IDA*) to adjust its depth, this technique strikes a balance between the depth-limiting characteristics of
DFS and the memory-intensive nature of BFS. Because it concentrates on cells with fewer possibilities, the hybrid solver
does well on most puzzles, particularly those with a higher degree of constraint. However, the heuristic might not be as
successful in reducing the search space in highly symmetrical or minimally constrained puzzles, so in these instances it
will be slower than usual.
Because the hybrid solver constantly refines the search depending on the heuristic, it is faster than BFS, particularly
when puzzles are more constrained. Nevertheless, the hybrid solution could be slower than DFS and need more iterations
if the heuristic does not significantly guide the search. In such cases, the algorithm essentially performs the same
operations as DFS but with additional overhead from heuristic calculations and iterative threshold adjustments. Because
the hybrid solution avoids storing every conceivable state at once, space efficiency is improved compared to BFS. Unlike
BFS, which may become unreasonably costly in terms of space (demonstrated by HardPuzzle2.txt), the hybrid solver
balances memory consumption by employing heuristic-based exploration and dynamic depth limitation.
The hybrid solver is more appropriate for rapidly identifying a single solution as opposed to identifying all possible
solutions. Finding the first possible solution is its main priority, and it stops when it does. With some adjustments,
it could be able to identify all solutions, but it is not built with this function in mind. The heuristic and the
iterative deepening process, which are key components of the algorithm's efficiency, work well for solving constrained
puzzles but may not be as good at exploring every possibility in puzzles that call for an exhaustive search. The hybrid
solver's dependence on heuristic prioritizing speeds it up in some situations but restricts its ability to do an
exhaustive search.
*/

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class PuzzleSolver {
    // Size of the Sudoku grid (9x9)
    private static final int GRID_SIZE = 9;

    public static void main(String[] args) throws IOException {
        // Prompt the user for a file and load the puzzle
        String fileName = getFileNameFromUser();
        if (fileName == null) return;
        int[][] board = loadPuzzleFromFile(fileName);
        if (board.length == 0 || board[0].length == 0) {
            System.out.println("Failed to load puzzle from file.");
            return;
        }

        // Display the original puzzle
        System.out.println("\nOriginal Sudoku Puzzle:");
        SudokuBoard start = new SudokuBoard(board);
        printBoard(start);

        // Ask user to select BFS, DFS, or Hybrid
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nChoose a solving method:");
        System.out.println("a. Breadth-First Search (BFS)");
        System.out.println("b. Depth-First Search (DFS)");
        System.out.println("c. Hybrid Search (BFS & DFS)");
        System.out.print("Enter your choice (a, b, or c): ");
        String choice = scanner.nextLine().trim().toLowerCase();

        // Keep track of the memory and time to solve the puzzle
        System.out.println("\nTracking memory and time usage...");
        long startTime = System.nanoTime();
        trackMemoryUsage("Before solving");

        // Solve the puzzle based on the chosen method
        List<SudokuBoard> solutions = new ArrayList<>();
        if ("a".equals(choice)) {
            solutions = solvePuzzleBFS(start);
        } else if ("b".equals(choice)) {
            solutions = solvePuzzleDLS(start);
        } else if ("c".equals(choice)) {
            solutions = solvePuzzleHybrid(start);
        } else {
            System.out.println("Invalid choice.");
            return;
        }

        long endTime = System.nanoTime();
        trackMemoryUsage("After solving");
        System.out.printf("Time taken: %.2f ms%n", (endTime - startTime) / 1e6);

        // Display the solved puzzle or an error message if no solution exists
        if (!solutions.isEmpty()) {
            System.out.println("\nSolved Sudoku puzzle(s) (BFS may find multiple solutions):");
            for (SudokuBoard b : solutions) {
                printBoard(b);
                System.out.println("\n");
            }
        } else {
            System.out.println("\nNo solution exists for the given Sudoku puzzle.");
        }
    }

    // As per the paper, BFS returns all solutions found
    private static List<SudokuBoard> solvePuzzleBFS(SudokuBoard start) {
        List<SudokuBoard> solutions = new ArrayList<>();
        // Initialize the graph and queue for BFS
        SolverGraph graph = new SolverGraph(start);
        Queue<SudokuBoard> queue = new LinkedList<>();
        // Keep track of visited states (to prevent revisiting)
        Set<String> known = new HashSet<>();

        // Add the starting board to the graph and queue
        queue.add(start);
        known.add(start.getBoardAsString()); // Record the hash of the starting board to avoid revisiting it
        graph.addVertex(start); // Add the start board as a vertex in the graph

        // Perform BFS
        while (!queue.isEmpty()) {
            SudokuBoard currentBoard = queue.poll();

            // If the board is solved, add it to solutions
            if (currentBoard.isSolved()) {
                solutions.add(currentBoard);
            }

            for (SudokuBoard nextState : currentBoard.getNextStates(false)) {
                String nextStateStr = nextState.getBoardAsString();
                if (!known.contains(nextStateStr)) {
                    queue.add(nextState);
                    known.add(nextStateStr); // Mark this board as visited
                    graph.addVertex(nextState); // Add the next state as a vertex in the graph
                    graph.addEdge(currentBoard, nextState); // Add an edge from the current board to the next state
                }
            }
        }

        // Return the list of solutions
        return solutions;
    }

    // Since a 9x9 Sudoku puzzle has only 81 spaces, a DLS effectively behaves the same as a DFS in this context
    // Future improvement: exploring an optimal depth limit for this DLS could be valuable to determine if a solution is
    // inevitable or impossible earlier than depth 81, improving efficiency
    // As per the paper, the DLS only returns the first solution found
    private static List<SudokuBoard> solvePuzzleDLS(SudokuBoard start) {
        // Initialize the graph and stack for DLS
        SolverGraph graph = new SolverGraph(start); // Initialize the graph with the start board
        Stack<SudokuBoard> stack = new Stack<>();
        List<SudokuBoard> solutions = new ArrayList<>();
        // Keep track of visited states (to prevent revisting)
        Set<String> known = new HashSet<>();

        // Push the starting board onto the stack
        stack.push(start);
        known.add(start.getBoardAsString()); // Record the hash of the starting board to avoid revisiting it
        graph.addVertex(start); // Add the start board as a vertex in the graph

        // Perform DLS
        while (!stack.isEmpty()) {
            SudokuBoard currentBoard = stack.pop();

            // If the board is solved, add it to solutions
            if (currentBoard.isSolved()) {
                solutions.add(currentBoard);
                return solutions; // Stop after finding the first solution
            }

            // Get the next states and process them
            for (SudokuBoard nextState : currentBoard.getNextStates(false)) {
                // Use a hash for uniqueness
                String nextStateStr = nextState.getBoardAsString();
                if (!known.contains(nextStateStr)) {
                    // Add to stack for further exploration
                    stack.push(nextState);
                    known.add(nextStateStr); // Mark this board as visited
                    graph.addVertex(nextState); // Add the next state as a vertex in the graph
                    graph.addEdge(currentBoard, nextState); // Add an edge from the current board to the next state
                }
            }
        }
        // If no solution is found, return an empty list
        return solutions;
    }

    // Method combining BFS and DFS using Iterative Deepening A* (IDA*)
    // https://www.geeksforgeeks.org/iterative-deepening-a-algorithm-ida-artificial-intelligence/
    // Uses a depth-first strategy while maintaining an adaptive threshold to balance memory efficiency
    // Only returns first solution found
    private static List<SudokuBoard> solvePuzzleHybrid(SudokuBoard start) {
        // Initialize the graph Hybrid searching
        SolverGraph graph = new SolverGraph(start);
        List<SudokuBoard> solutions = new ArrayList<>();
        int threshold = start.heuristic();

        while (true) {
            // Perform a hybrid DFS/BFS up to the current threshold
            int result = hybridHelper(start, 0, threshold, graph, new HashSet<>(), solutions);
            if (result == -1) break; // Found a solution
            if (result == Integer.MAX_VALUE) return solutions; // No solution exists
            threshold = result; // Update the threshold
        }
        return solutions;
    }

    private static int hybridHelper(SudokuBoard board, int g, int threshold, SolverGraph graph, Set<Integer> known, List<SudokuBoard> solutions) {
        // Calculate the estimated cost (f = g + h),
        // where g = depth and h = heuristic value
        int f = g + board.heuristic();

        // If the estimated cost exceeds the current threshold,
        // return this cost to update the threshold
        if (f > threshold) return f;

        // If the board is solved, add it to the solutions list and stop searching further
        if (board.isSolved()) {
            solutions.add(board);
            return -1; // Found a solution
        }

        // Mark the current board as visited by adding its hash to the "known" set
        known.add(board.getCompactHash());
        graph.addVertex(board); // Add the current board as a vertex in the graph

        int min = Integer.MAX_VALUE; // Minimum cost for the next iteration

        // Explore all possible next states from the current board
        for (SudokuBoard nextState : board.getNextStates(true)) {
            // If this state has not been visited, explore it recursively
            int nextStateHash = nextState.getCompactHash();
            if (!known.contains(nextStateHash)) {
                graph.addVertex(nextState); // Add the next state as a vertex in the graph
                graph.addEdge(board, nextState); // Add an edge from the current board to the next state

                // Perform a recursive hybrid search for the next state
                int result = hybridHelper(nextState, g + 1, threshold, graph, known, solutions);

                // If a solution was found, propagate this back
                if (result == -1) return -1;

                // Otherwise, update the minimum cost for the next iteration
                min = Math.min(min, result);
            }
        }

        // Backtrack: remove the current board's hash from the "known" set
        known.remove(board.getCompactHash());

        return min; // Return the minimum cost for the next iteration
    }

    // Prompt the user to choose a puzzle file
    private static String getFileNameFromUser() {
        System.out.println("Choose a Sudoku puzzle to solve:");
        System.out.println("a. Easy Puzzle");
        System.out.println("b. Medium Puzzle");
        System.out.println("c. Hard Puzzle");
        System.out.print("Enter your choice (a-c): ");

        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine().trim().toLowerCase();
        // Map the user's choice to the corresponding file name
        switch (choice) {
            case "a": return "EasyPuzzle1.txt";
            case "b": return "MediumPuzzle1.txt";
            case "c": return "HardPuzzle1.txt";
            default:
                System.out.println("Invalid choice.");
                return null;
        }
    }

    // Load the Sudoku puzzle from a text file
    // The file should contain 9 rows of 9 integers, separated by spaces
    private static int[][] loadPuzzleFromFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        int[][] board = new int[GRID_SIZE][GRID_SIZE];
        String line;
        int row = 0;
        // Read each line of the file and populate the board
        while ((line = br.readLine()) != null && row < GRID_SIZE) {
            String[] values = line.trim().split("\\s+");
            for (int col = 0; col < GRID_SIZE; col++) {
                board[row][col] = Integer.parseInt(values[col]);
            }
            row++;
        }
        br.close();
        return board;
    }

    // Print the Sudoku board to the console in a readable format
    // Includes grid lines for better visual clarity
    static void printBoard(SudokuBoard board) {
        for (int row = 0; row < GRID_SIZE; row++) {
            if (row % 3 == 0 && row != 0) {
                System.out.println("- - - - - - - - - - -");
            }
            for (int col = 0; col < GRID_SIZE; col++) {
                if (col % 3 == 0 && col != 0) {
                    System.out.print("| ");
                }
                System.out.print(board.getBoard()[row][col] + " ");
            }
            System.out.println();
        }
    }

    // Method to track memory and time to solve
    private static void trackMemoryUsage(String phase) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println(phase + " - Used Memory: " + (usedMemory / 1024) + " KB");
    }
}

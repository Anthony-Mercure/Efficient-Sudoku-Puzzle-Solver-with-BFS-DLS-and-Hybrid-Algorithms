/* ***************************************************
 * SudokuBoard class is used in conjunction with the SolverGraph class to solve Sudoku puzzles using graph algorithms
 * Provides functionality to:
 *  Store the current state of the board
 *  Generate possible next states
 *  Check if the board is solved
 *  Compare boards for equality
 *  Provide a representation for hashing
 *************************************************** */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SudokuBoard {
    // 2D array representing the Sudoku board
    private final int[][] board;
    // Size of the Sudoku grid (9x9)
    private static final int GRID_SIZE = 9;

    // Constructor to initialize the Sudoku graph with a given board
    public SudokuBoard(int[][] board) {
        this.board = copyBoard(board);
    }

    // Generates possible next states of the Sudoku board.
    // If prioritizeMostConstrained is true, uses the heuristic to prioritize cells with fewer options.
    // Otherwise, uses basic state generation.
    public List<SudokuBoard> getNextStates(boolean prioritizeMostConstrained) {
        if (prioritizeMostConstrained) {
            return getNextStatesWithHeuristic(); // Uses a heuristic-based approach
        } else {
            return getNextStatesBasic(); // Uses a basic state generation approach
        }
    }

    // Check if placing a number in a specific position is valid
    // (the number does not already exist in the same row, column, or 3x3 subgrid)
    private boolean isValid(int row, int col, int num) {
        // Check the row for duplicates
        for (int i = 0; i < GRID_SIZE; i++) {
            if (board[row][i] == num) return false;
        }
        // Check the column for duplicates
        for (int i = 0; i < GRID_SIZE; i++) {
            if (board[i][col] == num) return false;
        }
        // Check the 3x3 subgrid for duplicates
        // Calculate the starting row of the subgrid
        int boxRowStart = row - row % 3;
        // Calculate the starting column of the subgrid
        int boxColStart = col - col % 3;
        for (int i = boxRowStart; i < boxRowStart + 3; i++) {
            for (int j = boxColStart; j < boxColStart + 3; j++) {
                if (board[i][j] == num) return false;
            }
        }
        // If no conflicts, the move is valid
        return true;
    }

    // Check if the Sudoku puzzle is solved (if there are no empty cells (0s) remaining)
    public boolean isSolved() {
        for (int[] row : board) {
            for (int cell : row) {
                if (cell == 0) return false;
            }
        }
        return true;
    }

    // Get a copy of the current board
    public int[][] getBoard() {
        return copyBoard(this.board);
    }

    // Utility method to copy the board
    private int[][] copyBoard(int[][] original) {
        int[][] copy = new int[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, GRID_SIZE);
        }
        return copy;
    }

    // Method provides a unique identifier for the current board state for tracking visited states.
    // Returns an integer representing the compact hash of the board.
    public int getCompactHash() {
        return Arrays.deepHashCode(board);
    }

    // Counts the number of valid numbers (1-9) that can be placed in the specified cell.
    // Used by the heuristic-based state generator to prioritize cells with fewer options.
    private int countValidNumbers(int row, int col) {
        boolean[] possible = new boolean[GRID_SIZE + 1];
        Arrays.fill(possible, true);

        // Mark invalid numbers based on the row and column
        for (int i = 0; i < GRID_SIZE; i++) {
            if (board[row][i] != 0) possible[board[row][i]] = false;
            if (board[i][col] != 0) possible[board[i][col]] = false; // Assume all numbers are valid initially
        }

        // Mark invalid numbers based on the 3x3 subgrid
        int boxRowStart = row - row % 3;
        int boxColStart = col - col % 3;
        for (int i = boxRowStart; i < boxRowStart + 3; i++) {
            for (int j = boxColStart; j < boxColStart + 3; j++) {
                if (board[i][j] != 0) possible[board[i][j]] = false; // Subgrid check
            }
        }

        // Count the remaining valid numbers
        int count = 0;
        for (int num = 1; num <= GRID_SIZE; num++) {
            if (possible[num]) count++;
        }
        return count;
    }

    // Generate possible states for the graph
    // Finds the first empty cell (0) and generates new boards by trying numbers 1-9
    // Returns a list of SudokuGraph instances representing possible next states
    private List<SudokuBoard> getNextStatesBasic() {
        List<SudokuBoard> nextStates = new ArrayList<>();
        // Iterate over the entire grid to find the first empty cell
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                // Check for an empty cell
                if (board[row][col] == 0) {
                    // Try all possible numbers (1-9) in the empty cell
                    for (int num = 1; num <= GRID_SIZE; num++) {
                        // Validate the move
                        if (isValid(row, col, num)) {
                            // Place the number in the empty cell
                            board[row][col] = num;
                            // Add the new state
                            nextStates.add(new SudokuBoard(board));
                            board[row][col] = 0; // Backtrack
                        }
                    }
                    // Return immediately after generating states for the first empty cell
                    return nextStates;
                }
            }
        }
        return nextStates; // Return an empty list if no moves can be generated
    }

    // Heuristic for Hybrid solver:
    // Counts the total number of empty cells and returns it as the heuristic value.
    // Fewer empty cells indicate closer proximity to a solution.
    public int heuristic() {
        int emptyCells = 0;
        // Count all empty cells (value 0) in the board
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (board[row][col] == 0) {
                    emptyCells++;
                }
            }
        }
        return emptyCells;
    }

    // Generates possible next states using heuristic-based approach.
    // Prioritizes the cell with the fewest valid options for state generation.
    private List<SudokuBoard> getNextStatesWithHeuristic() {
        List<SudokuBoard> nextStates = new ArrayList<>();
        int minOptions = GRID_SIZE + 1;
        // Track the most constrained cell
        int targetRow = -1;
        int targetCol = -1;

        // Find the cell with the fewest valid options
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (board[row][col] == 0) {
                    int options = countValidNumbers(row, col);
                    if (options < minOptions) { // Update if fewer options are found
                        minOptions = options;
                        targetRow = row;
                        targetCol = col;
                    }
                }
            }
        }
        // If no empty cell is found, return an empty list
        if (targetRow == -1 || targetCol == -1) {
            return nextStates;
        }

        // Generate states by filling the most constrained cell
        for (int num = 1; num <= GRID_SIZE; num++) {
            if (isValid(targetRow, targetCol, num)) { // Validate the move
                board[targetRow][targetCol] = num; // Place the number temporarily
                nextStates.add(new SudokuBoard(board)); // Create a new state
                board[targetRow][targetCol] = 0; // Backtrack to restore the original state
            }
        }
        return nextStates;
    }

    // Was originally using Arrays.deepToString() to check if two boards were equal, but it used too much memory
    // Chat recommended SudokuBoard has its own equal function to check if two boards are deeply equal
    // This also used too much memory but we kept it just in case
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SudokuBoard that = (SudokuBoard) o;
        // Compare 2D arrays
        return Arrays.deepEquals(this.board, that.board);
    }

    // Create a string representation of the board
    // Less susceptible to collisions than getCompactHash()
    // Chat recommended this StringBuilder java class
    public String getBoardAsString() {
        StringBuilder sb = new StringBuilder();
        for (int[] row : board) {
            for (int cell : row) {
                sb.append(cell);
            }
        }
        return sb.toString();
    }
}

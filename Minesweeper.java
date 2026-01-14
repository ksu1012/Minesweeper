package Minesweeper;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Minesweeper {
    JFrame frame = new JFrame("Minesweeper");
    int tileSize = 50;
    int rows = 10;
    int cols = 10;
    int numMines = 10;

    int[][] board = new int[rows][cols]; // -1 for mine, 0-8 for number of adjacent mines

    public Minesweeper() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(cols * tileSize, rows * tileSize);
        frame.setLayout(new GridLayout(rows, cols));

        for (int i = 0; i < rows * cols; i++) {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(tileSize, tileSize));
            frame.add(button);
        }

        frame.pack();
        frame.setVisible(true);
    }

    public void placeMines() {
        int placedMines = 0;
        while (placedMines < numMines) {
            int r = (int) (Math.random() * rows);
            int c = (int) (Math.random() * cols);
            if (board[r][c] != -1) {
                board[r][c] = -1;
                placedMines++;
                updateAdjacentCounts(r, c);
            }
        }
    }

    public void updateAdjacentCounts(int mineRow, int mineCol) {
        for (int r = mineRow - 1; r <= mineRow + 1; r++) {
            for (int c = mineCol - 1; c <= mineCol + 1; c++) {
                if (r >= 0 && r < rows && c >= 0 && c < cols && board[r][c] != -1) {
                    board[r][c]++;
                }
            }
        }
    }

    public void revealTile(int row, int col) {
        // Reveal current tile IF not already revealed

        //then recursively reveal adjacent tiles if the count is 0, include this in previous if-statement
        if (board[row][col] == 0) {
            for (int r = row - 1; r <= row + 1; r++) {
                for (int c = col - 1; c <= col + 1; c++) {
                    if (r >= 0 && r < rows && c >= 0 && c < cols && board[r][c] != -1) {
                        revealTile(r, c);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        new Minesweeper();
    }
}

package Minesweeper;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Minesweeper {
    JFrame frame = new JFrame("Minesweeper");
    
    // UI Panels
    JPanel topPanel = new JPanel();
    JPanel boardPanel = new JPanel();

    // HUD Elements
    JLabel minesLabel = new JLabel();
    JLabel timeLabel = new JLabel();
    JButton resetButton = new JButton();

    int tileSize = 40; // Size of each tile in pixels

    // Backend variables
    int rows = 10;
    int cols = 10;
    int numMines = 10;
    int minesRemaining;
    int seconds = 0;
    
    boolean gameOver = false;
    boolean firstClick = true;
    
    // Logic Arrays
    int[][] board;      // -1 for mine, 0-8 for adjacent
    JButton[][] buttons;
    boolean[][] revealed;
    boolean[][] flagged;

    // Timer
    Timer timer;

    // Images
    Image[] numIcons = new Image[9]; // 0 to 8
    Image mineIcon, mineRedIcon, flagIcon, tileIcon, tilePressedIcon;

    public Minesweeper() {
        // 1. Load Images
        loadSprites();

        // 2. Setup Frame & Menu
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        setupMenu();

        // 3. Setup Top HUD
        setupTopPanel();
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(boardPanel, BorderLayout.CENTER);

        // 4. Initialize Timer
        timer = new Timer(1000, e -> {
            seconds++;
            updateTimerLabel();
        });

        // 5. Start Default Game
        startNewGame(10, 10, 10);
        
        frame.setVisible(true);
    }

    private void setupTopPanel() {
        topPanel.setLayout(new BorderLayout());
        topPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        topPanel.setPreferredSize(new Dimension(0, 50));

        // Mine Counter (Left)
        minesLabel.setFont(new Font("Monospaced", Font.BOLD, 25));
        minesLabel.setHorizontalAlignment(JLabel.CENTER);
        minesLabel.setPreferredSize(new Dimension(80, 50));
        minesLabel.setOpaque(true);
        minesLabel.setBackground(Color.BLACK);
        minesLabel.setForeground(Color.RED);

        // Timer (Right)
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 25));
        timeLabel.setHorizontalAlignment(JLabel.CENTER);
        timeLabel.setPreferredSize(new Dimension(80, 50));
        timeLabel.setOpaque(true);
        timeLabel.setBackground(Color.BLACK);
        timeLabel.setForeground(Color.RED);

        // Reset Button (Center)
        resetButton.setFocusable(false);
        resetButton.setFont(new Font("SansSerif", Font.BOLD, 20));
        resetButton.setText("☺");
        resetButton.addActionListener(e -> startNewGame(rows, cols, numMines));

        topPanel.add(minesLabel, BorderLayout.WEST);
        topPanel.add(resetButton, BorderLayout.CENTER);
        topPanel.add(timeLabel, BorderLayout.EAST);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");

        JMenuItem beginner = new JMenuItem("Beginner (9x9, 10 Mines)");
        beginner.addActionListener(e -> startNewGame(9, 9, 10));

        JMenuItem intermediate = new JMenuItem("Intermediate (16x16, 40 Mines)");
        intermediate.addActionListener(e -> startNewGame(16, 16, 40));

        JMenuItem expert = new JMenuItem("Expert (16x30, 99 Mines)");
        expert.addActionListener(e -> startNewGame(16, 30, 99));

        JMenuItem custom = new JMenuItem("Custom...");
        custom.addActionListener(e -> showCustomDialog());

        gameMenu.add(beginner);
        gameMenu.add(intermediate);
        gameMenu.add(expert);
        gameMenu.addSeparator();
        gameMenu.add(custom);

        menuBar.add(gameMenu);
        frame.setJMenuBar(menuBar);
    }

    private void showCustomDialog() {
        JTextField rField = new JTextField("20");
        JTextField cField = new JTextField("20");
        JTextField mField = new JTextField("50");

        Object[] message = { "Rows:", rField, "Cols:", cField, "Mines:", mField };

        int option = JOptionPane.showConfirmDialog(frame, message, "Custom Board", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                int r = Integer.parseInt(rField.getText());
                int c = Integer.parseInt(cField.getText());
                int m = Integer.parseInt(mField.getText());
                
                // Limits to prevent crashing
                r = Math.max(5, Math.min(24, r));
                c = Math.max(5, Math.min(50, c));
                m = Math.min(m, (r * c) - 9); // Ensure at least 9 spots for first click

                startNewGame(r, c, m);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Invalid Input");
            }
        }
    }

    // --- GAME START LOGIC ---
    public void startNewGame(int r, int c, int m) {
        this.rows = r;
        this.cols = c;
        this.numMines = m;
        this.minesRemaining = m;
        
        // Reset Logic
        board = new int[rows][cols];
        buttons = new JButton[rows][cols];
        revealed = new boolean[rows][cols];
        flagged = new boolean[rows][cols];
        
        gameOver = false;
        firstClick = true;
        seconds = 0;
        timer.stop();
        
        // Update HUD
        updateMineLabel();
        updateTimerLabel();
        resetButton.setText("☺");

        // Rebuild Board UI
        boardPanel.removeAll();
        boardPanel.setLayout(new GridLayout(rows, cols));
        boardPanel.setPreferredSize(new Dimension(cols * tileSize, rows * tileSize));

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(tileSize, tileSize));
                button.setBorderPainted(false);
                button.setMargin(new Insets(0,0,0,0));
                button.setIcon(new ImageIcon(tileIcon));
                
                // Add the Advanced Mouse Listener
                button.addMouseListener(new TileMouseHandler(i, j));
                
                buttons[i][j] = button;
                boardPanel.add(button);
            }
        }
        
        frame.pack();
        frame.setLocationRelativeTo(null); // Center on screen
    }

    // --- MOUSE HANDLER CLASS (Press, Drag, Release) ---
    class TileMouseHandler extends MouseAdapter {
        int r, c;
        boolean pressed = false;

        public TileMouseHandler(int r, int c) {
            this.r = r;
            this.c = c;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (gameOver || revealed[r][c]) return;

            if (SwingUtilities.isRightMouseButton(e)) {
                toggleFlag(r, c);
            } else if (SwingUtilities.isLeftMouseButton(e) && !flagged[r][c]) {
                pressed = true;
                buttons[r][c].setIcon(new ImageIcon(tilePressedIcon)); // Visual "down" state
                resetButton.setText("😮");
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (gameOver) return;
            resetButton.setText("☺");

            if (SwingUtilities.isLeftMouseButton(e) && pressed) {
                // Check if mouse is still over the button
                if (buttons[r][c].contains(e.getPoint()) && !flagged[r][c]) {
                    revealTile(r, c);
                } else {
                    // Dragged out -> Cancel
                    buttons[r][c].setIcon(new ImageIcon(tileIcon));
                }
                pressed = false;
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // If dragging out, revert visual to unclicked
            if (pressed) {
                buttons[r][c].setIcon(new ImageIcon(tileIcon));
            }
        }
    }

    public void revealTile(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return;
        if (revealed[r][c] || flagged[r][c]) return;

        // Handle First Click Safe Zone
        if (firstClick) {
            firstClick = false;
            timer.start();
            placeMines(r, c); // Pass the clicked coordinate to avoid it
        }

        revealed[r][c] = true;

        if (board[r][c] == -1) {
            // Game Over
            buttons[r][c].setIcon(new ImageIcon(mineRedIcon));
            gameOver = true;
            timer.stop();
            resetButton.setText("😵");
            showAllMines();
            JOptionPane.showMessageDialog(frame, "Game Over!");
        } else {
            // Safe Tile
            int val = board[r][c];
            buttons[r][c].setIcon(new ImageIcon(numIcons[val]));

            if (val == 0) {
                // Recursively reveal neighbors
                for (int i = r - 1; i <= r + 1; i++) {
                    for (int j = c - 1; j <= c + 1; j++) {
                        revealTile(i, j);
                    }
                }
            }
            checkWin();
        }
    }

    public void placeMines(int safeRow, int safeCol) {
        int placed = 0;
        while (placed < numMines) {
            int r = (int) (Math.random() * rows);
            int c = (int) (Math.random() * cols);
            
            // Check if this spot is in the 3x3 exclusion zone around the first click
            boolean inSafeZone = (r >= safeRow - 1 && r <= safeRow + 1) && 
                                 (c >= safeCol - 1 && c <= safeCol + 1);

            if (board[r][c] != -1 && !inSafeZone) {
                board[r][c] = -1;
                placed++;
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

    public void toggleFlag(int r, int c) {
        if (revealed[r][c]) return;

        flagged[r][c] = !flagged[r][c];
        if (flagged[r][c]) {
            buttons[r][c].setIcon(new ImageIcon(flagIcon));
            minesRemaining--;
        } else {
            buttons[r][c].setIcon(new ImageIcon(tileIcon));
            minesRemaining++;
        }
        updateMineLabel();
    }
    
    public void checkWin() {
        int safeTiles = (rows * cols) - numMines;
        int revealedCount = 0;
        for(int r=0; r<rows; r++) {
            for(int c=0; c<cols; c++) {
                if(revealed[r][c]) revealedCount++;
            }
        }
        
        if(revealedCount == safeTiles) {
            gameOver = true;
            timer.stop();
            minesRemaining = 0;
            updateMineLabel();
            resetButton.setText("😎");
            JOptionPane.showMessageDialog(frame, "You Win!");
        }
    }

    public void showAllMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c] == -1 && !revealed[r][c]) {
                    buttons[r][c].setIcon(new ImageIcon(mineIcon));
                } else if (board[r][c] != -1 && flagged[r][c]) {
                   // Optional: Set a "wrong flag" icon here if you have one
                }
            }
        }
    }
    
    private void updateMineLabel() {
        minesLabel.setText(String.format("%03d", Math.max(0, minesRemaining)));
    }
    
    private void updateTimerLabel() {
        timeLabel.setText(String.format("%03d", Math.min(999, seconds)));
    }

    public void loadSprites() {
        try {
            BufferedImage sheet = null;
            try (java.io.InputStream is = getClass().getResourceAsStream("atlas.png")) {
                if (is != null) sheet = ImageIO.read(is);
            }

            if (sheet == null) {
                File f = new File("atlas.png");
                if (!f.exists()) f = new File(System.getProperty("user.dir"), "atlas.png");
                if (f.exists()) sheet = ImageIO.read(f);
            }

            if (sheet == null) {
                System.err.println("atlas.png not found.");
                JOptionPane.showMessageDialog(frame, "Could not load atlas.png", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            // Row 0
            numIcons[0] = scale(sheet.getSubimage(0, 0, 16, 16)); 
            numIcons[1] = scale(sheet.getSubimage(16, 0, 16, 16));
            numIcons[2] = scale(sheet.getSubimage(32, 0, 16, 16));
            numIcons[3] = scale(sheet.getSubimage(48, 0, 16, 16));

            // Row 1
            numIcons[4] = scale(sheet.getSubimage(0, 16, 16, 16));
            numIcons[5] = scale(sheet.getSubimage(16, 16, 16, 16));
            numIcons[6] = scale(sheet.getSubimage(32, 16, 16, 16));
            numIcons[7] = scale(sheet.getSubimage(48, 16, 16, 16));

            // Row 2
            numIcons[8] = scale(sheet.getSubimage(0, 32, 16, 16));
            tileIcon    = scale(sheet.getSubimage(16, 32, 16, 16)); 
            flagIcon    = scale(sheet.getSubimage(32, 32, 16, 16));

            // Row 3
            mineIcon    = scale(sheet.getSubimage(0, 48, 16, 16));
            mineRedIcon = scale(sheet.getSubimage(16, 48, 16, 16));
            
            // "Pressed" state is visually the same as empty "0" tile
            tilePressedIcon = numIcons[0];

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private Image scale(BufferedImage img) {
        return img.getScaledInstance(tileSize, tileSize, Image.SCALE_SMOOTH);
    }

    public static void main(String[] args) {
        new Minesweeper();
    }
}
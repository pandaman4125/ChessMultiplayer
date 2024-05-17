import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

public class ChessClient extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private char[][] board;
    private BufferedImage[][] pieceImages;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean whiteTurn;
    private boolean playerColor;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChessClient client = new ChessClient();
            client.setVisible(true);
        });
    }

    public ChessClient() {
        setTitle("Chess Client");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Connect to server
        try {
            //------------------------------------------------------------//
            // || REPLACE "localhost" with IP address of host computer || //
            // -----------------------------------------------------------//
            socket = new Socket("localhost", 12345); // Connect to localhost on port 12345
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize game state
        board = new char[8][8];
        pieceImages = new BufferedImage[8][8]; // Assuming 2 players and 6 types of pieces per player
        loadPieceImages(); // Load piece images
        startGame();
        whiteTurn = true;

        // Add mouse listener for game interaction
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int cellSize = 50;
                int margin = 10;
                int xOffset = (getWidth() - 8 * cellSize - margin) / 2;
                int yOffset = (getHeight() - 8 * cellSize - margin) / 2;
                int x = e.getX();
                int y = e.getY();
                int row = (y - margin - yOffset) / cellSize;
                int col = (x - margin - xOffset) / cellSize;

                if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                    char piece = board[row][col];
                    if (selectedRow == -1 && selectedCol == -1) {
                        if (piece != '.' && whiteTurn == Character.isUpperCase(piece) && playerColor == whiteTurn) {
                            selectedRow = row;
                            selectedCol = col;
                            repaint();
                        }
                    } else {
                        if (isValidCastling(selectedRow, selectedCol, row, col, board)) {
                            // Send move to server
                            String move = "castle " + selectedRow + " " + selectedCol + " " + row + " " + col;;
                            out.println(move); 
                            selectedRow = -1;
                            selectedCol = -1;
                        } else if (isValidMove(selectedRow, selectedCol, row, col, board) && !isCheckAfterMove(selectedRow, selectedCol, row, col)) {
                            // Send move to server
                            String move = selectedRow + " " + selectedCol + " " + row + " " + col;
                            out.println(move);
                            selectedRow = -1;
                            selectedCol = -1;
                        } else {
                            // Invalid move, reset the selection
                            selectedRow = -1;
                            selectedCol = -1;
                        }

                        // Clear selection
                        selectedRow = -1;
                        selectedCol = -1;
                    }
                }
            }
        });
        Thread messageListenerThread = new Thread(this::listenForServerMessages);
        messageListenerThread.start();
    }
    private void listenForServerMessages() {
        try {
            BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = serverIn.readLine()) != null) {
                // Process the message received from the server
                if (line.equals("TURN_WHITE")) {
                    // Update the client's turn variable to indicate it's white's turn
                    whiteTurn = true;
                } else if (line.equals("TURN_BLACK")) {
                    // Update the client's turn variable to indicate it's black's turn
                    whiteTurn = false;
                } else if (line.length() > 15) {
                    // Extract the board state from the message and update the client's board
                    String[] parts = line.split(" ");
                    updateBoardState(parts);
                    System.out.println("yippee");
                    repaint();
                } else if (line.startsWith("COLOR")) {
                    // Extract the assigned color from the message
                    String[] parts = line.split("\\s+", 2);
                    String assignedColor = parts[1];
                    // Update the client's color variable accordingly
                    if (assignedColor.equals("WHITE")) {
                        playerColor = true;
                        System.out.println("You are white");
                    } else if (assignedColor.equals("BLACK")) {
                        playerColor = false;
                        System.out.println("You are black");
                    }
                    else {
                    // Handle other types of messages from the server if needed
                }
            }
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Method to update the client's board state based on the received string
    private void updateBoardState(String[] boardState) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = boardState[i].charAt(j);
            }
        }
        System.out.println("boardupdate");
        repaint();
    }
    
    // Load piece images
    private void loadPieceImages() {
        String[] pieceTypes = { "rook", "knight", "bishop", "queen", "king", "pawn" };
        String[] colors = { "white", "black" };

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 6; j++) {
                try {
                    BufferedImage image = ImageIO.read(new File(colors[i] + pieceTypes[j] + ".png"));
                    pieceImages[i][j] = image;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // start game
    public void startGame() {
        // 'R' for Rook, 'N' for Knight, 'B' for Bishop, 'Q' for Queen, 'K' for King,
        // 'P' for Pawn
        // White pieces will be represented with uppercase letters, black pieces with
        // lowercase letters.

        // Place white pieces
        board[7] = new char[] { 'R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R' };
        for (int i = 0; i < 8; i++) {
            board[6][i] = 'P';
        }

        // Place black pieces
        board[0] = new char[] { 'r', 'n', 'b', 'q', 'k', 'b', 'n', 'r' };
        for (int i = 0; i < 8; i++) {
            board[1][i] = 'p';
        }

        // Fill empty spaces with dots
        for (int i = 2; i < 6; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = '.';
            }
        }

    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        int cellSize = 50;
        int margin = 10;

        int width = 8 * cellSize;
        int height = 8 * cellSize;

        int xOffset = (getWidth() - width - margin) / 2;
        int yOffset = (getHeight() - height - margin) / 2;

        // Draw board squares
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int x = j * cellSize + margin + xOffset;
                int y = i * cellSize + margin + yOffset;

                Color color = (i + j) % 2 == 0 ? Color.lightGray : Color.darkGray;
                g.setColor(color);
                g.fillRect(x, y, cellSize, cellSize);

                // Highlight selected square
                if (i == selectedRow && j == selectedCol) {
                    g.setColor(Color.blue);
                    g.drawRect(x, y, cellSize - 1, cellSize - 1);
                }

                // Highlight valid moves
                if (selectedRow != -1 && selectedCol != -1 && isValidMove(selectedRow, selectedCol, i, j, board) && !isCheckAfterMove(selectedRow, selectedCol, i, j)) {
                    g.setColor(new Color(0, 255, 0, 50)); // Transparent green
                    g.fillRect(x, y, cellSize, cellSize);
                }
            }
        }

        // Draw pieces
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int x = j * cellSize + margin + xOffset;
                int y = i * cellSize + margin + yOffset;

                char piece = board[i][j];
                if (piece != '.') {
                    BufferedImage img = pieceImages[Character.isUpperCase(piece) ? 0 : 1][getPieceIndex(piece)];
                    g.drawImage(img, x, y, cellSize, cellSize, null);
                }
            }
        }

        if (isCheckmate(true, board)) {
            endGameBlack(g);
        } else if (isCheckmate(false, board)) {
            endGameWhite(g);
        }
    }

    private int getPieceIndex(char piece) {
        // Returns the index of the piece in the pieceTypes array
        return switch (Character.toLowerCase(piece)) {
            case 'r' -> 0;
            case 'n' -> 1;
            case 'b' -> 2;
            case 'q' -> 3;
            case 'k' -> 4;
            case 'p' -> 5;
            default -> 7;
        };
    }

    private boolean isValidCastling(int startRow, int startCol, int endRow, int endCol, char[][] board) {
        char piece = board[startRow][startCol];
        if (piece != 'K' && piece != 'k') // Only kings can castle
            return false;
    
        if (startRow != endRow || Math.abs(startCol - endCol) != 2) // King must move 2 squares horizontally
            return false;
    
        if (isCheck(whiteTurn, board)) // Cannot castle out of check
            return false;
    
        if (piece == 'K' && startRow != 7) // White king can only castle from the initial position
            return false;
    
        if (piece == 'k' && startRow != 0) // Black king can only castle from the initial position
            return false;
    
        if (endCol == 6) { // King side castling
            if (board[startRow][7] != 'R' && board[startRow][7] != 'r') // No rook at the corner
                return false;
    
            if (board[startRow][5] != '.' || board[startRow][6] != '.') // Path is obstructed
                return false;

            // Cannot castle through check
            return !isCheckAfterMove(startRow, startCol, startRow, startCol + 1);
        } else if (endCol == 2) { // Queen side castling
            if (board[startRow][0] != 'R' && board[startRow][0] != 'r') // No rook at the corner
                return false;
    
            if (board[startRow][3] != '.' || board[startRow][2] != '.' || board[startRow][1] != '.') // Path is obstructed
                return false;

            // Cannot castle through check
            return !isCheckAfterMove(startRow, startCol, startRow, startCol - 1);
        } else {
            return false; // Invalid castling move
        }
    }

    // end game screen
    private void endGameWhite(Graphics g) {
        // Set the font and color for the "Game Over" text
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.setColor(Color.RED);
        
        // Get the size of the text
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth("White Wins!");
        int textHeight = fm.getHeight();
        
        // Calculate the position to center the text on the screen
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() - textHeight) / 2;
        
        // Draw the "Game Over" text on the screen
        g.drawString("White Wins!", x, y);
    }
    private void endGameBlack(Graphics g) {
        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.setColor(Color.RED);

        // Get the size of the text
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth("Black Wins!");
        int textHeight = fm.getHeight();

        // Calculate the position to center the text on the screen
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() - textHeight) / 2;


        g.drawString("Black Wins!", x, y);
    }
    private boolean isCheck(boolean isWhite, char[][] test) {
        // Find the king's position
        int kingRow = -1;
        int kingCol = -1;
        char kingSymbol = isWhite ? 'K' : 'k';
    
        // Iterate through the board to find the king
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (test[i][j] == kingSymbol) {
                    kingRow = i;
                    kingCol = j;
                    break;
                }
            }
        }
    
        // Iterate through the board to check for opponent's pieces that can attack the king
        char[] opponentPieces = isWhite ? new char[] { 'r', 'n', 'b', 'q', 'k', 'p' }
                : new char[] { 'R', 'N', 'B', 'Q', 'K', 'P' };
        for (char piece : opponentPieces) {
            // Generate potential moves for each opponent's piece
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    if (test[i][j] == piece && isValidMove(i, j, kingRow, kingCol, test)) {
                        return true; // King is in check
                    }
                }
            }
        }
    
        return false; // King is not in check
    }
    
    private boolean isCheckAfterMove(int startRow, int startCol, int endRow, int endCol) {
        char[][] tempBoard = cloneBoard(board); // Create a temporary board
        char piece = tempBoard[startRow][startCol];
    
        // Make the move on the temporary board
        tempBoard[endRow][endCol] = piece;
        tempBoard[startRow][startCol] = '.';
    
        // Check if the move puts the king in check on the temporary board
        return isCheck(whiteTurn, tempBoard);
    }

    private boolean isCheckmate(boolean isWhite, char[][] board) {
        // Iterate through all pieces
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] != '.' && (isWhite && Character.isUpperCase(board[i][j]) || !isWhite && Character.isLowerCase(board[i][j]))) {
                    // This is one of the player's pieces
                    for (int x = 0; x < 8; x++) {
                        for (int y = 0; y < 8; y++) {
                            if (isValidMove(i, j, x, y, board)) {
                                // If this move is valid, check if it puts the king in check
                                char[][] tempBoard = cloneBoard(board); // Create a temporary board
                                char piece = tempBoard[i][j];
                                tempBoard[x][y] = piece;
                                tempBoard[i][j] = '.';
                                if (!isCheck(isWhite, tempBoard)) {
                                    // If the king is not in check after this move, return false (not checkmate)
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        // If no move prevents the king from being in check, it's checkmate
        return true;
    }
    private char[][] cloneBoard(char[][] board) {
        char[][] clone = new char[8][8];
        for (int i = 0; i < 8; i++) {
            System.arraycopy(board[i], 0, clone[i], 0, 8);
        }
        return clone;
    }

    private boolean isValidMove(int startRow, int startCol, int endRow, int endCol, char[][] board) {
        char piece = board[startRow][startCol];
        char targetPiece = board[endRow][endCol];
    
        // If the destination is occupied by a piece of the same color, the move is invalid
        if (Character.isUpperCase(piece) && Character.isUpperCase(targetPiece)) {
            return false;
        }
        if (Character.isLowerCase(piece) && Character.isLowerCase(targetPiece)) {
            return false;
        }
    
        // Check for valid moves based on the piece type
        return switch (Character.toLowerCase(piece)) {
            case 'r' -> isValidRookMove(startRow, startCol, endRow, endCol, board);
            case 'n' -> isValidKnightMove(startRow, startCol, endRow, endCol);
            case 'b' -> isValidBishopMove(startRow, startCol, endRow, endCol, board);
            case 'q' -> isValidRookMove(startRow, startCol, endRow, endCol, board)
                    || isValidBishopMove(startRow, startCol, endRow, endCol, board);
            case 'k' -> isValidKingMove(startRow, startCol, endRow, endCol);
            case 'p' -> isValidPawnMove(startRow, startCol, endRow, endCol, board);
            default -> false; // Invalid piece
        };
    }
    
    
    

    private boolean isValidRookMove(int startRow, int startCol, int endRow, int endCol, char[][] board) {
        if (startRow == endRow) {
            // Moving horizontally
            int i = Integer.compare(endCol, startCol);
            for (int col = startCol + i; col != endCol; col += i) {
                if (board[startRow][col] != '.') {
                    return false; // Piece in the way
                }
            }
            return true;
        } else if (startCol == endCol) {
            // Moving vertically
            int i = Integer.compare(endRow, startRow);
            for (int row = startRow + i; row != endRow; row += i) {
                if (board[row][startCol] != '.') {
                    return false; // Piece in the way
                }
            }
            return true;
        } else {
            return false; // Not a valid rook move (neither horizontal nor vertical)
        }
    }

    private boolean isValidKnightMove(int startRow, int startCol, int endRow, int endCol) {
        // Knight moves in an L-shape (2 squares in one direction, 1 square
        // perpendicular)
        int rowDiff = Math.abs(endRow - startRow);
        int colDiff = Math.abs(endCol - startCol);
        return (rowDiff == 1 && colDiff == 2) || (rowDiff == 2 && colDiff == 1);
    }

    private boolean isValidBishopMove(int startRow, int startCol, int endRow, int endCol, char[][] board) {
        // Bishop moves diagonally (same absolute change in row and column)
        int rowDiff = Math.abs(endRow - startRow);
        int colDiff = Math.abs(endCol - startCol);

        // Check if the move is diagonal
        if (rowDiff != colDiff) {
            return false; // Not a diagonal move
        }

        // Direction of movement
        int rowStep = Integer.compare(endRow, startRow);
        int colStep = Integer.compare(endCol, startCol);

        for (int i = 1; i < rowDiff; i++) {
            int row = startRow + i * rowStep;
            int col = startCol + i * colStep;
            if (board[row][col] != '.') {
                return false; // piece in the way
            }
        }
        return true;
    }

    private boolean isValidKingMove(int startRow, int startCol, int endRow, int endCol) {
        // King moves one square in any direction
        int rowDiff = Math.abs(endRow - startRow);
        int colDiff = Math.abs(endCol - startCol);
        return rowDiff <= 1 && colDiff <= 1;
    }

    private boolean isValidPawnMove(int startRow, int startCol, int endRow, int endCol, char[][] board) {
        char piece = board[startRow][startCol];
        boolean isWhite = Character.isUpperCase(piece);
        int direction = isWhite ? -1 : 1;

        // Moving forward
        if (endCol == startCol && board[endRow][endCol] == '.') {
            // Regular move
            if (endRow == startRow + direction) {
                return true;
            }
            // Double move from starting position
            if (isStartingPosition(startRow, isWhite) && endRow == startRow + 2 * direction
                    && board[startRow + direction][startCol] == '.') {
                return true;
            }
        }

        // Capturing diagonally
        return Math.abs(endCol - startCol) == 1 && endRow == startRow + direction
                && Character.isUpperCase(board[endRow][endCol]) == !isWhite && board[endRow][endCol] != '.';
    }

    private boolean isStartingPosition(int row, boolean isWhite) {
        return (isWhite && row == 6) || (!isWhite && row == 1);
    }
    // Add methods to handle receiving game state updates from the server
    
}

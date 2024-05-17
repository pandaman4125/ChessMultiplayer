import java.io.*;
import java.net.*;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ChessServer server;
    private List<PrintWriter> clientOutputStreams; // List of output streams for all clients
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, ChessServer server, List<PrintWriter> clientOutputStreams) {
        System.out.println("new");
        this.clientSocket = socket;
        this.server = server;
        this.clientOutputStreams = clientOutputStreams; // Initialize list of output streams
        
        ChessServer.whiteTurn = true;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String[] move;
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                
                System.out.println("Received move from client: " + inputLine);
                // Process the move received from the client
                move = inputLine.split(" ");
                if (move[0].equals("castle")) {
                    castle(Integer.parseInt(move[1]), Integer.parseInt(move[2]), Integer.parseInt(move[3]), Integer.parseInt(move[4]), ChessServer.board);
                } else {
                ChessServer.board[Integer.parseInt(move[2])][Integer.parseInt(move[3])] = ChessServer.board[Integer.parseInt(move[0])][Integer.parseInt(move[1])];
                ChessServer.board[Integer.parseInt(move[0])][Integer.parseInt(move[1])] = '.';
                ChessServer.whiteTurn = !ChessServer.whiteTurn;
                }
                // Broadcast the whiteTurn variable and updated board state to all clients
                broadcastGameState();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void castle(int startRow, int startCol, int endRow, int endCol, char[][] board) {
        // Move the king
        board[endRow][endCol] = board[startRow][startCol];
        board[startRow][startCol] = '.';
    
        // Move the rook
        if (endCol == 6) { // King side castling
            board[startRow][5] = board[startRow][7];
            board[startRow][7] = '.';
        } else if (endCol == 2) { // Queen side castling
            board[startRow][3] = board[startRow][0];
            board[startRow][0] = '.';
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    // Method to broadcast the whiteTurn variable and updated board state to all clients
    private void broadcastGameState() {
        for (PrintWriter outputStream : clientOutputStreams) {
            // Send the whiteTurn variable to the client
            outputStream.println(ChessServer.whiteTurn ? "TURN_WHITE" : "TURN_BLACK");
            
            // Send the updated board state to the client
            StringBuilder boardState = new StringBuilder();
            for (char[] row : ChessServer.board) {
                boardState.append(String.valueOf(row)).append(" ");
            }
            System.out.print(boardState);
            outputStream.println(boardState.toString());
        }
    }
}

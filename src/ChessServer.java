import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ChessServer {
    private List<ClientHandler> clients = new ArrayList<>();
    private List<PrintWriter> clientOutputStreams = new ArrayList<>(); // List of output streams for all clients
    public static final String COLOR_WHITE = "WHITE";
    public static final String COLOR_BLACK = "BLACK";
    private int nextColorIndex = 0;
    public static boolean whiteTurn;
    public static char[][] board;
    public static void main(String[] args) {
        ChessServer server = new ChessServer();
        server.start(12345); // Start server on port 12345
    }

    public void Initialize() {
        this.whiteTurn = true;
        this.board = new char[8][8];
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

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                clientOutputStreams.add(out); // Add client's output stream to the list

                ClientHandler clientHandler = new ClientHandler(clientSocket, this, clientOutputStreams);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
                Initialize();
                String color = nextColorIndex % 2 == 0 ? COLOR_WHITE : COLOR_BLACK;
                clientHandler.sendMessage("COLOR " + color); // Send color to the client
                nextColorIndex++;
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
}


import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServiceServer {
    private static final int PORT = 43221;
    // Shared thread-safe registry of active client output streams
    private static final List<PrintWriter> clientWriters = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        System.out.println("Chat server running on port " + PORT);
        ServerSocket serverSocket = new ServerSocket(PORT);

        try {
            while (true) {
                // Blocks until a new client connects, then hands off the socket to a worker thread
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } finally {
            serverSocket.close();
        }
    }

    // Broadcasts a text message to every connected client currently in the registry
    public static void broadcast(String message) {
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println(message);
            }
        }
    }

    // Worker thread handling I/O and protocol parsing for a single client
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Register this client's stream so they receive broadcasts
                clientWriters.add(out);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    // Protocol step 1: Split command verb from data payload
                    String[] commandParts = inputLine.split("!", 2);
                    String command = commandParts[0];

                    if (command.equals("JOIN")) {
                        if (commandParts.length > 1) {
                            clientName = commandParts[1];
                            broadcast("[Server]: " + clientName + " has joined the chat!");
                        }
                    } else if (command.equals("MSG")) {
                        if (commandParts.length > 1) {
                            // Protocol step 2: Split sender identity from the chat text
                            String[] msgParts = commandParts[1].split(";", 2);
                            if (msgParts.length == 2) {
                                String sender = msgParts[0];
                                String messageText = msgParts[1];
                                broadcast(sender + ": " + messageText);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // Connection severed or dropped unexpectedly
            } finally {
                // Clean up when client disconnects
                if (out != null) {
                    clientWriters.remove(out);
                }
                if (clientName != null) {
                    broadcast("[Server]: " + clientName + " has left the chat.");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    // Socket already closed
                }
            }
        }
    }
}
import java.io.*;
import java.net.*;

public class ChatServiceClient {
    public static void main(String[] args) throws IOException {

        String hostName = "localhost";
        int portNumber = 43221;

        try {
            Socket chatSocket = new Socket(hostName, portNumber);
            PrintWriter out = new PrintWriter(chatSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

           
            System.out.print("Enter your username: ");
            String username = stdIn.readLine();
            if (username != null && !username.trim().isEmpty()) {
                out.println("JOIN!" + username.trim());
            } else {
                System.out.println("Username cannot be empty. Exiting.");
                chatSocket.close();
                return;
            }

            // Background thread, reads input from server in parallel
            Thread listenerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String incomingMessage;
                        while ((incomingMessage = in.readLine()) != null) {
                            System.out.println(incomingMessage);
                        }
                    } catch (IOException e) {
                        System.out.println("Connection to server closed.");
                    }
                }
            });
            listenerThread.start();

            // Main thread, stays focused on reading user keyboard input
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                if (!userInput.trim().isEmpty()) {
                    out.println("MSG!" + username + ";" + userInput);
                }
            }

            // Clean up socket when keyboard input stream ends 
            chatSocket.close();

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + hostName);
            System.exit(1);
        }
    }
}
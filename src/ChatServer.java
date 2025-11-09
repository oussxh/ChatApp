import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    // hold PrintWriter for broadcast and the name for user list management
    private static Map<String, PrintWriter> clients = new ConcurrentHashMap<>();
    private static final String USER_LIST_PREFIX = "/users:";

    public static void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Server started on port " + PORT);
                while (true) {
                    Socket client = serverSocket.accept();
                    new Thread(new ClientHandler(client)).start();
                }
            } catch (IOException e) {
                System.out.println("Server error: " + e.getMessage());
            }
        }).start();
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String name;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Server now waits for the name from the client GUI
                name = in.readLine(); 
                
                if (name == null || name.trim().isEmpty() || clients.containsKey(name)) {
                     // handle empty/duplicate name
                     if (name == null || name.trim().isEmpty()) {
                        out.println("[System] Name is required. Disconnecting.");
                     } else {
                        out.println("[System] Name '" + name + "' is already in use. Disconnecting.");
                     }
                     return;
                } 

                clients.put(name, out);

                broadcast(name + " joined the chat.", false); // Chat message
                sendUserListToAll(); // Update the sidebar for everyone

                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.equalsIgnoreCase("/exit")) break;
                    broadcast(name + ": " + msg, true); // True means format as chat message
                }

            } catch (IOException e) {
                System.out.println("Client error or forced disconnect.");
            } finally {
                cleanUp();
            }
        }

        private void cleanUp() {
            if (name != null) {
                clients.remove(name);
                broadcast(name + " left the chat.", false);
                sendUserListToAll(); // Update the sidebar for everyone
            }
            try { socket.close(); } catch (IOException ignored) {}
        }

        // Overloaded broadcast to allow for plain text (for system messages) or formatted chat
        private void broadcast(String msg, boolean isChatMessage) {
            String finalMsg = isChatMessage ? "**" + msg.replaceFirst(":", "**:") : "[System] " + msg;
            for (PrintWriter writer : clients.values()) {
                writer.println(finalMsg);
            }
            System.out.println("Broadcast: " + finalMsg);
        }

        // method to send the current user list to ALL clients
        private void sendUserListToAll() {
            String userList = String.join(",", clients.keySet());
            String message = USER_LIST_PREFIX + userList;
            
            for (PrintWriter writer : clients.values()) {
                writer.println(message);
            }
        }
    }
}
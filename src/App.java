import javax.swing.*;

public class App {
    public static void main(String[] args) {
        // Start the server in a separate thread
        ChatServer.startServer();

        // Wait a second for server to start
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        // Launch GUI client
        SwingUtilities.invokeLater(() -> {
            ChatClientGUI gui = new ChatClientGUI("localhost", 12345);
            gui.setVisible(true);
        });
    }
}

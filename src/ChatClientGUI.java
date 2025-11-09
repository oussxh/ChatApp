import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

public class ChatClientGUI extends JFrame {
    private JTextPane chatArea;
    private JTextField inputField;
    private JButton sendButton, exitButton;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    // UI Components for Connection and Users
    private JTextArea onlineUsersArea; 
    private JTextField usernameField; 
    private JButton connectButton;
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    // Connection Details
    private final String SERVER_HOST;
    private final int SERVER_PORT;

    // Custom Window Dragging
    private Point initialClick; 

    // Styles
    private Style defaultStyle;
    private Style boldStyle;
    
    // Dark UI Colors and Constants
    private static final Color BACKGROUND_DARK = new Color(20, 20, 28);
    private static final Color COMPONENT_DARKER = new Color(35, 35, 45);
    private static final Color ACCENT_PREMIUM = new Color(0, 174, 239); // Muted Cyan-Blue
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color ERROR_RED = new Color(200, 50, 50);
    private static final Font MODERN_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    // Protocol Constant
    private static final String USER_LIST_PREFIX = "/users:";

    public ChatClientGUI(String host, int port) {
        // Set predefined connection details
        this.SERVER_HOST = host;
        this.SERVER_PORT = port;
        
        setTitle("Oussama Chikh Chat App");
        setSize(700, 550); 
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        // Window Decoration
        setUndecorated(true);
        getRootPane().setBorder(BorderFactory.createLineBorder(ACCENT_PREMIUM, 2));

        getContentPane().setBackground(BACKGROUND_DARK);
        setLayout(new BorderLayout(10, 10)); 
        
        // Custom Title Bar
        JPanel titleBar = createCustomTitleBar();
        add(titleBar, BorderLayout.NORTH);

        // Styles for StyledDocument
        StyledDocument doc = new JTextPane().getStyledDocument(); 
        defaultStyle = doc.addStyle("default", null);
        StyleConstants.setForeground(defaultStyle, TEXT_LIGHT);
        StyleConstants.setFontFamily(defaultStyle, MODERN_FONT.getFamily());
        StyleConstants.setFontSize(defaultStyle, MODERN_FONT.getSize());

        boldStyle = doc.addStyle("bold", null);
        StyleConstants.setBold(boldStyle, true);
        StyleConstants.setForeground(boldStyle, ACCENT_PREMIUM); 
        StyleConstants.setFontFamily(boldStyle, MODERN_FONT.getFamily());
        StyleConstants.setFontSize(boldStyle, MODERN_FONT.getSize());
        
        // Chat Area
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(MODERN_FONT);
        chatArea.setBackground(COMPONENT_DARKER);
        chatArea.setForeground(TEXT_LIGHT);
        chatArea.setStyledDocument(doc); 
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(ACCENT_PREMIUM.darker(), 1)); 
        add(scrollPane, BorderLayout.CENTER);
        
        // Right Panel (Online Users and Connect)
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout(10, 10));
        rightPanel.setPreferredSize(new Dimension(180, 0));
        rightPanel.setBackground(BACKGROUND_DARK);
        rightPanel.setBorder(new EmptyBorder(10, 0, 10, 10)); 

        // 1. Online Users Display
        onlineUsersArea = new JTextArea("Status: Disconnected\n\nOnline Users:\n");
        onlineUsersArea.setEditable(false);
        onlineUsersArea.setBackground(COMPONENT_DARKER.darker()); 
        onlineUsersArea.setForeground(TEXT_LIGHT);
        onlineUsersArea.setFont(MODERN_FONT.deriveFont(Font.BOLD, 12f));
        onlineUsersArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane usersScrollPane = new JScrollPane(onlineUsersArea);
        usersScrollPane.setBorder(BorderFactory.createLineBorder(BACKGROUND_DARK.brighter(), 1)); 
        rightPanel.add(usersScrollPane, BorderLayout.CENTER);

        // 2. Connect Interface
        JPanel connectPanel = new JPanel();
        connectPanel.setLayout(new BoxLayout(connectPanel, BoxLayout.Y_AXIS));
        connectPanel.setBackground(BACKGROUND_DARK);
        
        // Username Field Setup
        usernameField = new JTextField("Oussama Chikh"); // Default starting name
        usernameField.setBackground(COMPONENT_DARKER);
        usernameField.setForeground(TEXT_LIGHT);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_PREMIUM, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5) 
        ));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, usernameField.getPreferredSize().height));
        
        connectButton = new JButton("Connect");
        customizeButton(connectButton, ACCENT_PREMIUM);
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add components to panel in desired order
        JLabel nameLabel = new JLabel("Username:");
        nameLabel.setForeground(TEXT_LIGHT);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        connectPanel.add(nameLabel);
        connectPanel.add(usernameField);
        connectPanel.add(Box.createRigidArea(new Dimension(0, 15))); 
        
        connectPanel.add(connectButton);
        
        rightPanel.add(connectPanel, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);
        
        // Bottom Panel
        JPanel bottom = new JPanel(new BorderLayout(10, 0)); 
        bottom.setBackground(BACKGROUND_DARK);

        inputField = new JTextField();
        inputField.setBackground(COMPONENT_DARKER);
        inputField.setForeground(TEXT_LIGHT);
        inputField.setFont(MODERN_FONT);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_PREMIUM.darker(), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10) 
        ));
        inputField.setEnabled(false); 

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); 
        buttonPanel.setBackground(BACKGROUND_DARK);
        
        sendButton = new JButton("Send");
        customizeButton(sendButton, ACCENT_PREMIUM);
        sendButton.setPreferredSize(new Dimension(80, 40)); 
        sendButton.setEnabled(false); 

        exitButton = new JButton("Exit");
        customizeButton(exitButton, ERROR_RED); 
        exitButton.setPreferredSize(new Dimension(80, 40));

        buttonPanel.add(exitButton);
        buttonPanel.add(sendButton);

        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(buttonPanel, BorderLayout.EAST);
        
        bottom.setBorder(new EmptyBorder(0, 10, 10, 10)); 
        add(bottom, BorderLayout.SOUTH);
        
        // Actions (Connect and Disconnect Logic)
        connectButton.addActionListener(e -> attemptConnection());
        usernameField.addActionListener(e -> attemptConnection()); // Connect on ENTER
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        
        exitButton.addActionListener(e -> {
            if (isConnected.get()) {
                closeConnection(true); 
            }
            System.exit(0);
        });
    }

    private JPanel createCustomTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(BACKGROUND_DARK.darker()); 
        titleBar.setPreferredSize(new Dimension(700, 30));

        JLabel titleLabel = new JLabel(" Multi Client Chat App");
        titleLabel.setForeground(TEXT_LIGHT);
        titleLabel.setFont(MODERN_FONT.deriveFont(Font.BOLD, 14f));
        titleBar.add(titleLabel, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controls.setBackground(BACKGROUND_DARK.darker());

        JButton minimizeButton = createControlButton("—", Color.GRAY);
        minimizeButton.addActionListener(e -> setState(JFrame.ICONIFIED));

        JButton closeButton = createControlButton("X", ERROR_RED);
        closeButton.addActionListener(e -> {
            if (isConnected.get()) {
                closeConnection(true); 
            }
            System.exit(0);
        });

        controls.add(minimizeButton);
        controls.add(closeButton);
        titleBar.add(controls, BorderLayout.EAST);

        // Drag Functionality
        titleBar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                initialClick = e.getPoint();
                getComponentAt(initialClick);
            }
        });
        titleBar.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                int thisX = getLocation().x;
                int thisY = getLocation().y;

                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                int newX = thisX + xMoved;
                int newY = thisY + yMoved;

                setLocation(newX, newY);
            }
        });

        return titleBar;
    }
    
    private JButton createControlButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setForeground(TEXT_LIGHT);
        button.setBackground(BACKGROUND_DARK.darker());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        button.setFont(MODERN_FONT.deriveFont(Font.BOLD, 14f));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(baseColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(BACKGROUND_DARK.darker());
            }
        });
        return button;
    }

    private void attemptConnection() {
        if (isConnected.get()) {
            closeConnection(true);
            return;
        }
        
        // Get Name
        String name = usernameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Use Host/Port
        String host = SERVER_HOST;
        int port = SERVER_PORT;
        
        try {
            // Establish Connection
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send Name
            out.println(name);
            
            // UI Update on Success
            isConnected.set(true);
            connectButton.setText("Disconnect");
            customizeButton(connectButton, ERROR_RED);
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            usernameField.setEnabled(false); // Disable name field while connected
            onlineUsersArea.setText("Status: Connected to " + host + ":" + port + "\n\nOnline Users:\n");
            chatArea.setText(""); 

            // Start Listener Thread
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        if (msg.startsWith(USER_LIST_PREFIX)) {
                            updateOnlineUsers(msg.substring(USER_LIST_PREFIX.length()));
                        } else {
                            appendMessage(msg);
                        }
                    }
                } catch (IOException e) {
                    // Happens on server close or forced disconnect
                } finally {
                    SwingUtilities.invokeLater(() -> closeConnection(false)); 
                }
            }).start();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot connect to " + host + ":" + port + ". Server may be offline.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            isConnected.set(false);
        }
    }
    
    private void updateOnlineUsers(String userListCsv) {
        String[] users = userListCsv.split(",");
        if (userListCsv.isEmpty() && users.length == 1 && users[0].isEmpty()) {
            users = new String[0];
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Status: Connected\n\nOnline Users (").append(users.length).append("):\n");
        for (String user : users) {
            if (!user.trim().isEmpty()) {
                sb.append("  - ").append(user.trim()).append("\n");
            }
        }
        onlineUsersArea.setText(sb.toString());
    }

    private void closeConnection(boolean informServer) {
        if (!isConnected.get()) return;
        
        try {
            if (informServer && out != null) {
                out.println("/exit");
            }
            if(socket != null) socket.close();
            
        } catch(IOException ignored) {}
        
        // Reset UI
        isConnected.set(false);
        connectButton.setText("Connect");
        customizeButton(connectButton, ACCENT_PREMIUM);
        inputField.setEnabled(false);
        sendButton.setEnabled(false);
        usernameField.setEnabled(true); // Re-enable name field
        onlineUsersArea.setText("Status: Disconnected\n\nOnline Users:\n");
        appendMessage("[System] Disconnected.");
    }

    private void customizeButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(TEXT_LIGHT);
        button.setFocusPainted(false); 
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); 
        button.setFont(MODERN_FONT.deriveFont(Font.BOLD, 15f)); 

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(Math.min(255, bgColor.getRed() + 30), Math.min(255, bgColor.getGreen() + 30), Math.min(255, bgColor.getBlue() + 30)));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty() && out != null && isConnected.get()) {
            out.println(msg);
            inputField.setText("");
        }
    }

    private void appendMessage(String msg) {
        StyledDocument doc = chatArea.getStyledDocument();
        try {
            if (msg.startsWith("**")) {
                int endBold = msg.indexOf("**", 2);
                if (endBold > 0) {
                    String username = msg.substring(2, endBold);
                    String rest = msg.substring(endBold + 2);
                    doc.insertString(doc.getLength(), username, boldStyle);
                    doc.insertString(doc.getLength(), rest + "\n", defaultStyle);
                } else {
                    doc.insertString(doc.getLength(), msg + "\n", defaultStyle);
                }
            } else {
                doc.insertString(doc.getLength(), msg + "\n", defaultStyle);
            }
            chatArea.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {}
        
        // host and port are still passed here but used internally as constants
        SwingUtilities.invokeLater(() -> new ChatClientGUI("localhost", 12345).setVisible(true));
    }
}
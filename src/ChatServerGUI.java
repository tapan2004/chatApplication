import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatServerGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField portField;
    private JButton startServerButton;
    private JButton sendButton;
    private JLabel statusLabel;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private boolean isServerRunning = false;
    private int currentPort;

    public ChatServerGUI() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Chat Server");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(650, 550);
        setLocationRelativeTo(null);

        // Create components
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(248, 249, 250));
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Chat Messages"));

        portField = new JTextField("8080", 6);
        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageField.setEnabled(false);

        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        sendButton.setPreferredSize(new Dimension(80, 30));

        startServerButton = new JButton("Start Server");
        startServerButton.setPreferredSize(new Dimension(120, 30));

        statusLabel = new JLabel("Server Status: Stopped");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Layout
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBorder(BorderFactory.createEtchedBorder());
        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);
        topPanel.add(startServerButton);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(statusLabel);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.add(new JLabel("Message:"), BorderLayout.WEST);
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Event listeners
        startServerButton.addActionListener(e -> toggleServer());
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        // Window closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
                System.exit(0);
            }
        });

        appendToChat("=== Chat Server Started ===", "SYSTEM");
        appendToChat("Set port number and click 'Start Server'", "SYSTEM");
    }

    private void toggleServer() {
        if (!isServerRunning) {
            startServer();
        } else {
            stopServer();
        }
    }

    private void startServer() {
        String portText = portField.getText().trim();

        try {
            currentPort = Integer.parseInt(portText);

            if (currentPort < 1024 || currentPort > 65535) {
                showError("Please enter a port number between 1024 and 65535");
                return;
            }

            // Create server socket with proper error handling
            serverSocket = new ServerSocket(currentPort);
            serverSocket.setReuseAddress(true);
            isServerRunning = true;

            // Update UI
            updateServerUI(true);
            appendToChat("Server started on port " + currentPort, "SYSTEM");
            appendToChat("Waiting for client connection...", "SYSTEM");

            // Start accepting connections in background
            startAcceptingConnections();

        } catch (NumberFormatException e) {
            showError("Please enter a valid port number");
        } catch (IOException e) {
            handleServerStartError(e);
        }
    }

    private void handleServerStartError(IOException e) {
        isServerRunning = false;
        String errorMsg = e.getMessage();

        if (errorMsg.contains("Address already in use") || errorMsg.contains("bind")) {
            appendToChat("Port " + currentPort + " is already in use!", "ERROR");

            // Find and suggest available ports
            int[] suggestedPorts = findAvailablePorts();
            if (suggestedPorts.length > 0) {
                StringBuilder suggestion = new StringBuilder("Try these ports: ");
                for (int i = 0; i < Math.min(3, suggestedPorts.length); i++) {
                    if (i > 0) suggestion.append(", ");
                    suggestion.append(suggestedPorts[i]);
                }
                appendToChat(suggestion.toString(), "SYSTEM");

                // Auto-set the first available port
                portField.setText(String.valueOf(suggestedPorts[0]));
            }

            showError("Port " + currentPort + " is already in use!\n\nTry port " +
                    (suggestedPorts.length > 0 ? suggestedPorts[0] : (currentPort + 1)));
        } else {
            appendToChat("Failed to start server: " + errorMsg, "ERROR");
            showError("Failed to start server: " + errorMsg);
        }
    }

    private int[] findAvailablePorts() {
        int[] testPorts = {8080, 8081, 8082, 9090, 9091, 9092, 7778, 7779, 8000, 8001};
        java.util.List<Integer> available = new java.util.ArrayList<>();

        for (int port : testPorts) {
            try (ServerSocket testSocket = new ServerSocket(port)) {
                available.add(port);
            } catch (IOException e) {
                // Port is busy
            }
        }

        return available.stream().mapToInt(Integer::intValue).toArray();
    }

    private void startAcceptingConnections() {
        new Thread(() -> {
            try {
                appendToChat("Listening for connections on port " + currentPort + "...", "SYSTEM");
                clientSocket = serverSocket.accept();

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Client Connected (Port: " + currentPort + ")");
                    statusLabel.setForeground(new Color(34, 139, 34));
                    messageField.setEnabled(true);
                    sendButton.setEnabled(true);
                    messageField.requestFocus();
                });

                appendToChat("Client connected from: " + clientSocket.getInetAddress().getHostAddress(), "SYSTEM");

                setupClientCommunication();
                startMessageReading();

            } catch (IOException e) {
                if (isServerRunning && !e.getMessage().contains("socket closed")) {
                    SwingUtilities.invokeLater(() -> {
                        appendToChat("Error accepting client connection: " + e.getMessage(), "ERROR");
                    });
                }
            }
        }, "ServerAcceptThread").start();
    }

    private void setupClientCommunication() throws IOException {
        bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    private void startMessageReading() {
        new Thread(() -> {
            try {
                String message;
                while (isServerRunning && (message = bufferedReader.readLine()) != null) {
                    if (message.equalsIgnoreCase("exit")) {
                        SwingUtilities.invokeLater(() -> {
                            appendToChat("Client requested disconnection", "SYSTEM");
                            disconnectClient();
                        });
                        break;
                    }
                    final String msg = message;
                    SwingUtilities.invokeLater(() -> appendToChat(msg, "CLIENT"));
                }
            } catch (SocketException e) {
                if (isServerRunning) {
                    SwingUtilities.invokeLater(() -> {
                        appendToChat("Client disconnected unexpectedly", "SYSTEM");
                        disconnectClient();
                    });
                }
            } catch (IOException e) {
                if (isServerRunning) {
                    SwingUtilities.invokeLater(() -> {
                        appendToChat("Connection error: " + e.getMessage(), "ERROR");
                        disconnectClient();
                    });
                }
            }
        }, "MessageReaderThread").start();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty() || printWriter == null) return;

        printWriter.println(message);
        appendToChat(message, "SERVER");
        messageField.setText("");

        if (message.equalsIgnoreCase("exit")) {
            disconnectClient();
        }
    }

    private void disconnectClient() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (printWriter != null) printWriter.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            // Ignore cleanup errors
        }

        messageField.setEnabled(false);
        sendButton.setEnabled(false);
        statusLabel.setText("Server Running - No Client (Port: " + currentPort + ")");
        statusLabel.setForeground(Color.ORANGE);

        appendToChat("Client disconnected. Waiting for new connection...", "SYSTEM");

        // Start accepting new connections
        if (isServerRunning) {
            startAcceptingConnections();
        }
    }

    private void stopServer() {
        isServerRunning = false;

        try {
            if (printWriter != null) printWriter.close();
            if (bufferedReader != null) bufferedReader.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            // Ignore cleanup errors
        }

        updateServerUI(false);
        appendToChat("Server stopped", "SYSTEM");
    }

    private void updateServerUI(boolean running) {
        if (running) {
            startServerButton.setText("Stop Server");
            portField.setEnabled(false);
            statusLabel.setText("Server Running (Port: " + currentPort + ")");
            statusLabel.setForeground(Color.ORANGE);
        } else {
            startServerButton.setText("Start Server");
            portField.setEnabled(true);
            statusLabel.setText("Server Status: Stopped");
            statusLabel.setForeground(Color.RED);
            messageField.setEnabled(false);
            sendButton.setEnabled(false);
        }
    }

    private void appendToChat(String message, String type) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String prefix;

            switch (type) {
                case "CLIENT":
                    prefix = "CLIENT";
                    break;
                case "SERVER":
                    prefix = "SERVER";
                    break;
                case "SYSTEM":
                    prefix = "SYSTEM";
                    break;
                case "ERROR":
                    prefix = "ERROR";
                    break;
                default:
                    prefix = type;
            }

            chatArea.append(String.format("[%s] %s: %s%n", timestamp, prefix, message));
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void autoStartServer() {
    }

//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            try {
//                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//            } catch (Exception e) {
//                // Use default look and feel
//            }
//            new ChatServerGUI().setVisible(true);
//        });
//    }
}
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.ConnectException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatClientGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField serverField;
    private JTextField portField;
    private JButton connectButton;
    private JButton sendButton;
    private JLabel statusLabel;

    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private boolean isConnected = false;

    public ChatClientGUI() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Chat Client");
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

        serverField = new JTextField("localhost", 12);
        portField = new JTextField("8080", 6);
        connectButton = new JButton("Connect");
        connectButton.setPreferredSize(new Dimension(100, 30));

        statusLabel = new JLabel("Status: Disconnected");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageField.setEnabled(false);

        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        sendButton.setPreferredSize(new Dimension(80, 30));

        // Layout
        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        connectionPanel.setBorder(BorderFactory.createEtchedBorder());
        connectionPanel.add(new JLabel("Server:"));
        connectionPanel.add(serverField);
        connectionPanel.add(new JLabel("Port:"));
        connectionPanel.add(portField);
        connectionPanel.add(connectButton);
        connectionPanel.add(Box.createHorizontalStrut(20));
        connectionPanel.add(statusLabel);

        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        messagePanel.add(new JLabel("Message:"), BorderLayout.WEST);
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(connectionPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(messagePanel, BorderLayout.SOUTH);

        // Event listeners
        connectButton.addActionListener(e -> toggleConnection());
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        // Window closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                System.exit(0);
            }
        });

        appendToChat("=== Chat Client Started ===", "SYSTEM");
        appendToChat("Enter server details and click Connect", "SYSTEM");
    }

    private void toggleConnection() {
        if (!isConnected) {
            connect();
        } else {
            disconnect();
        }
    }

    private void connect() {
        String serverAddress = serverField.getText().trim();
        String portText = portField.getText().trim();

        if (serverAddress.isEmpty()) {
            showError("Please enter server address");
            return;
        }

        if (portText.isEmpty()) {
            showError("Please enter port number");
            return;
        }

        try {
            int port = Integer.parseInt(portText);

            if (port < 1024 || port > 65535) {
                showError("Please enter a port number between 1024 and 65535");
                return;
            }

            appendToChat("Connecting to " + serverAddress + ":" + port + "...", "SYSTEM");

            // Update UI to show connecting state
            connectButton.setText("Connecting...");
            connectButton.setEnabled(false);
            statusLabel.setText("Status: Connecting...");
            statusLabel.setForeground(Color.ORANGE);

            // Connect in background thread
            new Thread(() -> {
                try {
                    socket = new Socket(serverAddress, port);
                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    printWriter = new PrintWriter(socket.getOutputStream(), true);

                    isConnected = true;

                    SwingUtilities.invokeLater(() -> {
                        // Update UI for successful connection
                        connectButton.setText("Disconnect");
                        connectButton.setEnabled(true);
                        statusLabel.setText("Status: Connected to " + serverAddress + ":" + port);
                        statusLabel.setForeground(new Color(34, 139, 34));
                        messageField.setEnabled(true);
                        sendButton.setEnabled(true);
                        serverField.setEnabled(false);
                        portField.setEnabled(false);
                        messageField.requestFocus();

                        appendToChat("Successfully connected to server!", "SYSTEM");
                    });

                    // Start reading messages
                    startMessageReading();

                } catch (ConnectException e) {
                    SwingUtilities.invokeLater(() -> {
                        handleConnectionError("Cannot connect to server. Make sure the server is running on " +
                                serverAddress + ":" + port);
                    });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        handleConnectionError("Connection failed: " + e.getMessage());
                    });
                }
            }, "ConnectionThread").start();

        } catch (NumberFormatException e) {
            showError("Please enter a valid port number");
            resetConnectionUI();
        }
    }

    private void handleConnectionError(String errorMessage) {
        appendToChat(errorMessage, "ERROR");
        showError(errorMessage);
        resetConnectionUI();
    }

    private void resetConnectionUI() {
        connectButton.setText("Connect");
        connectButton.setEnabled(true);
        statusLabel.setText("Status: Disconnected");
        statusLabel.setForeground(Color.RED);
    }

    private void startMessageReading() {
        new Thread(() -> {
            try {
                String message;
                while (isConnected && (message = bufferedReader.readLine()) != null) {
                    if (message.equalsIgnoreCase("exit")) {
                        SwingUtilities.invokeLater(() -> {
                            appendToChat("Server closed the connection", "SYSTEM");
                            disconnect();
                        });
                        break;
                    }
                    final String msg = message;
                    SwingUtilities.invokeLater(() -> appendToChat(msg, "SERVER"));
                }
            } catch (SocketException e) {
                if (isConnected) {
                    SwingUtilities.invokeLater(() -> {
                        appendToChat("Connection lost with server", "SYSTEM");
                        disconnect();
                    });
                }
            } catch (IOException e) {
                if (isConnected) {
                    SwingUtilities.invokeLater(() -> {
                        appendToChat("Connection error: " + e.getMessage(), "ERROR");
                        disconnect();
                    });
                }
            }
        }, "MessageReaderThread").start();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty() || printWriter == null) return;

        printWriter.println(message);
        appendToChat(message, "CLIENT");
        messageField.setText("");

        if (message.equalsIgnoreCase("exit")) {
            disconnect();
        }
    }

    private void disconnect() {
        isConnected = false;

        try {
            if (printWriter != null) {
                printWriter.println("exit");
                printWriter.close();
            }
            if (bufferedReader != null) bufferedReader.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Ignore cleanup errors
        }

        // Update UI
        connectButton.setText("Connect");
        connectButton.setEnabled(true);
        statusLabel.setText("Status: Disconnected");
        statusLabel.setForeground(Color.RED);
        messageField.setEnabled(false);
        sendButton.setEnabled(false);
        serverField.setEnabled(true);
        portField.setEnabled(true);

        appendToChat("Disconnected from server", "SYSTEM");
    }

    private void appendToChat(String message, String type) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String prefix;

            switch (type) {
                case "SERVER":
                    prefix = "SERVER";
                    break;
                case "CLIENT":
                    prefix = "CLIENT";
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

//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            try {
//                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//            } catch (Exception e) {
//                // Use default look and feel
//            }
//            new ChatClientGUI().setVisible(true);
//        });
//    }
}
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // GUI Server
        Thread guiServerThread = new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    ChatServerGUI serverGUI = new ChatServerGUI();
                    serverGUI.setVisible(true);

                    // Auto-start server on port 8080
                    serverGUI.autoStartServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, "GUI-Server-Thread");
        guiServerThread.start();

        //  GUI Client
        Thread guiClientThread = new Thread(() -> {
            try {
                Thread.sleep(2500); // Wait for GUI server to start
                SwingUtilities.invokeLater(() -> {
                    try {
                        ChatClientGUI clientGUI = new ChatClientGUI();
                        clientGUI.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "GUI-Client-Thread");
        guiClientThread.start();

        //  Server
        Thread consoleServerThread = new Thread(() -> {
            try {
                new serverSite().startServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Console-Server-Thread");
        consoleServerThread.start();

       //client
        Thread consoleClientThread = new Thread(() -> {
            try {
                Thread.sleep(2500); // Wait for console server to start
                new clientSite().startClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Console-Client-Thread");
        consoleClientThread.start();

        System.out.println("components started");
    }
}

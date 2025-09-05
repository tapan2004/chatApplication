import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class clientSite {

    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;

    // Start the client
    public void startClient() {
        try {
            System.out.println("Connecting to server...");
            socket = new Socket("localhost", 7777);
            System.out.println("Connected to server: " + socket);

            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(socket.getOutputStream(), true);

            startReading();
            startWriting();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Thread for reading messages from server
    private void startReading() {
        Runnable readerTask = () -> {
            System.out.println("Reader started...");
            try {
                while (true) {
                    String msg = bufferedReader.readLine();
                    if (msg == null || msg.equalsIgnoreCase("exit")) {
                        System.out.println("Server disconnected...");
                        closeResources();
                        break;
                    }
                    System.out.println("Server: " + msg);
                }
            } catch (IOException e) {
                System.out.println("Connection closed.");
            }
        };
        new Thread(readerTask).start();
    }

    // Thread for writing messages to server
    private void startWriting() {
        Runnable writerTask = () -> {
            System.out.println("Writer started...");
            try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    String content = consoleReader.readLine();
                    printWriter.println(content);
                    if (content.equalsIgnoreCase("exit")) {
                        closeResources();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        new Thread(writerTask).start();
    }

    // Close all resources properly
    private void closeResources() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (printWriter != null) printWriter.close();
            if (socket != null) socket.close();
            System.out.println("Client closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    // Entry point
//    public static void main(String[] args) {
//        System.out.println("Starting client...");
//        new clientSite().startClient();
//    }
}
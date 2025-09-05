import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class serverSite {

    private ServerSocket serverSocket;
    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;

    // Start the server
    public void startServer() {
        try {
            serverSocket = new ServerSocket(7777);
            System.out.println("Server is ready to accept connection...");
            System.out.println("Waiting for client...");

            socket = serverSocket.accept();
            System.out.println("Client connected: " + socket);

            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(socket.getOutputStream(), true);

            startReading();
            startWriting();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Thread for reading messages from client
    private void startReading() {
        Runnable readerTask = () -> {
            System.out.println("Reader started...");
            try {
                while (true) {
                    String msg = bufferedReader.readLine();
                    if (msg == null || msg.equalsIgnoreCase("exit")) {
                        System.out.println("Client disconnected...");
                        closeResources();
                        break;
                    }
                    System.out.println("Client: " + msg);
                }
            } catch (IOException e) {
                System.out.println("Connection closed.");
            }
        };
        new Thread(readerTask).start();
    }

    // Thread for writing messages to client
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
            if (serverSocket != null) serverSocket.close();
            System.out.println("Server closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    // Entry point
//    public static void main(String[] args) {
//        System.out.println("Starting server...");
//        new serverSite().startServer();
//    }
}
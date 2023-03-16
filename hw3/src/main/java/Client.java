import java.io.*;
import java.net.Socket;

public class Client {
    private String filename;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public Client(String host, int port, String filename) {
        try {
            this.filename = filename;
            clientSocket = new Socket(host, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void start() {
        try {
            System.out.println("-- CLIENT STARTED --");
            String request = "GET /file=" + filename + " HTTP/1.1";
            out.println(request);
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("-- CLIENT FINISHED --");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Invalid number of argumnets. Usage: ./Client <host> <port> <filename>");
            return;
        }
        Client client = new Client(args[0], Integer.parseInt(args[1]), args[2]);
        client.start();
    }
}

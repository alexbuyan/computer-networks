import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Time;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
    private final ThreadPoolExecutor threadPoolExecutor; // for multithreading support

    private final ServerSocket serverSocket;

    private static final int port = 9000; // port server is running on

    Server(int nThreads) throws IOException {
        serverSocket = new ServerSocket(port);
        threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads);
    }

    public void start() {
        try {
            System.out.println("-- SERVER STARTED --");
            // wait for connections and process them
            while (serverSocket.isBound() && !serverSocket.isClosed()) {
                threadPoolExecutor.execute(new ClientRequestHandler(serverSocket.accept()));
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        int nThreads = 10;
        if (args.length > 0) {
            nThreads = Integer.parseInt(args[0]);
        }
        Server server = new Server(nThreads);
        server.start();
    }

    class ClientRequestHandler implements Runnable {
        private final Socket clientSocket;
        private static final Path dirPath = Paths.get("hw3/src/main/resources/");
        private static final File dir = new File(dirPath.toString());
        private static final String CRLF = "\r\n";

        ClientRequestHandler(Socket socket) {
            clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                System.out.println("-- [" + clientSocket.getPort() + "] CLIENT CONNECTED --");

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream();

                // handle request
                System.out.println("-- [" + clientSocket.getPort() + "] HANDLING REQUEST --");
                String request = in.readLine();
                List<String> requestTokens = Arrays.stream(request.split(" ")).toList();

                // response values;
                String status = null;
                String contentType = null;
                String responseBody = null;
                StringBuilder response = new StringBuilder();
                String foundFile = null;

                // process request
                System.out.println("-- [" + clientSocket.getPort() + "] PROCESSING REQUEST --");
                if (requestTokens.get(0).equals("GET")) {
                    String query = requestTokens.get(1);
                    List<String> queryTokens = Arrays.stream(query.split("=")).toList();
                    if (queryTokens.size() == 1) {
                        System.out.println("-- [" + clientSocket.getPort() + "] CREATING RESPONSE --");

                        status = "HTTP/1.1 400 Bad Request" + CRLF;
                        contentType = "Content-Type: text/html" + CRLF;
                        responseBody = "<html><body>Invalid Request: try http://localhost:9000/file=filename</body></html>";

                        System.out.println("-- [" + clientSocket.getPort() + "] WRITING RESPONSE --");
                        response.append(status).append(contentType).append(CRLF).append(responseBody);
                        out.write(response.toString().getBytes());
                        out.flush();

                        in.close();
                        out.close();
                        clientSocket.close();
                        System.out.println("-- [" + clientSocket.getPort() + "] CLIENT GOT RESPONSE --");
                        return;
                    }
                    String requestedFilename = queryTokens.get(1);
                    System.out.println("-- [" + clientSocket.getPort() + "] REQUESTED FILENAME : " + requestedFilename + " --");
                    System.out.println("-- [" + clientSocket.getPort() + "] DIRECTORY CHECK STARTED --");
                    try (DirectoryStream<Path> dStream = Files.newDirectoryStream(dirPath)) {
                        for (Path filePath : dStream) {
                            String fileName = filePath.getFileName().toString();
                            if (fileName.equals(requestedFilename)) {
                                status = "HTTP/1.1 200 OK" + CRLF;
                                contentType = "Content-Type: text/html" + CRLF;
                                foundFile = dirPath + "/" + fileName;
                                System.out.println("-- [" + clientSocket.getPort() + "] FILE FOUND --");
                                break;
                            }
                        }
                        if (foundFile == null) {
                            System.out.println("-- [" + clientSocket.getPort() + "] FILE NOT FOUND --");
                        }
                    }
                    System.out.println("-- [" + clientSocket.getPort() + "] DIRECTORY CHECK FINISHED --");

                    System.out.println("-- [" + clientSocket.getPort() + "] CREATING RESPONSE --");
                    if (status == null) {
                        status = "HTTP/1.1 404 Not Found" + CRLF;
                        contentType = "Content-Type: text/html" + CRLF;
                    }
                    if (foundFile == null) {
                        responseBody = "<html><body>" + requestedFilename + " not found!</body></html>";
                    } else {
                        File file = new File(foundFile);
                        responseBody = new String(Files.readAllBytes(file.toPath()));
                    }
                    System.out.println("-- [" + clientSocket.getPort() + "] WRITING RESPONSE --");
                    response.append(status).append(contentType).append(CRLF).append(responseBody);
                    out.write(response.toString().getBytes());
                    out.flush();
                }

                in.close();
                out.close();
                clientSocket.close();
                System.out.println("-- [" + clientSocket.getPort() + "] CLIENT GOT RESPONSE --");
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ServerMultiThread {
    private static final Path dirPath = Paths.get("hw3/src/main/resources/");
    private static final File dir = new File(dirPath.toString());


    private static final int port = 10300;
    private static final int defaultNumberOfThreads = 10;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        HttpContext context = server.createContext("/", ServerMultiThread::httpHandler);


        int numberOfThreads = defaultNumberOfThreads;
        if (args.length > 0) {
            numberOfThreads = Integer.parseInt(args[0]);
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        server.setExecutor(executor);
        server.start();
    }

    private static void httpHandler(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        Map<String, String> queryAttributes = URLEncodedUtils.parse(requestURI, Charset.defaultCharset()).stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        String requestedFileName = queryAttributes.get("file");
        System.out.println("requested filename: " + requestedFileName);
        System.out.println("checking directory: " + dir.getAbsolutePath());
        System.out.println("Size of directory: " + Objects.requireNonNull(dir.listFiles()).length + " files");

        try (DirectoryStream<Path> dStream = Files.newDirectoryStream(dirPath)) {
            for (Path filePath : dStream) {
                String fileName = filePath.getFileName().toString();
                System.out.println("checking file: " + fileName);
                if (fileName.equals(requestedFileName)) {
                    File file = Paths.get(dirPath.toString(), fileName).toFile();
                    FileInputStream fout = new FileInputStream(file);
                    byte[] data = fout.readAllBytes();
                    exchange.sendResponseHeaders(200, data.length);
                    OutputStream out = exchange.getResponseBody();
                    out.write(data);
                    out.close();
                    return;
                }
            }
        }

        String response = "File not found: " + requestedFileName;
        exchange.sendResponseHeaders(404, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}

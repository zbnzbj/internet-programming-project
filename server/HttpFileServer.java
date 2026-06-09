package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;

/**
 * 基于原生 HTTP 的文件共享服务器 (HTTP File Server)
 * 核心技术：com.sun.net.httpserver.HttpServer
 * 功能：
 * 1. 监听特定的 HTTP 端口（28000），提供跨平台的文件传输支持。
 * 2. 提供 `/upload` 接口接收客户端上传的文件并存储至服务端本地的 uploads 文件夹。
 * 3. 提供 `/download` 接口允许客户端通过标准 HTTP GET 请求下载文件。
 */
public class HttpFileServer {
    private final int port;
    private final String uploadDir = "uploads/";

    public HttpFileServer(int port) {
        this.port = port;
        new File(uploadDir).mkdirs();
    }

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/upload", new UploadHandler());
            server.createContext("/download", new DownloadHandler());
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); // thread pool for HTTP
            server.start();
            ServerLog.info("HTTP File Server started on port " + port);
        } catch (IOException e) {
            ServerLog.error("HTTP File Server error: " + e.getMessage());
        }
    }

    class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String fileName = exchange.getRequestHeaders().getFirst("File-Name");
                if (fileName == null || fileName.isEmpty()) {
                    fileName = "unknown_" + System.currentTimeMillis();
                }

                File file = new File(uploadDir + fileName);
                try (InputStream is = exchange.getRequestBody();
                     FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                
                ServerLog.info("File uploaded via HTTP: " + fileName);
                String response = "File uploaded successfully";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }

    class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("file=")) {
                    String fileName = query.substring(5);
                    File file = new File(uploadDir + fileName);
                    
                    if (file.exists() && !file.isDirectory()) {
                        exchange.sendResponseHeaders(200, file.length());
                        try (OutputStream os = exchange.getResponseBody();
                             FileInputStream fis = new FileInputStream(file)) {
                            byte[] buffer = new byte[4096];
                            int count;
                            while ((count = fis.read(buffer)) != -1) {
                                os.write(buffer, 0, count);
                            }
                        }
                        ServerLog.info("File downloaded via HTTP: " + fileName);
                    } else {
                        String response = "404 Not Found";
                        exchange.sendResponseHeaders(404, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                } else {
                    exchange.sendResponseHeaders(400, -1);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}

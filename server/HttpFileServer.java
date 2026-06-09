package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;

/**
 * [EN] Native HTTP File Sharing Server
 * Core technology: com.sun.net.httpserver.HttpServer
 * Features:
 * 1. Listens on a specific HTTP port (28000) for cross-platform file transfers.
 * 2. Provides '/upload' endpoint for clients to upload files to the server's 'uploads' directory.
 * 3. Provides '/download' endpoint for clients to download files via standard HTTP GET requests.
 *
 * [ZH] 基于原生 HTTP 的文件共享服务器
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
        // [EN] Create the uploads directory if it does not exist
        // [ZH] 如果上传文件夹不存在则自动创建
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void start() {
        try {
            // [EN] Create the HTTP server and bind it to the port
            // [ZH] 创建 HTTP 服务器并绑定至指定端口
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/upload", new UploadHandler());
            server.createContext("/download", new DownloadHandler());
            // [EN] Use a thread pool to handle concurrent HTTP requests
            // [ZH] 使用线程池并发处理 HTTP 请求
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); 
            server.start();
            ServerLog.info("HTTP File Server started on port " + port);
        } catch (IOException e) {
            ServerLog.error("HTTP File Server failed: " + e.getMessage());
        }
    }

    // [EN] Handler for processing file upload requests (HTTP POST)
    // [ZH] 负责处理客户端文件上传请求的处理器 (HTTP POST)
    class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String filename = exchange.getRequestHeaders().getFirst("File-Name");
                if (filename == null || filename.isEmpty()) {
                    filename = "uploaded_" + System.currentTimeMillis();
                }
                
                File targetFile = new File(uploadDir, filename);
                try (InputStream is = exchange.getRequestBody();
                     FileOutputStream fos = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, count);
                    }
                }
                String response = "File uploaded successfully.";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                ServerLog.info("File uploaded: " + filename);
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }

    // [EN] Handler for processing file download requests (HTTP GET)
    // [ZH] 负责处理客户端文件下载请求的处理器 (HTTP GET)
    class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String filename = query != null && query.startsWith("file=") ? query.substring(5) : null;
                
                if (filename != null) {
                    File file = new File(uploadDir, filename);
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
                        ServerLog.info("File downloaded: " + filename);
                        return;
                    }
                }
                exchange.sendResponseHeaders(404, -1); // Not Found
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }
}

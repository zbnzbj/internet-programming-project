package server;

import java.util.Scanner;

/**
 * 服务端启动入口类 (Main Entry Point)
 * 该类负责统筹并启动所有的服务端组件，包括：
 * 1. NioChatServer (基于 NIO 的高性能消息分发服务器)
 * 2. AuthServer (基于 SSL/TLS 的安全认证注册/登录服务器)
 * 3. UdpBroadcaster (UDP 服务端自动发现广播)
 * 4. HttpFileServer (基于 HTTP 的大文件上传/下载服务器)
 * 5. WebAdminServer (可视化的网页端管理员控制台)
 */
public class ServerMain {
    public static final int AUTH_PORT = 28443;
    public static final int CHAT_PORT = 28080;
    public static final int UDP_PORT = 28888;
    public static final int HTTP_PORT = 28000;
    public static final int WEB_ADMIN_PORT = 28081;

    public static void main(String[] args) {
        System.out.println("Starting Server Components...");
        
        // Initialize Database
        Database db = new Database();
        
        // Start Authentication Server (SSL/TLS)
        AuthServer authServer = new AuthServer(AUTH_PORT, db);
        new Thread(authServer).start();

        // Start Chat Server (NIO)
        NioChatServer chatServer = new NioChatServer(CHAT_PORT);
        new Thread(chatServer).start();

        // Start UDP Broadcaster
        UdpBroadcaster udpBroadcaster = new UdpBroadcaster(UDP_PORT, CHAT_PORT);
        new Thread(udpBroadcaster).start();

        // Start HTTP File Server
        HttpFileServer httpFileServer = new HttpFileServer(HTTP_PORT);
        httpFileServer.start();

        // Start Web Admin Server
        WebAdminServer webAdminServer = new WebAdminServer(WEB_ADMIN_PORT, chatServer);
        webAdminServer.start();

        ServerLog.info("All server components started successfully.");
        System.out.println("\n--- Type 'quit' to shutdown server, or /kick <user> ---");

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if ("quit".equalsIgnoreCase(line)) {
                ServerLog.info("Server shutting down...");
                System.exit(0);
            } else if (line.startsWith("/kick ")) {
                // Since NioChatServer manages clients, we would typically call a method on it.
                // For simplicity, we can just say admin should use an admin client connection.
                System.out.println("Please connect using an admin client to issue /kick commands, or restart server.");
            } else {
                System.out.println("Unknown command.");
            }
        }
    }
}

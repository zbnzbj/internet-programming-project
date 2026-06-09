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
        
        // 初始化数据库连接对象，供各组件共享使用
        Database db = new Database();
        
        // 2. 初始化并启动基于 SSL/TLS 的安全认证服务器，专职处理高密级的注册登录
        AuthServer authServer = new AuthServer(AUTH_PORT, db);
        new Thread(authServer).start();

        // 1. 初始化并启动基于 NIO 的核心聊天服务器，放入独立线程运行，防止阻塞主线程
        NioChatServer chatServer = new NioChatServer(CHAT_PORT);
        new Thread(chatServer).start();

        // 3. 启动 UDP 广播服务，每隔一段时间向局域网大喊自己的存在，方便客户端自动连入
        UdpBroadcaster udpBroadcaster = new UdpBroadcaster(UDP_PORT, CHAT_PORT);
        new Thread(udpBroadcaster).start();

        // 4. 启动轻量级 HTTP 文件服务器，用于接收客户端的文件上传和下载请求
        HttpFileServer httpFileServer = new HttpFileServer(HTTP_PORT);
        httpFileServer.start();

        // 5. 启动可视化的 Web 端管理员控制台，监听 28081 端口
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

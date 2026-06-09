package server;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * [EN] Secure Authentication Server
 * Core technology: End-to-End Encryption using SSLServerSocket.
 * Features: Processes client registration (REGISTER) and login (LOGIN) requests,
 * ensuring that passwords and sensitive data are not intercepted during network transmission.
 *
 * [ZH] 安全认证服务器
 * 核心技术：使用 SSLServerSocket 提供端到端加密 (End-to-End Encryption)。
 * 功能：处理客户端的注册 (REGISTER) 和登录 (LOGIN) 请求，确保密码等敏感信息
 * 在网络传输过程中不会被中间人抓包窃听。
 */
public class AuthServer implements Runnable {
    private final int port;
    private final Database db;
    // [EN] Thread pool to handle multiple authentication requests concurrently
    // [ZH] 线程池用于并发处理多个身份验证请求
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public AuthServer(int port, Database db) {
        this.port = port;
        this.db = db;
    }

    @Override
    public void run() {
        try {
            // [EN] Load the Java keystore containing the server's SSL certificate
            // [ZH] 加载包含服务端 SSL 证书的 Java 密钥库
            System.setProperty("javax.net.ssl.keyStore", "server_keystore.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "password");

            SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port);
            ServerLog.info("SSL Auth Server started on port " + port);

            // [EN] Continuously listen for incoming secure connection requests
            // [ZH] 持续监听传入的安全连接请求
            while (true) {
                Socket client = serverSocket.accept();
                threadPool.execute(() -> handleAuthRequest(client));
            }
        } catch (Exception e) {
            ServerLog.error("SSL Server error: " + e.getMessage());
        }
    }

    // [EN] Handle a single authentication request
    // [ZH] 处理单次认证请求
    private void handleAuthRequest(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

            // [EN] Read the authentication command string (e.g. "LOGIN username password")
            // [ZH] 读取身份验证指令字符串（例如："LOGIN username password"）
            String request = in.readLine();
            if (request == null) return;

            String[] parts = request.split(" ");
            if (parts.length == 3) {
                String cmd = parts[0];
                String user = parts[1];
                String pass = parts[2];

                // [EN] Process registration or login based on the command
                // [ZH] 根据指令处理注册或登录
                if ("REGISTER".equals(cmd)) {
                    boolean success = db.registerUser(user, pass);
                    out.println(success ? "SUCCESS" : "FAIL");
                } else if ("LOGIN".equals(cmd)) {
                    boolean success = db.verifyUser(user, pass);
                    if (success) ServerLog.info("User logged in: " + user);
                    else ServerLog.warning("Failed login attempt for user: " + user);
                    out.println(success ? "SUCCESS" : "FAIL");
                }
            }
        } catch (IOException e) {
            ServerLog.error("Auth client error: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }
}

package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * [EN] Web Admin Dashboard Server (High-Score Extension)
 * Core technology: Lightweight HTTP interface built with HttpServer.
 * Features:
 * 1. Listens on port 28081, allowing admins to access the control panel via a browser.
 * 2. Dynamically reads and displays the current list of online users.
 * 3. Provides a graphical "Kick" button, invoking NioChatServer's API to disconnect users.
 *
 * [ZH] 网页端管理员控制台 [高分附加项]
 * 核心技术：基于 HttpServer 搭建的微型 HTTP Web 界面。
 * 功能：
 * 1. 监听 28081 端口，管理员可以通过浏览器直接访问后台管理页面。
 * 2. 动态读取并展示当前在线的用户列表。
 * 3. 提供图形化的“踢出 (Kick)”按钮，通过调用 NioChatServer 的底层接口将用户强制下线。
 */
public class WebAdminServer {
    private final int port;
    private final NioChatServer chatServer;

    public WebAdminServer(int port, NioChatServer chatServer) {
        this.port = port;
        this.chatServer = chatServer;
    }

    public void start() {
        try {
            // [EN] Create the HTTP server and bind it to the admin port
            // [ZH] 创建 HTTP 服务器并绑定至管理员面板专用端口
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new DashboardHandler());
            server.createContext("/api/kick", new KickHandler());
            server.setExecutor(null); // [EN] Default executor / [ZH] 默认执行器
            server.start();
            ServerLog.info("Web Admin Server started on http://localhost:" + port);
        } catch (IOException e) {
            ServerLog.error("Web Admin Server failed: " + e.getMessage());
        }
    }

    // [EN] Handler for rendering the HTML dashboard
    // [ZH] 负责渲染 HTML 网页控制台的处理器
    class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Admin Dashboard</title>");
            html.append("<style>body{font-family: Arial; padding: 20px;} table{border-collapse: collapse; width: 100%;} th, td{border: 1px solid #ddd; padding: 8px;} th{background-color: #f2f2f2;} button{background-color: #f44336; color: white; border: none; padding: 5px 10px; cursor: pointer;}</style>");
            html.append("<script>function kickUser(user) { fetch('/api/kick?user=' + user, {method: 'POST'}).then(() => location.reload()); }</script>");
            html.append("</head><body>");
            html.append("<h2>Chat Server Admin Dashboard</h2>");
            html.append("<table><tr><th>Username</th><th>Action</th></tr>");

            // [EN] Retrieve active users from the chat server
            // [ZH] 从聊天主干服务器获取当前活跃的用户列表
            for (String user : chatServer.getOnlineUsers()) {
                html.append("<tr><td>").append(user).append("</td>");
                html.append("<td><button onclick=\"kickUser('").append(user).append("')\">Kick</button></td></tr>");
            }

            html.append("</table></body></html>");

            // [EN] Send HTML response
            // [ZH] 将生成的 HTML 响应发送回浏览器
            byte[] response = html.toString().getBytes();
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    // [EN] API Handler for the Kick action
    // [ZH] 负责处理前端 Kick 点击事件的 API 处理器
    class KickHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("user=")) {
                    String user = query.substring(5);
                    // [EN] Invoke the NioChatServer API to drop the user
                    // [ZH] 调用聊天服务器的接口，强制断开指定用户的连接
                    chatServer.kickUser(user);
                }
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
            }
        }
    }
}

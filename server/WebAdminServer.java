package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class WebAdminServer {
    private final int port;
    private final NioChatServer chatServer;

    public WebAdminServer(int port, NioChatServer chatServer) {
        this.port = port;
        this.chatServer = chatServer;
    }

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new DashboardHandler());
            server.createContext("/kick", new KickHandler());
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            ServerLog.info("Web Admin Server started on port " + port);
        } catch (IOException e) {
            ServerLog.error("Web Admin Server error: " + e.getMessage());
        }
    }

    class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Admin Dashboard</title>");
            html.append("<style>body{font-family: Arial, sans-serif; margin: 40px;} ");
            html.append("table{border-collapse: collapse; width: 50%;} th, td{border: 1px solid #ddd; padding: 8px;} th{background-color: #f2f2f2;}</style>");
            html.append("</head><body>");
            html.append("<h2>Chat Room Admin Dashboard</h2>");
            html.append("<h3>Online Users</h3>");
            html.append("<table><tr><th>Username</th><th>Action</th></tr>");

            for (String user : chatServer.getOnlineUsers()) {
                html.append("<tr>");
                html.append("<td>").append(user).append("</td>");
                html.append("<td><a href='/kick?user=").append(user).append("'><button>Kick</button></a></td>");
                html.append("</tr>");
            }

            if (chatServer.getOnlineUsers().isEmpty()) {
                html.append("<tr><td colspan='2'>No users online</td></tr>");
            }

            html.append("</table></body></html>");

            byte[] response = html.toString().getBytes();
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    class KickHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("user=")) {
                String targetUser = query.substring(5);
                chatServer.kickUser(targetUser);
                ServerLog.info("Web Admin kicked user: " + targetUser);
            }
            // Redirect back to dashboard
            exchange.getResponseHeaders().add("Location", "/");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }
    }
}

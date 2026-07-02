package client;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.cert.X509Certificate;

/**
 * [EN] Core Network Communication Component (Network Client)
 * Core Responsibilities:
 * 1. Auto-discovery: Listens to port 28888 for UDP broadcasts to automatically find the server IP.
 * 2. Secure Auth: Establishes an SSLSocket connection to port 28443 for encrypted login/registration.
 * 3. Chat Comm: Uses standard TCP Sockets to connect to the server's NIO chat backbone.
 * 4. Advanced Net: Provides SOCKS proxy support and handles dual-stack DNS resolution.
 * 5. File Transfer: Uses HttpURLConnection for POST uploads and GET downloads.
 *
 * [ZH] 客户端核心网络通信组件
 * 核心职责：
 * 1. 自动发现：监听 28888 端口的 UDP 广播，实现局域网内服务器 IP 的自动发现。
 * 2. 安全认证：通过 SSLSocket 与服务端的 28443 端口建立强加密连接，进行登录注册。
 * 3. 聊天通信：通过标准的 TCP Socket 接入服务端的 NIO 聊天主干。
 * 4. 高级网络：提供 SOCKS 代理流量转发支持，并使用 InetAddress 处理双栈 DNS 解析。
 * 5. 文件传输：使用 HttpURLConnection 发送 POST 上传和 GET 下载请求。
 */
public class NetworkClient {
    private String serverIp = "127.0.0.1";
    private final int authPort = 28443;
    private final int chatPort = 28080;
    
    private Socket chatSocket;
    private BufferedReader chatIn;
    private PrintWriter chatOut;
    private String username;
    
    private String proxyIp;
    private int proxyPort;
    private Proxy proxy;
    
    private ChatGUI gui;

    public NetworkClient() {
        // [EN] Start a background thread specifically to listen for UDP discovery broadcasts
        // [ZH] 在后台启动一个独立线程，专门用于监听 UDP 广播以自动发现服务端 IP
        new Thread(this::listenForUdpDiscovery).start();
    }

    public void setGui(ChatGUI gui) {
        this.gui = gui;
    }
    
    public void setServerIp(String ip) {
        this.serverIp = ip;
    }

    // [EN] Configure SOCKS proxy settings dynamically
    // [ZH] 动态配置 SOCKS 代理设置
    public void setProxy(String ip, int port) {
        this.proxyIp = ip;
        this.proxyPort = port;
        if (ip != null && !ip.isEmpty()) {
            this.proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(ip, port));
            System.out.println("[DNS/Proxy] Configured SOCKS proxy: " + ip + ":" + port);
        } else {
            this.proxy = null;
        }
    }
    
    // [EN] Resolves hostname to both IPv4 and IPv6 addresses to verify DNS capabilities
    // [ZH] 将主机名解析为 IPv4 和 IPv6 地址，以验证双栈 DNS 解析能力
    private void resolveHostnameAndIPv6() {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(serverIp);
            System.out.println("[DNS] Resolved '" + serverIp + "' to the following IPs:");
            for (InetAddress addr : addresses) {
                if (addr instanceof Inet6Address) {
                    System.out.println("      - [IPv6] " + addr.getHostAddress());
                } else {
                    System.out.println("      - [IPv4] " + addr.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("[DNS] Failed to resolve hostname: " + serverIp);
        }
    }

    // [EN] Listens for UDP broadcasts to automatically find the server IP.
    // [ZH] 监听 UDP 广播以自动发现服务端 IP。如果检测到，将自动把 serverIp 替换为发现的 IP
    private void listenForUdpDiscovery() {
        try (DatagramSocket socket = new DatagramSocket(28888)) {
            byte[] buf = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                if (msg.startsWith("SERVER_DISCOVERY:")) {
                    String ip = packet.getAddress().getHostAddress();
                    if (!ip.equals(this.serverIp)) {
                        this.serverIp = ip;
                        if (gui != null) {
                            gui.appendSystemMessage("Auto-discovered server at " + ip);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("UDP Discovery not active or port in use: " + e.getMessage());
        }
    }

    // [EN] Bypass SSL validation since we are using a self-signed temporary certificate
    // [ZH] 绕过 SSL 证书校验（因为我们使用的是自签名的临时证书，直接信任所有证书）
    private SSLSocketFactory getTrustAllSocketFactory() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        return sc.getSocketFactory();
    }

    public boolean register(String user, String pass) {
        return sendAuthRequest("REGISTER", user, pass);
    }

    public boolean login(String user, String pass) {
        boolean success = sendAuthRequest("LOGIN", user, pass);
        if (success) {
            this.username = user;
            connectToChatServer();
        }
        return success;
    }

    // [EN] Send registration or login request securely over SSL/TLS
    // [ZH] 通过 SSL/TLS 安全通道发送注册或登录请求
    private boolean sendAuthRequest(String cmd, String user, String pass) {
        resolveHostnameAndIPv6();
        try {
            SSLSocketFactory factory = getTrustAllSocketFactory();
            Socket underlyingSocket;
            // [EN] Support SOCKS Proxy if configured / [ZH] 若已配置则支持 SOCKS 代理
            if (proxy != null) {
                underlyingSocket = new Socket(proxy);
                underlyingSocket.connect(new InetSocketAddress(serverIp, authPort));
            } else {
                underlyingSocket = new Socket(serverIp, authPort);
            }
            
            try (SSLSocket socket = (SSLSocket) factory.createSocket(underlyingSocket, serverIp, authPort, true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                 
                socket.startHandshake();
                out.println(cmd + " " + user + " " + pass);
                String response = in.readLine();
                return "SUCCESS".equals(response);
            }
        } catch (Exception e) {
            if (gui != null) gui.appendSystemMessage("Auth connection error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // [EN] Connect to the main NIO Chat Server after successful authentication
    // [ZH] 在成功认证后，连接至主要的 NIO 聊天服务器
    private void connectToChatServer() {
        try {
            if (proxy != null) {
                chatSocket = new Socket(proxy);
                chatSocket.connect(new InetSocketAddress(serverIp, chatPort));
            } else {
                chatSocket = new Socket(serverIp, chatPort);
            }
            chatIn = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
            chatOut = new PrintWriter(chatSocket.getOutputStream(), true);
            
            // [EN] Send JOIN protocol message to declare presence
            // [ZH] 向服务端发送 JOIN 协议消息，宣告当前用户名加入聊天室
            chatOut.println("JOIN " + username);
            
            // [EN] Start a dedicated thread to continuously listen for incoming messages
            // [ZH] 启动一个专门的线程持续监听服务端发来的聊天消息
            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            if (gui != null) gui.appendSystemMessage("Failed to connect to chat server.");
        }
    }

    // [EN] Thread method to read incoming data from the server
    // [ZH] 线程主方法，用于从服务端读取传入的数据流
    private void listenForMessages() {
        try {
            String msg;
            while ((msg = chatIn.readLine()) != null) {
                if (msg.startsWith("ONLINE_USERS ")) {
                    // [EN] Update GUI user list / [ZH] 更新界面的用户列表
                    String[] users = msg.substring(13).split(",");
                    if (gui != null) gui.updateOnlineUsers(users);
                } else {
                    // [EN] Update GUI chat area / [ZH] 更新界面的聊天记录
                    if (gui != null) gui.appendMessage(msg);
                }
            }
        } catch (IOException e) {
            if (gui != null) gui.appendSystemMessage("Disconnected from server.");
        }
    }

    // [EN] Send standard text message / [ZH] 发送标准文本消息
    public void sendMessage(String msg) {
        if (chatOut != null) {
            chatOut.println(msg);
        }
    }

    public String getUsername() {
        return username;
    }
    
    // [EN] Upload a file using HTTP POST request
    // [ZH] 使用 HTTP POST 请求上传文件
    public void uploadFile(File file) {
        new Thread(() -> {
            try {
                URL url = new URL("http://" + serverIp + ":28000/upload");
                HttpURLConnection conn;
                if (proxy != null) {
                    conn = (HttpURLConnection) url.openConnection(proxy);
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("File-Name", file.getName());
                
                try (OutputStream os = conn.getOutputStream();
                     FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    gui.appendSystemMessage("File uploaded successfully: " + file.getName());
                    sendMessage("SYS: " + username + " shared a file: " + file.getName());
                } else {
                    gui.appendSystemMessage("File upload failed.");
                }
            } catch (Exception e) {
                gui.appendSystemMessage("Error uploading file: " + e.getMessage());
            }
        }).start();
    }
    
    // [EN] Download a file using HTTP GET request
    // [ZH] 使用 HTTP GET 请求下载文件
    public void downloadFile(String filename, File destDir) {
        new Thread(() -> {
            try {
                URL url = new URL("http://" + serverIp + ":28000/download?file=" + filename);
                HttpURLConnection conn;
                if (proxy != null) {
                    conn = (HttpURLConnection) url.openConnection(proxy);
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    File outFile = new File(destDir, filename);
                    try (InputStream is = conn.getInputStream();
                         FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[4096];
                        int count;
                        while ((count = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, count);
                        }
                    }
                    gui.appendSystemMessage("File downloaded successfully to: " + outFile.getAbsolutePath());
                } else {
                    gui.appendSystemMessage("File download failed. File might not exist.");
                }
            } catch (Exception e) {
                gui.appendSystemMessage("Error downloading file: " + e.getMessage());
            }
        }).start();
    }
}

package client;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.cert.X509Certificate;

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
        // Listen for UDP discovery in background
        new Thread(this::listenForUdpDiscovery).start();
    }

    public void setGui(ChatGUI gui) {
        this.gui = gui;
    }
    
    public void setServerIp(String ip) {
        this.serverIp = ip;
    }

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

    // Listens for UDP broadcasts to automatically find the server IP
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

    // Bypass SSL validation for self-signed certificates
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

    private boolean sendAuthRequest(String cmd, String user, String pass) {
        resolveHostnameAndIPv6();
        try {
            SSLSocketFactory factory = getTrustAllSocketFactory();
            Socket underlyingSocket;
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
            
            // Join message
            chatOut.println("JOIN " + username);
            
            // Start listening thread
            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            gui.appendSystemMessage("Failed to connect to chat server.");
        }
    }

    private void listenForMessages() {
        try {
            String msg;
            while ((msg = chatIn.readLine()) != null) {
                if (msg.startsWith("ONLINE_USERS ")) {
                    String[] users = msg.substring(13).split(",");
                    gui.updateOnlineUsers(users);
                } else {
                    gui.appendMessage(msg);
                }
            }
        } catch (IOException e) {
            gui.appendSystemMessage("Disconnected from server.");
        }
    }

    public void sendMessage(String msg) {
        if (chatOut != null) {
            chatOut.println(msg);
        }
    }

    public String getUsername() {
        return username;
    }
    
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

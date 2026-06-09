package server;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthServer implements Runnable {
    private final int port;
    private final Database db;
    private final ExecutorService threadPool;

    public AuthServer(int port, Database db) {
        this.port = port;
        this.db = db;
        this.threadPool = Executors.newCachedThreadPool();
        
        // Set keystore for SSL Server
        System.setProperty("javax.net.ssl.keyStore", "server_keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
    }

    @Override
    public void run() {
        try {
            SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(port);
            
            ServerLog.info("SSL Authentication Server started on port " + port);

            while (true) {
                Socket client = serverSocket.accept();
                threadPool.submit(new AuthHandler(client));
            }
        } catch (IOException e) {
            ServerLog.error("AuthServer error: " + e.getMessage());
        }
    }

    private class AuthHandler implements Runnable {
        private final Socket socket;

        public AuthHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String request = in.readLine();
                if (request != null) {
                    String[] parts = request.split(" ");
                    if (parts.length == 3) {
                        String cmd = parts[0];
                        String user = parts[1];
                        String pass = parts[2];

                        if ("REGISTER".equalsIgnoreCase(cmd)) {
                            boolean success = db.register(user, pass);
                            out.println(success ? "SUCCESS" : "FAIL_EXISTS");
                        } else if ("LOGIN".equalsIgnoreCase(cmd)) {
                            boolean success = db.authenticate(user, pass);
                            out.println(success ? "SUCCESS" : "FAIL_AUTH");
                        } else {
                            out.println("ERROR_UNKNOWN_CMD");
                        }
                    } else {
                        out.println("ERROR_FORMAT");
                    }
                }
            } catch (IOException e) {
                ServerLog.warning("Auth connection error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignored
                }
            }
        }
    }
}

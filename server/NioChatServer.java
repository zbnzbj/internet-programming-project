package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class NioChatServer implements Runnable {
    private final int port;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    
    // Maps SocketChannel to Username
    private final Map<SocketChannel, String> clients = new HashMap<>();
    private final Set<String> bannedUsers = new HashSet<>();

    public Collection<String> getOnlineUsers() {
        return new ArrayList<>(clients.values());
    }

    public NioChatServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            ServerLog.info("NIO Chat Server started on port " + port);

            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                    iter.remove();
                }
            }
        } catch (IOException e) {
            ServerLog.error("NIO Server error: " + e.getMessage());
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        ServerLog.info("New client connected: " + clientChannel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        try {
            int bytesRead = clientChannel.read(buffer);
            if (bytesRead == -1) {
                disconnect(clientChannel);
                return;
            }

            buffer.flip();
            String message = new String(buffer.array(), 0, buffer.limit()).trim();
            processMessage(clientChannel, message);
        } catch (IOException e) {
            disconnect(clientChannel);
        }
    }

    private void processMessage(SocketChannel clientChannel, String msg) throws IOException {
        if (msg.startsWith("JOIN ")) {
            String username = msg.substring(5).trim();
            if (bannedUsers.contains(username)) {
                sendMsg(clientChannel, "SYS: You are BANNED from this server.");
                clientChannel.close();
                return;
            }
            clients.put(clientChannel, username);
            broadcast("SYS: " + username + " has joined the chat.", null);
            sendOnlineUsers();
            return;
        }

        String sender = clients.getOrDefault(clientChannel, "Unknown");

        if (msg.startsWith("/kick ")) { // Basic Admin command handled here
            if (!sender.equalsIgnoreCase("admin")) {
                sendMsg(clientChannel, "SYS: Only admin can use this command.");
                return;
            }
            String target = msg.substring(6).trim();
            kickUser(target);
            return;
        }

        if (msg.startsWith("/ban ")) { // Ban Admin command
            if (!sender.equalsIgnoreCase("admin")) {
                sendMsg(clientChannel, "SYS: Only admin can use this command.");
                return;
            }
            String target = msg.substring(5).trim();
            bannedUsers.add(target);
            kickUser(target); // Kick them out immediately
            broadcast("SYS: " + target + " has been permanently BANNED by admin.", null);
            return;
        }

        if (msg.startsWith("/msg ")) {
            // Private message format: /msg targetUser Hello!
            String[] parts = msg.split(" ", 3);
            if (parts.length == 3) {
                String target = parts[1];
                String content = parts[2];
                sendPrivateMsg(sender, target, content);
            }
            return;
        }

        // Default: broadcast
        broadcast(sender + ": " + msg, null);
    }

    private void broadcast(String msg, SocketChannel excludeClient) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap((msg + "\n").getBytes());
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                SocketChannel ch = (SocketChannel) key.channel();
                if (ch != excludeClient) {
                    buffer.rewind();
                    ch.write(buffer);
                }
            }
        }
    }

    private void sendMsg(SocketChannel ch, String msg) throws IOException {
        ch.write(ByteBuffer.wrap((msg + "\n").getBytes()));
    }

    private void sendPrivateMsg(String sender, String targetUser, String msg) throws IOException {
        for (Map.Entry<SocketChannel, String> entry : clients.entrySet()) {
            if (entry.getValue().equals(targetUser)) {
                sendMsg(entry.getKey(), "[Private from " + sender + "]: " + msg);
                return;
            }
        }
    }

    private void sendOnlineUsers() throws IOException {
        StringBuilder usersMsg = new StringBuilder("ONLINE_USERS ");
        for (String user : clients.values()) {
            usersMsg.append(user).append(",");
        }
        broadcast(usersMsg.toString(), null);
    }

    public void kickUser(String targetUser) {
        try {
            for (Map.Entry<SocketChannel, String> entry : clients.entrySet()) {
                if (entry.getValue().equals(targetUser)) {
                    SocketChannel ch = entry.getKey();
                    sendMsg(ch, "SYS: You have been kicked by admin.");
                    ch.close();
                    clients.remove(ch);
                    broadcast("SYS: " + targetUser + " has been kicked.", null);
                    sendOnlineUsers();
                    ServerLog.info("Admin kicked user: " + targetUser);
                    break;
                }
            }
        } catch (IOException e) {
            ServerLog.error("Error kicking user: " + e.getMessage());
        }
    }

    private void disconnect(SocketChannel ch) {
        try {
            String user = clients.remove(ch);
            ch.close();
            if (user != null) {
                ServerLog.info("User disconnected: " + user);
                broadcast("SYS: " + user + " has left.", null);
                sendOnlineUsers();
            }
        } catch (IOException e) {
            ServerLog.error("Error closing channel: " + e.getMessage());
        }
    }
}

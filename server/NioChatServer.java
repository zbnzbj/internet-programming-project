package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

/**
 * [EN] High-Concurrency Message Distribution Server (NIO Chat Server)
 * Core technology: Java NIO (Non-blocking I/O), including Selector and SocketChannel.
 * Features:
 * 1. Uses a single-thread Selector to poll events, greatly improving concurrency.
 * 2. Handles group chat broadcasting and private message routing.
 * 3. Processes administrator commands (/kick, /ban).
 *
 * [ZH] 高并发聊天分发核心服务器
 * 核心技术：Java NIO (Non-blocking I/O)，包括 Selector 和 SocketChannel。
 * 功能：
 * 1. 使用单线程 Selector 轮询事件，极大地提高了并发处理能力（区别于传统 BIO 的一线程一连接）。
 * 2. 负责处理群聊广播、私聊精准投递。
 * 3. 处理管理员特权指令（/kick 踢人、/ban 封禁）。
 */
public class NioChatServer implements Runnable {
    private final int port;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    
    // [EN] Global dictionary mapping underlying SocketChannels to Usernames
    // [ZH] 维护客户端底层连接 (SocketChannel) 到其对应用户名 (Username) 的全局映射字典
    private final Map<SocketChannel, String> clients = new HashMap<>();
    
    // [EN] Set to track permanently banned usernames
    // [ZH] 记录被永久封禁的用户名的黑名单集合
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
            // [EN] Open and initialize the Selector and ServerSocketChannel
            // [ZH] 打开并初始化 NIO 核心组件 Selector 和 ServerSocketChannel
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false); // [EN] Non-blocking mode / [ZH] 配置为非阻塞模式
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            ServerLog.info("NIO Chat Server started on port " + port);

            // [EN] Main event loop for polling channel events
            // [ZH] 主事件轮询循环，不断检查是否有新的通道事件发生
            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    // [EN] Check isValid() to prevent CancelledKeyException on kicked/disconnected clients
                    // [ZH] 检查 key 是否仍有效，防止对已被踢出/断开的客户端操作时抛出 CancelledKeyException
                    if (key.isValid() && key.isReadable()) {
                        handleRead(key);
                    }
                    iter.remove();
                }
            }
        } catch (IOException e) {
            ServerLog.error("NIO Server error: " + e.getMessage());
        }
    }

    // [EN] Accept a new incoming client connection
    // [ZH] 接收一个新的客户端连接请求
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        ServerLog.info("New client connected: " + clientChannel.getRemoteAddress());
    }

    // [EN] Read data from a readable client channel
    // [ZH] 从可读的客户端通道中读取数据
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

    // [EN] Process and route incoming messages based on protocol commands
    // [ZH] 根据自定义协议和指令处理接收到的消息
    private void processMessage(SocketChannel clientChannel, String msg) throws IOException {
        // [EN] Handle client JOIN event
        // [ZH] 处理客户端加入聊天室的请求
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

        // [EN] Process basic admin command: /kick <username> to disconnect a user
        // [ZH] 处理基础管理员指令：/kick <用户名>，用于强制踢出某个在线用户
        if (msg.startsWith("/kick ")) {
            if (!sender.equalsIgnoreCase("admin")) {
                sendMsg(clientChannel, "SYS: Only admin can use this command.");
                return;
            }
            String target = msg.substring(6).trim();
            kickUser(target);
            return;
        }

        // [EN] Process ultimate admin command: /ban <username> to permanently block a user
        // [ZH] 处理最高级管理员指令：/ban <用户名>，永久封禁并立刻踢出该用户
        if (msg.startsWith("/ban ")) {
            if (!sender.equalsIgnoreCase("admin")) {
                sendMsg(clientChannel, "SYS: Only admin can use this command.");
                return;
            }
            String target = msg.substring(5).trim();
            bannedUsers.add(target);
            kickUser(target); // [EN] Kick them immediately / [ZH] 调用底层踢人逻辑，立刻强制切断该用户的 Socket 连接
            broadcast("SYS: " + target + " has been permanently BANNED by admin.", null);
            return;
        }

        // [EN] Process private message logic. Format: /msg <targetUser> <content>
        // [ZH] 处理私聊消息逻辑。解析标准私聊格式：/msg 目标用户名 聊天内容，例如：/msg Alice 你好呀！
        if (msg.startsWith("/msg ")) {
            String[] parts = msg.split(" ", 3);
            if (parts.length == 3) {
                String target = parts[1];
                String content = parts[2];
                sendPrivateMsg(sender, target, content);
            }
            return;
        }

        // [EN] Default behavior: broadcast the standard message to all online users
        // [ZH] 如果不是任何特殊指令，默认行为：将该普通消息广播给聊天室内所有的在线用户
        broadcast(sender + ": " + msg, null);
    }

    // [EN] Broadcast a message to all connected SocketChannels
    // [ZH] 向所有已连接的通道广播消息
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

    // [EN] Send a message to a specific SocketChannel
    // [ZH] 向特定的通道发送单播消息
    private void sendMsg(SocketChannel ch, String msg) throws IOException {
        ch.write(ByteBuffer.wrap((msg + "\n").getBytes()));
    }

    // [EN] Route a private message to a target user
    // [ZH] 路由分发一条私聊消息给目标用户
    private void sendPrivateMsg(String sender, String targetUser, String msg) throws IOException {
        // [EN] Find the sender's channel for sending confirmation
        // [ZH] 找到发送者的通道，用于发送确认副本
        SocketChannel senderChannel = null;
        for (Map.Entry<SocketChannel, String> entry : clients.entrySet()) {
            if (entry.getValue().equals(sender)) {
                senderChannel = entry.getKey();
                break;
            }
        }

        for (Map.Entry<SocketChannel, String> entry : clients.entrySet()) {
            if (entry.getValue().equals(targetUser)) {
                sendMsg(entry.getKey(), "[Private from " + sender + "]: " + msg);
                // [EN] Send confirmation to sender / [ZH] 向发送者发送确认副本
                if (senderChannel != null) {
                    sendMsg(senderChannel, "[Private to " + targetUser + "]: " + msg);
                }
                return;
            }
        }
        // [EN] Target user not found / [ZH] 目标用户不在线
        if (senderChannel != null) {
            sendMsg(senderChannel, "SYS: User '" + targetUser + "' is not online.");
        }
    }

    // [EN] Send the updated list of online users to everyone
    // [ZH] 向全服广播当前最新在线用户列表
    private void sendOnlineUsers() throws IOException {
        StringBuilder usersMsg = new StringBuilder("ONLINE_USERS ");
        for (String user : clients.values()) {
            usersMsg.append(user).append(",");
        }
        broadcast(usersMsg.toString(), null);
    }

    // [EN] Disconnect a user administratively
    // [ZH] 以管理员权限强制断开指定用户的连接
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

    // [EN] Handle a client disconnection event
    // [ZH] 处理客户端的主动断开连接事件
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

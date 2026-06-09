package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 * [EN] Client Graphical User Interface (GUI)
 * Core technology: Java Swing Framework
 * Features:
 * 1. Provides visual popups for login and registration, supporting SOCKS proxy and DNS resolution.
 * 2. Renders the main chat interface, including a global message area, online users list, and input box.
 * 3. Integrates HTTP-based file upload and download interaction buttons.
 *
 * [ZH] 客户端图形用户界面
 * 核心技术：Java Swing 框架
 * 功能：
 * 1. 提供登录与注册的可视化弹窗，支持高分附加项（输入 SOCKS 代理和主机名 DNS 解析）。
 * 2. 渲染主聊天界面，包含全局消息展示区、在线用户列表、私聊/群聊输入框。
 * 3. 集成基于 HTTP 的文件上传和下载交互按钮。
 */
public class ChatGUI {
    private final NetworkClient client;
    
    // [EN] Main Frame components / [ZH] 主窗口组件
    private JFrame frame;
    private JTextArea chatArea;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private JTextField inputField;

    public ChatGUI(NetworkClient client) {
        this.client = client;
        showLoginDialog();
    }

    // [EN] Displays the initial Login/Register dialog
    // [ZH] 显示初始的登录/注册对话框
    private void showLoginDialog() {
        JDialog dialog = new JDialog((Frame)null, "Chat Login", true);
        dialog.setLayout(new GridLayout(6, 2, 5, 5));
        dialog.setSize(350, 250);
        dialog.setLocationRelativeTo(null);

        JTextField serverIpField = new JTextField("127.0.0.1");
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextField proxyIpField = new JTextField();
        JTextField proxyPortField = new JTextField();

        dialog.add(new JLabel("Server Hostname/IP:")); dialog.add(serverIpField);
        dialog.add(new JLabel("Username:")); dialog.add(userField);
        dialog.add(new JLabel("Password:")); dialog.add(passField);
        dialog.add(new JLabel("SOCKS Proxy IP (Opt):")); dialog.add(proxyIpField);
        dialog.add(new JLabel("SOCKS Proxy Port (Opt):")); dialog.add(proxyPortField);

        JButton loginBtn = new JButton("Login");
        JButton regBtn = new JButton("Register");

        // [EN] Login button action / [ZH] 登录按钮动作处理
        loginBtn.addActionListener(e -> {
            applyNetworkSettings(serverIpField, proxyIpField, proxyPortField);
            String u = userField.getText();
            String p = new String(passField.getPassword());
            if (client.login(u, p)) {
                dialog.dispose();
                initMainChatGUI();
            } else {
                JOptionPane.showMessageDialog(dialog, "Login failed!");
            }
        });

        // [EN] Register button action / [ZH] 注册按钮动作处理
        regBtn.addActionListener(e -> {
            applyNetworkSettings(serverIpField, proxyIpField, proxyPortField);
            String u = userField.getText();
            String p = new String(passField.getPassword());
            if (client.register(u, p)) {
                JOptionPane.showMessageDialog(dialog, "Registered successfully! Please login.");
            } else {
                JOptionPane.showMessageDialog(dialog, "Registration failed. User may exist.");
            }
        });

        dialog.add(loginBtn);
        dialog.add(regBtn);
        
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        dialog.setVisible(true);
    }

    // [EN] Apply user-defined proxy and DNS settings to the NetworkClient
    // [ZH] 将用户自定义的代理和 DNS 设置应用到底层网络客户端
    private void applyNetworkSettings(JTextField serverIpField, JTextField proxyIpField, JTextField proxyPortField) {
        client.setServerIp(serverIpField.getText().trim());
        String pIp = proxyIpField.getText().trim();
        String pPort = proxyPortField.getText().trim();
        if (!pIp.isEmpty() && !pPort.isEmpty()) {
            try {
                client.setProxy(pIp, Integer.parseInt(pPort));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Invalid proxy port.");
            }
        }
    }

    // [EN] Initializes the main chat window after successful login
    // [ZH] 成功登录后初始化主聊天窗口界面
    private void initMainChatGUI() {
        frame = new JFrame("Chat Room - " + client.getUsername());
        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // [EN] Left Panel: Chat messages / [ZH] 左侧面板：聊天消息展示区
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // [EN] Right Panel: Online users list / [ZH] 右侧面板：在线用户列表区
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));
        userScrollPane.setBorder(BorderFactory.createTitledBorder("Online Users"));
        frame.add(userScrollPane, BorderLayout.EAST);

        // [EN] Bottom Panel: Input and File transfer controls / [ZH] 底部面板：消息输入与文件传输控制区
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.addActionListener(e -> sendInput());
        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> sendInput());
        
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton uploadBtn = new JButton("Upload File");
        JButton downloadBtn = new JButton("Download File");
        
        // [EN] Upload File Action / [ZH] 上传文件动作处理
        uploadBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                client.uploadFile(file);
            }
        });
        
        // [EN] Download File Action / [ZH] 下载文件动作处理
        downloadBtn.addActionListener(e -> {
            String filename = inputField.getText().trim();
            if (filename.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please type the exact filename in the input box first.");
                return;
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File dir = chooser.getSelectedFile();
                client.downloadFile(filename, dir);
            }
        });

        filePanel.add(uploadBtn);
        filePanel.add(downloadBtn);

        bottomPanel.add(filePanel, BorderLayout.NORTH);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);
        
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    // [EN] Sends the text input to the server
    // [ZH] 将输入框中的文本发送至服务端
    private void sendInput() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            client.sendMessage(msg);
            inputField.setText("");
        }
    }

    // [EN] Appends a standard chat message to the text area safely
    // [ZH] 安全地将标准聊天消息追加到文本展示区
    public void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    // [EN] Appends a system notification to the text area
    // [ZH] 将系统级通知消息追加到文本展示区
    public void appendSystemMessage(String msg) {
        appendMessage("[SYSTEM] " + msg);
    }

    // [EN] Updates the list of currently online users
    // [ZH] 刷新右侧当前的在线用户列表
    public void updateOnlineUsers(String[] users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String u : users) {
                if (!u.isEmpty()) userListModel.addElement(u);
            }
        });
    }
}

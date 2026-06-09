package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class ChatGUI {
    private final NetworkClient client;
    
    private JFrame mainFrame;
    private JTextArea chatArea;
    private JTextField inputField;
    private DefaultListModel<String> usersListModel;
    private JList<String> usersList;

    public ChatGUI(NetworkClient client) {
        this.client = client;
        showLoginScreen();
    }

    private void showLoginScreen() {
        JFrame loginFrame = new JFrame("Chat Login & Settings");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(400, 300);
        loginFrame.setLayout(new GridLayout(6, 2, 10, 10));
        loginFrame.setLocationRelativeTo(null);

        loginFrame.add(new JLabel("Server Hostname/IP:"));
        JTextField hostField = new JTextField("127.0.0.1");
        loginFrame.add(hostField);

        loginFrame.add(new JLabel("SOCKS Proxy IP (Optional):"));
        JTextField proxyIpField = new JTextField();
        loginFrame.add(proxyIpField);

        loginFrame.add(new JLabel("SOCKS Proxy Port:"));
        JTextField proxyPortField = new JTextField();
        loginFrame.add(proxyPortField);

        loginFrame.add(new JLabel("Username:"));
        JTextField userField = new JTextField();
        loginFrame.add(userField);

        loginFrame.add(new JLabel("Password:"));
        JPasswordField passField = new JPasswordField();
        loginFrame.add(passField);

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");

        loginFrame.add(loginBtn);
        loginFrame.add(registerBtn);

        // Helper to configure client before auth
        Runnable configClient = () -> {
            client.setServerIp(hostField.getText().trim());
            String pIp = proxyIpField.getText().trim();
            String pPort = proxyPortField.getText().trim();
            if (!pIp.isEmpty() && !pPort.isEmpty()) {
                try {
                    client.setProxy(pIp, Integer.parseInt(pPort));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(loginFrame, "Invalid Proxy Port", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                client.setProxy(null, 0);
            }
        };

        loginBtn.addActionListener(e -> {
            configClient.run();
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword()).trim();
            if (user.isEmpty() || pass.isEmpty()) return;
            
            if (client.login(user, pass)) {
                loginFrame.dispose();
                showMainChatScreen();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Login Failed", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        registerBtn.addActionListener(e -> {
            configClient.run();
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword()).trim();
            if (user.isEmpty() || pass.isEmpty()) return;
            
            if (client.register(user, pass)) {
                JOptionPane.showMessageDialog(loginFrame, "Registration Successful! Please login.");
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Registration Failed (User may exist)", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        loginFrame.setVisible(true);
    }

    private void showMainChatScreen() {
        mainFrame = new JFrame("Chat - " + client.getUsername());
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(600, 400);
        mainFrame.setLocationRelativeTo(null);

        // Chat Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // Online Users List
        usersListModel = new DefaultListModel<>();
        usersList = new JList<>(usersListModel);
        JScrollPane usersScrollPane = new JScrollPane(usersList);
        usersScrollPane.setPreferredSize(new Dimension(150, 0));

        // Bottom Input Panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendBtn = new JButton("Send");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        // Top Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton uploadBtn = new JButton("Upload File");
        JButton downloadBtn = new JButton("Download File");
        JLabel hintLabel = new JLabel("  Hint: /msg <user> <message> for private message");
        hintLabel.setForeground(Color.GRAY);
        toolbar.add(uploadBtn);
        toolbar.add(downloadBtn);
        toolbar.add(hintLabel);

        mainFrame.add(toolbar, BorderLayout.NORTH);
        mainFrame.add(scrollPane, BorderLayout.CENTER);
        mainFrame.add(usersScrollPane, BorderLayout.EAST);
        mainFrame.add(inputPanel, BorderLayout.SOUTH);

        // Event Listeners
        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        uploadBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                client.uploadFile(file);
            }
        });

        downloadBtn.addActionListener(e -> {
            String filename = JOptionPane.showInputDialog(mainFrame, "Enter filename to download:");
            if (filename != null && !filename.trim().isEmpty()) {
                JFileChooser dirChooser = new JFileChooser();
                dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (dirChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                    client.downloadFile(filename.trim(), dirChooser.getSelectedFile());
                }
            }
        });

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Wait briefly for network to close gracefully
                System.exit(0);
            }
        });

        mainFrame.setVisible(true);
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            client.sendMessage(msg);
            inputField.setText("");
        }
    }

    public void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (chatArea != null) {
                chatArea.append(msg + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
        });
    }

    public void appendSystemMessage(String msg) {
        appendMessage("[SYS] " + msg);
    }

    public void updateOnlineUsers(String[] users) {
        SwingUtilities.invokeLater(() -> {
            if (usersListModel != null) {
                usersListModel.clear();
                for (String user : users) {
                    if (user != null && !user.trim().isEmpty()) {
                        usersListModel.addElement(user);
                    }
                }
            }
        });
    }
}

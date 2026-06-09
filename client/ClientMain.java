package client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 客户端启动入口 (Client Main Entry)
 * 功能：
 * 初始化 Swing 图形界面组件和底层网络通信组件，并将它们进行互相绑定。
 */
public class ClientMain {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            NetworkClient client = new NetworkClient();
            ChatGUI gui = new ChatGUI(client);
            client.setGui(gui);
        });
    }
}

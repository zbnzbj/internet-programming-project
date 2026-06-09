package client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * [EN] Client Main Entry
 * Features: Initializes the Swing Graphical User Interface and the underlying
 * network communication components, binding them together.
 *
 * [ZH] 客户端启动入口
 * 功能：初始化 Swing 图形界面组件和底层网络通信组件，并将它们进行互相绑定。
 */
public class ClientMain {
    public static void main(String[] args) {
        try {
            // [EN] Set system native look and feel for better UI rendering
            // [ZH] 设置系统原生的外观风格，以获得更美观的 UI 渲染效果
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // [EN] Ensure GUI creation runs on the Event Dispatch Thread (EDT) for thread safety
        // [ZH] 确保 GUI 的创建运行在事件分发线程 (EDT) 上，保证线程安全
        SwingUtilities.invokeLater(() -> {
            NetworkClient networkClient = new NetworkClient();
            ChatGUI gui = new ChatGUI(networkClient);
            networkClient.setGui(gui);
        });
    }
}

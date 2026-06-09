package client;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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

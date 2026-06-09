package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * [EN] UDP Server Discovery Broadcaster
 * Core technology: DatagramSocket / UDP Broadcast
 * Features:
 * Periodically (e.g., every 2 seconds) sends UDP broadcast packets containing server info to port 28888 on the LAN.
 * This allows clients to automatically discover and connect to the server without manually typing an IP.
 *
 * [ZH] UDP 广播服务器自动发现组件
 * 核心技术：DatagramSocket / UDP Broadcast
 * 功能：
 * 周期性（如每2秒）向局域网内的 28888 端口发送包含服务端 IP 信息的 UDP 广播包。
 * 这使得客户端无需手动输入 IP，即可在局域网内自动寻找并连接到服务器。
 */
public class UdpBroadcaster implements Runnable {
    private final int broadcastPort;
    private final String message;

    public UdpBroadcaster(int broadcastPort, int chatPort) {
        this.broadcastPort = broadcastPort;
        this.message = "SERVER_DISCOVERY:" + chatPort;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            byte[] buffer = message.getBytes();
            ServerLog.info("UDP Broadcaster started on port " + broadcastPort);

            // [EN] Infinite loop to broadcast server existence periodically
            // [ZH] 无限循环，定期向局域网广播服务端的存活状态
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length,
                        InetAddress.getByName("255.255.255.255"), // [EN] Broadcast IP / [ZH] 全局广播地址
                        broadcastPort
                    );
                    socket.send(packet);
                    Thread.sleep(2000); // [EN] Broadcast every 2 seconds / [ZH] 每两秒广播一次
                } catch (Exception e) {
                    ServerLog.error("UDP Broadcast error: " + e.getMessage());
                    Thread.sleep(5000);
                }
            }
        } catch (Exception e) {
            ServerLog.error("Failed to start UDP Broadcaster: " + e.getMessage());
        }
    }
}

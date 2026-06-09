package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * UDP 广播服务器自动发现组件 (UDP Server Discovery)
 * 核心技术：DatagramSocket / UDP Broadcast
 * 功能：
 * 周期性（如每2秒）向局域网内的 28888 端口发送包含服务端 IP 信息的 UDP 广播包。
 * 这使得客户端无需手动输入 IP，即可在局域网内自动寻找并连接到服务器。
 */
public class UdpBroadcaster implements Runnable {
    private final int broadcastPort;
    private final String message;

    public UdpBroadcaster(int broadcastPort, int tcpPort) {
        this.broadcastPort = broadcastPort;
        this.message = "SERVER_DISCOVERY:" + tcpPort;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            ServerLog.info("UDP Broadcaster started, targeting port " + broadcastPort);
            
            while (true) {
                byte[] buffer = message.getBytes();
                // Broadcast to standard broadcast address
                InetAddress group = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, broadcastPort);
                
                socket.send(packet);
                Thread.sleep(5000); // Broadcast every 5 seconds
            }
        } catch (IOException | InterruptedException e) {
            ServerLog.error("UDP Broadcaster error: " + e.getMessage());
        }
    }
}

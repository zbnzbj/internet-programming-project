package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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

package UDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class clientUDP {
    public clientUDP() {
        try {
            DatagramSocket socket = new DatagramSocket(7000);
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String str = new String(packet.getData(), 0, packet.getLength());
                System.out.println(str);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        clientUDP client = new clientUDP();
    }
}

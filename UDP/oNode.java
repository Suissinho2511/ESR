package UDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class oNode {

    String neighborsIP[]; // neighbor ip

    public oNode(String ips[]) {
        neighborsIP = ips;

        try {
            DatagramSocket socket_in = new DatagramSocket(5000);
            DatagramSocket socket_out = new DatagramSocket(5001);

            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            int i = 0;
            while (true) {
                socket_in.receive(packet);
                String str = new String(packet.getData(), 0, packet.getLength());
                str = "+++ " + str + " +++";
                buffer = str.getBytes();
                for (String ip : neighborsIP) {
                    packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), 5000);
                    socket_out.send(packet);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        oNode node = new oNode(args); // client ip
    }
}

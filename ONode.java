import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ONode {

    String neighborsIP[]; // neighbor ip

    public ONode(String ips[]) {
        neighborsIP = ips;

        try {
            DatagramSocket socket_in = new DatagramSocket(5000);
            DatagramSocket socket_out = new DatagramSocket(5001);

            byte[] buffer = new byte[20000];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket_in.receive(packet);
                byte[] data = packet.getData();
                for (String ip : neighborsIP) {
                    DatagramPacket out_packet = new DatagramPacket(data, data.length, InetAddress.getByName(ip), 5000);
                    socket_out.send(out_packet);
                }
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        ONode node = new ONode(args);
    }
}

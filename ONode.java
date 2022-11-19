import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ONode {

    String neighborsIP[]; // neighbor ip

    public ONode(String ips[]) {
        neighborsIP = ips;

        try {
            DatagramSocket socket_data = new DatagramSocket(5000);
            DatagramSocket socket_control = new DatagramSocket(5001);
            

            byte[] buffer = new byte[20000];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket_data.receive(packet);
                byte[] data = packet.getData();
                for (String ip : neighborsIP) {
                    DatagramPacket out_packet = new DatagramPacket(data, data.length, InetAddress.getByName(ip), 5000);
                    socket_data.send(out_packet);
                }
            }

            while (true)
            {
                socket_control.receive(packet);
                
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        ONode node = new ONode(args);
    }
}

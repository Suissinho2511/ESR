import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class clientUDP {
    public clientUDP(String ip) {
        try {
            DatagramSocket socket = new DatagramSocket(6000);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), 5000);
            while (true) {
                // String str = "Hello server: " + i++;
                // buffer = str.getBytes();
                // socket.send(packet);
                socket.receive(packet);
                String str = new String(packet.getData(), 0, packet.getLength());
                System.out.println(str);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        clientUDP client = new clientUDP(args[0]);
    }
}

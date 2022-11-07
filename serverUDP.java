import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class serverUDP {
    public serverUDP(String ip) {
        try {
            DatagramSocket socket = new DatagramSocket(5000);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            int i = 0;
            while (true) {
                // socket.receive(packet);
                // String str = new String(packet.getData(), 0, packet.getLength());
                // System.out.println("Server: " + str);
                // Thread.sleep(1000);
                String str = "Hello client: " + i++;
                System.out.println(str);
                buffer = str.getBytes();
                packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), 6000);
                Thread.sleep(1000);
                socket.send(packet);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        serverUDP server = new serverUDP(args[0]);
    }
}

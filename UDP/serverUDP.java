package UDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class serverUDP {
    public serverUDP(String ip) {
        try {
            DatagramSocket socket = new DatagramSocket(6000);
            byte[] buffer = new byte[1024];
            int i = 0;

            while (true) {
                String str = "Hello client: " + i++;
                System.out.println(str);
                buffer = str.getBytes();

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), 5000);
                socket.send(packet);

                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        serverUDP server = new serverUDP(args[0]); // oNode ip
    }
}

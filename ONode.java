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



			// Data thread

            new Thread(() -> { try {

				while (true) {
					socket_data.receive(packet);
					byte[] data = packet.getData();
					for (String ip : neighborsIP) {
						DatagramPacket out_packet = new DatagramPacket(data, data.length, InetAddress.getByName(ip), 5000);
						socket_data.send(out_packet);
					}
				}

				} catch (Exception e) {
					System.out.println(e);
					socket_data.close();
					socket_control.close();
					System.exit(-1);
				}
            }).start();



			// Control thread

            new Thread(() -> {

				System.out.println("UWU");
				socket_control.close();

            }).start();



        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        new ONode(args);
    }
}

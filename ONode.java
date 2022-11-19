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

			// Data thread

            new Thread(() -> { try {

				byte[] data_buffer = new byte[20000];
				DatagramPacket data_packet = new DatagramPacket(data_buffer, data_buffer.length);

				while (true) {
					socket_data.receive(data_packet);
					byte[] data = data_packet.getData();
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

            new Thread(() -> { try {
			
				byte[] ctrl_buffer = new byte[20000];
				DatagramPacket ctrl_packet = new DatagramPacket(ctrl_buffer, ctrl_buffer.length);

				while (true) {
					socket_control.receive(ctrl_packet);
					byte[] data = ctrl_packet.getData();
					for (String ip : neighborsIP) {
						DatagramPacket out_packet = new DatagramPacket(data, data.length, InetAddress.getByName(ip), 5001);
						socket_control.send(out_packet);
					}
				}

				} catch (Exception e) {
					System.out.println(e);
					socket_data.close();
					socket_control.close();
					System.exit(-1);
				}
            }).start();


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
        }
    }

    public static void main(String[] args) {
        new ONode(args);
    }
}

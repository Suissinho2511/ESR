
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ONode {

    private List<InetAddress> neighboursIP;
	private Map<InetAddress,Socket> connections;
	private DatagramSocket socket_data;
	private ServerSocket socket_control;


    public ONode(String ips[]) {

		//this.neighboursIP = new ArrayList<>();
		//this.neighboursIP.addAll(Arrays.asList(ips));

		// String to INetAddress
		//this.neighboursIP = new ArrayList<>();
		this.neighboursIP = Arrays.asList(ips).stream().map((name) -> {
			try {
				return InetAddress.getByName(name);
			} catch (UnknownHostException e1) {
				return null;
			}
		}).collect(Collectors.toList());

		System.out.println("[DEBUG] Neighbours:\n"+this.neighboursIP.toString());



        try {

			// Listen for UDP traffic on port 5000 - Data
			this.socket_data = new DatagramSocket(5000);
			System.out.println("[INFO] Data socket created");
			
			// Listen for TCP connections on port 5001 - Control
			this.socket_control = new ServerSocket(5001);
			System.out.println("[INFO] Control server socket created");

			// Connect to neighbours
			this.connections = new HashMap<InetAddress,Socket>();
			for (InetAddress ip : this.neighboursIP) {
				try {
					Socket s = new Socket(ip, 5001);
					this.connections.put(ip, s);
					System.out.println("[INFO] Connected to neighbour "+ip.toString());
				} catch (Exception offline) {System.out.println("[WARNING] Neighbour "+ip.toString()+" offline! (Ignore if it's an endpoint)");}
			}



			// Control thread for server socket
			newControlThread().start();
			System.out.println("[INFO] Control thread started");
			

			// Data thread
            newDataThread().start();
			System.out.println("[INFO] Data thread started");


			// Control thread for each neighbour
			for (Socket s : this.connections.values())
				newNeighbourThread(s).start();
			System.out.println("[INFO] Neighbour threads started");




        } catch (Exception e) {
            System.out.println(e);
			System.exit(-1);
        }
    }

	



	private Thread newNeighbourThread(Socket s) {
		return new Thread(() -> { try {

			//final Socket thread_socket = s;
		
			// Create buffer for control packets
			byte[] ctrl_buffer = new byte[20000];
			DatagramPacket ctrl_packet = new DatagramPacket(ctrl_buffer, ctrl_buffer.length);


			// Send initial probe to neighbour
			//OutputStream out = s.getOutputStream();
			//out.write("Hello!".getBytes());
			//out.flush();
			//out.close();
			System.out.println("[DEBUG] Probed "+s.getInetAddress().toString());


			// Listen for responses and process
			while (true) {

				//InputStream in = s.getInputStream();
				//String str = new String(in.readAllBytes());
				//System.out.println("[DEBUG] Received control message from "+s.getInetAddress().toString()+":\n" + str);
				//in.close();

			}

			} catch (Exception e) {
				System.out.println("[ERROR] Control thread for neighbour "+s.getInetAddress().toString()+" crashed!");
				System.out.println(e);
				System.exit(-1);
			}
		});
	}



	private Thread newControlThread() {
		return new Thread(() -> { try {

			while(true) {
				
				Socket new_socket = socket_control.accept();
				InetAddress ip = new_socket.getInetAddress();

				// New neighbour:
				if (!this.neighboursIP.contains(ip)) {
					System.out.println("[INFO] New neighbour: "+ip.toString());
					this.neighboursIP.add(ip);
					this.connections.put(ip,new_socket);
					System.out.println("[DEBUG] Neighbours:\n"+neighboursIP.toString());
					newNeighbourThread(new_socket).start();;
				}
			}
			

			} catch (Exception e) {
				System.out.println("[ERROR] Control thread crashed!");
				System.out.println(e);
				System.exit(-1);
			}
		});
	}



	private Thread newDataThread() {
		return new Thread(() -> { try {

			// Create buffer for data packets
			byte[] data_buffer = new byte[20000];
			DatagramPacket data_packet = new DatagramPacket(data_buffer, data_buffer.length);

			while (true) {

				// Receive data packet
				socket_data.receive(data_packet);

				// Ignore data packets from unknown sources
				InetAddress incoming_ip = data_packet.getAddress();
				if(!this.neighboursIP.contains(incoming_ip)) continue;
				
				byte[] data = data_packet.getData();
				//System.out.println("[DEBUG] Received data from "+incoming_ip);

				// Flood neighbours
				for (InetAddress ip : this.neighboursIP) {

					// (except the one who sent packet)
					if(ip.equals(incoming_ip)) continue;

					DatagramPacket out_packet = new DatagramPacket(data, data.length, ip, 5000);
					socket_data.send(out_packet);
					//System.out.println("[DEBUG] Sent data to "+ip);
				}
			}

			} catch (Exception e) {
				System.out.println("[ERROR] Data thread crashed!");
				System.out.println(e);
				System.exit(-1);
			}
		});
	}





    public static void main(String[] args) {
        new ONode(args);
    }
}
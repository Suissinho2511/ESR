
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ONode {

    private List<InetAddress> neighboursIP;
	private final ReadWriteLock neighbourIP_lock = new ReentrantReadWriteLock();
	private DatagramSocket socket_data;
	private ServerSocket socket_control;


    public ONode(String ips[]) {

		// String to INetAddress
		this.neighboursIP = Arrays.asList(ips).stream().map((name) -> {
			try {
				return InetAddress.getByName(name);
			} catch (UnknownHostException e1) {
				return null;
			}
		}).collect(Collectors.toList());

		System.out.println("[INFO] Neighbours:\n"+this.neighboursIP.toString());



        try {

			// Listen for UDP traffic on port 5000 - Data
			this.socket_data = new DatagramSocket(5000);
			System.out.println("[INFO] Data socket created");
			
			// Listen for TCP connections on port 5001 - Control
			this.socket_control = new ServerSocket(5001);
			System.out.println("[INFO] Control server socket created");

			// Connect to neighbours
			for (InetAddress ip : this.neighboursIP) {
				try {
					sendPing(ip, "Hello!");
					System.out.println("[INFO] Connected to neighbour "+ip.toString());
				} catch (Exception offline) {System.out.println("[WARNING] Neighbour "+ip.toString()+" offline! (May be an endpoint)");}
			}



			// Control thread for server socket
			newControlThread().start();
			System.out.println("[INFO] Control thread started");
			

			// Data thread
            newDataThread().start();
			System.out.println("[INFO] Data thread started");




        } catch (Exception e) {
            System.out.println(e);
			System.exit(-1);
        }
    }

	



	private Thread newNeighbourThread(Socket s) {
		return new Thread(() -> { try {
		
			// Create buffer for control packet
			byte[] ctrl_buffer = new byte[20000];
			DatagramPacket ctrl_packet = new DatagramPacket(ctrl_buffer, ctrl_buffer.length);


			// Process
			DataInputStream in = new DataInputStream(s.getInputStream());
			CABPacket cabp = new CABPacket();
			cabp.read(in);


			switch (cabp.type) {

				case 0:
					// Hello
					CABHelloPacket cabp_h = new CABHelloPacket(in);
					String str = cabp_h.getMessage();
					System.out.println("[DEBUG] Received ping message from "+s.getInetAddress().toString()+":\n" + str);
					break;
				
				case 1:
					// Probe path
					CABControlPacket cabp_c = new CABControlPacket(in);
					// TODO: Verificar availableJumps e assim
					cabp_c.addNode(s.getLocalAddress());
					// TODO: Spread packet
					System.out.println("[DEBUG] Received probe path request from "+s.getInetAddress().toString()+". Spreading...");
					break;
				
				case 2:
					// Reply path
					cabp_c = new CABControlPacket(in);
					// TODO: retornar à origem
					System.out.println("[DEBUG] Received path from "+s.getInetAddress().toString()+". Returning to origin...");
					break;

				default:
					System.out.println("[DEBUG] Received unknown message from "+s.getInetAddress().toString());
					break;
			}


			in.close();



			} catch (Exception e) {
				System.out.println("[ERROR] Control thread for neighbour "+s.getInetAddress().toString()+" crashed!");
				System.out.println(e);

				this.neighbourIP_lock.writeLock().lock();
				this.neighboursIP.remove(s.getInetAddress());
				this.neighbourIP_lock.writeLock().unlock();
				//System.exit(-1);
			}
		});
	}



	private Thread newControlThread() {
		return new Thread(() -> { try {

			while(true) {
				
				Socket new_socket = socket_control.accept();
				InetAddress ip = new_socket.getInetAddress();

				// New neighbour:
				if (!isNeighbour(ip)) {
					System.out.println("[INFO] New neighbour: "+ip.toString());
					addNeighbour(ip);
					System.out.println("[DEBUG] Neighbours:\n"+neighboursIP.toString());
					sendPing(ip, "Hello!");
				}

				// Process
				newNeighbourThread(new_socket).start();
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
				if(!isNeighbour(incoming_ip)) continue;
				
				byte[] data = data_packet.getData();
				//System.out.println("[DEBUG] Received data from "+incoming_ip);

				// Flood neighbours
				this.neighbourIP_lock.readLock().lock();
				for (InetAddress ip : this.neighboursIP) {

					// (except the one who sent packet)
					if(ip.equals(incoming_ip)) continue;

					DatagramPacket out_packet = new DatagramPacket(data, data.length, ip, 5000);
					socket_data.send(out_packet);
					//System.out.println("[DEBUG] Sent data to "+ip);
				}
				this.neighbourIP_lock.readLock().unlock();
			}

			} catch (Exception e) {
				System.out.println("[ERROR] Data thread crashed!");
				System.out.println(e);
				System.exit(-1);
			}
		});
	}






	private void sendPing(InetAddress ip, String message) throws IOException {
		Socket s = new Socket(ip, 5001);
		DataOutputStream out = new DataOutputStream(s.getOutputStream());
		new CABHelloPacket(message).write(out);
		out.close();
		s.close();
	}

	private void addNeighbour(InetAddress ip) {
		this.neighbourIP_lock.writeLock().lock();
		this.neighboursIP.add(ip);
		this.neighbourIP_lock.writeLock().unlock();
	}

	private boolean isNeighbour(InetAddress ip) {
		this.neighbourIP_lock.readLock().lock();
		boolean result = this.neighboursIP.contains(ip);
		this.neighbourIP_lock.readLock().unlock();
		return result;
	}





    public static void main(String[] args) {
        new ONode(args);
    }
}
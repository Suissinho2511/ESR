
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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










class Graph {

	//TODO: Adaptar para incluir delays e ter isso em consideração no pathfinding

	private class Vertex {
		public final String label;
		//public List<InetAddress> interfaces;
		//public List<Edge> interfaces;
		//public List<Vertex> adjVertices;

		public Vertex(String label) {this.label = label;}
	}

	private class Edge {
		public final Vertex from;
		public final Vertex to;

		public final InetAddress ifrom;
		public final InetAddress ito;

		public Float cost;

		public Edge(Vertex from, Vertex to, InetAddress ifrom, InetAddress ito) {this.from = from; this.to = to; this.ifrom = ifrom; this.ito = ito;}
		public Edge(Vertex from, Vertex to, InetAddress ifrom, InetAddress ito, Float cost) {this(from,to,ifrom,ito); this.cost = cost;}
	}

	private final Map<String, List<String>> adjVertices = new HashMap<>();

	public void addVertex(String label) {
		adjVertices.putIfAbsent(label, new ArrayList<>());
	}

	public void removeVertex(String label) {
		adjVertices.values().forEach(e -> e.remove(label));
		adjVertices.remove(label);
	}

	public void addEdge(String label1, String label2) {
		if(!adjVertices.containsKey(label1)) addVertex(label1);
		if(!adjVertices.containsKey(label2)) addVertex(label2);
		adjVertices.get(label1).add(label2);
	}

	public void removeEdge(String label1, String label2) {
		List<String> eV1 = adjVertices.get(label1);
		List<String> eV2 = adjVertices.get(label2);
		if (eV1 != null)
			eV1.remove(label2);
		if (eV2 != null)
			eV2.remove(label1);
	}

	public List<String> breadthFirst(String origem, String destino) {
		Set<String> visited = new LinkedHashSet<>();
		Queue<List<String>> queue = new LinkedList<>();

		List<String> first_node = new ArrayList<>();
		first_node.add(origem);

		queue.add(first_node);
		visited.add(origem);

		while (!queue.isEmpty()) {
			List<String> path = queue.poll();
			String vertex = path.get(path.size()-1);

			if(vertex.equals(destino)) return path;

			for (String v : this.getAdjVertices(vertex)) {
				if (!visited.contains(v)) {
					List<String> new_path = new ArrayList<>(path);
					new_path.add(v);

					queue.add(new_path);
					visited.add(v);
				}
			}
		}
		//return visited.stream().toList();
		return new ArrayList<>(visited);
	}

	public List<String> getAdjVertices(String label) {
		return adjVertices.getOrDefault(label, null);
	}

	public Set<String> getNodes() {
		return adjVertices.keySet();
	}

	public boolean equals(Graph other) {

		for (String node : other.getNodes())
			if(!this.getNodes().contains(node)) return false;
			
		for (String node : this.getNodes())
			if(!other.getNodes().contains(node)) return false;

		for (String from : other.getNodes())
			for (String to : other.getAdjVertices(from))
				if(!this.getAdjVertices(from).contains(to)) return false;
				
		for (String from : this.getNodes())
			for (String to : this.getAdjVertices(from))
				if(!other.getAdjVertices(from).contains(to)) return false;

		return true;
	}

	public boolean merge(Graph other) {
		boolean result = false; //were there changes

		// check nodes first
		for(String other_node : other.getNodes())
			if(!this.getNodes().contains(other_node)) {
				this.addVertex(other_node);
				result = true;
			}

		// now check vertices
		for(String other_node : other.getNodes())
			for(String adj : other.getAdjVertices(other_node))
				if(!this.getAdjVertices(other_node).contains(adj)) {
					this.addEdge(other_node, adj);
					result = true;
				}

		return result;
	}


	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();

		for (String from : this.getNodes())
			for (String to : this.getAdjVertices(from))
				str.append(from+'-'+to+'\n');

		return str.toString();
	}
}
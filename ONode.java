import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ONode {

    private List<String> neighboursIP;
	private Map<String,Socket> connections;
	private Graph topology;
	private final String self_ip;
	private DatagramSocket socket_data;
	private ServerSocket socket_control;


    public ONode(String ips[]) {

		this.neighboursIP = new ArrayList<>();
		this.neighboursIP.addAll(Arrays.asList(ips));

		// Start building overlay topology
		this.topology = new Graph();	
		this.self_ip = getPublicIpAddress();
		for (String ip : this.neighboursIP) {
			this.topology.addVertex(ip);
			this.topology.addEdge(self_ip, ip);
		}
		System.out.println("[DEBUG] Topology:\n"+topology.toString());



        try {

			// Listen for UDP traffic on port 5000 - Data
			this.socket_data = new DatagramSocket(5000);
			System.out.println("[INFO] Data socket created");
			
			// Listen for TCP connections on port 5001 - Control
			this.socket_control = new ServerSocket(5001);
			System.out.println("[INFO] Control server socket created");

			// Connect to neighbours
			this.connections = new HashMap<String,Socket>();
			for (String ip : this.neighboursIP) {
				try {
					Socket s = new Socket(ip, 5001);
					this.connections.put(self_ip, s);
					System.out.println("[INFO] Connected to neighbour "+ip);
				} catch (Exception offline) {System.out.println("[WARNING] Neighbour "+ip+" offline!");}
			}

			// Send initial probe to all neighbours
			for (Socket s : this.connections.values()) {
				this.topology.writeToSocket(s);
				System.out.println("[DEBUG] Probed "+s.getInetAddress().toString());
			}


			// Control thread for each neighbour
			for (Socket s : this.connections.values())
				newNeighbourThread(s).start();


			// Control thread for server socket
			newControlThread().start();


			// Data thread
            newDataThread().start();


        } catch (Exception e) {
            System.out.println(e);
			System.exit(-1);
        }
    }

	



	private Thread newNeighbourThread(Socket s) {
		return new Thread(() -> { try {

			final Socket thread_socket = s;
		
			// Create buffer for control packets
			byte[] ctrl_buffer = new byte[20000];
			DatagramPacket ctrl_packet = new DatagramPacket(ctrl_buffer, ctrl_buffer.length);

			// Listen for responses and process
			while (true) {

				// Read graph
				/*Graph other = Graph.readFromSocket(thread_socket);
				System.out.println("[DEBUG] Received topology from "+s.getInetAddress().toString());

				// If equal, do nothing
				if(this.topology.equals(other)) continue;

				// Merge with our current topology and check for changes
				boolean changed = this.topology.merge(other);
				System.out.println("[DEBUG] Topology from "+s.getInetAddress().toString()+" merged, with"+(changed?"":"out")+" changes");
				

				if(!changed) {
					
					// If not changed, reply with our own topology
					this.topology.writeToSocket(thread_socket);
					System.out.println("[DEBUG] Sent new topology from "+s.getInetAddress().toString()+" to "+thread_socket.getInetAddress().toString());

				} else {

					// Spread new topology if changed
					for (Socket out : this.connections.values()) {
						this.topology.writeToSocket(out);
						System.out.println("[DEBUG] Sent new topology from "+s.getInetAddress().toString()+" to "+out.getInetAddress().toString());
					}

				}*/

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
				String ip = new_socket.getInetAddress().toString();

				// New neighbour:
				if (!this.neighboursIP.contains(ip)) {
					System.out.println("[INFO] New neighbour: "+ip);
					this.neighboursIP.add(ip);
					this.connections.put(ip,new_socket);
					this.topology.addVertex(ip);
					this.topology.addEdge(self_ip, ip);
					System.out.println("[DEBUG] Topology:\n"+topology.toString());
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
				byte[] data = data_packet.getData();

				// Add neighbour if not already a neighbour (TODO: Método para isto, ou então ignorar pacotes de hosts desconhecidos)
				String incoming_ip = data_packet.getAddress().toString();
				//if(this.neighboursIP.contains(incoming_ip)) this.neighboursIP.add(incoming_ip);
				//if(this.neighboursIP.contains(incoming_ip)) continue;
				
				//System.out.println("[DEBUG] Received data from "+incoming_ip);

				// Flood neighbours
				for (String ip : this.neighboursIP) {

					// (except the one who sent packet)
					if(ip.equals(incoming_ip)) continue;

					DatagramPacket out_packet = new DatagramPacket(data, data.length, InetAddress.getByName(ip), 5000);
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



	// Thanks, stackoverflow
	private String getPublicIpAddress() {
		String res = null;
		try {
			String localhost = InetAddress.getLocalHost().getHostAddress();
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements()) {
				NetworkInterface ni = (NetworkInterface) e.nextElement();
				if(ni.isLoopback())
					continue;
				if(ni.isPointToPoint())
					continue;
				Enumeration<InetAddress> addresses = ni.getInetAddresses();
				while(addresses.hasMoreElements()) {
					InetAddress address = (InetAddress) addresses.nextElement();
					if(address instanceof Inet4Address) {
						String ip = address.getHostAddress();
						if(!ip.equals(localhost))
							System.out.println((res = ip));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}





    public static void main(String[] args) {
        new ONode(args);
    }
}




class Graph implements Serializable {

	//TODO: Adaptar para incluir delays e ter isso em consideração no pathfinding

	private class Vertex {
		public final String label;
		public Vertex(String label) {this.label = label;}
	}

	private class Edge {
		public final Vertex from;
		public final Vertex to;
		public Float cost;
		public Edge(Vertex from, Vertex to) {this.from = from; this.to = to;}
		public Edge(Vertex from, Vertex to, Float cost) {this.from = from; this.to = to; this.cost = cost;}
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

	public static Graph readFromSocket(Socket s) throws IOException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(s.getInputStream());
		Graph g = (Graph) in.readObject();
		in.close();
		return g;
	}

	public void writeToSocket(Socket s) throws IOException {
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        out.writeObject(this);
        out.close();
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
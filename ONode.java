import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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

public class ONode {

    private List<String> neighborsIP;
	private Map<String,Socket> connections; // Sockets to neighbors
	private Graph topology;


    public ONode(String ips[]) {

        this.neighborsIP = Arrays.asList(ips);

		// Start building overlay topology
		final String self_ip;
		try {self_ip = InetAddress.getLocalHost().toString(); this.topology.addVertex(self_ip);} catch (UnknownHostException ignore) {self_ip = "self";}
		for (String ip : this.neighborsIP) {
			this.topology.addVertex(ip);
			this.topology.addEdge(self_ip, ip);
		}



        try {

			// Listen for UDP traffic on port 5000 - Data
			DatagramSocket socket_data = new DatagramSocket(5000);
			
			// Listen for TCP connections on port 5001 - Control
			ServerSocket socket_control = new ServerSocket(5001);

			// Connect to neighbors
			this.connections = new HashMap<String,Socket>(this.neighborsIP.size());
			for (String ip : this.neighborsIP) {
				Socket s = new Socket(ip, 5001);
				this.connections.put(self_ip, s);
			}

			// Send initial probe to all neighbors
			for (Socket s : this.connections.values())
				this.topology.writeToSocket(s);



			// Control thread for each neighbor (TODO: Método para isto)

			for (Socket s : this.connections.values())
				new Thread(() -> { try {

					final Socket thread_socket = s;
				
					// Create buffer for control packets
					byte[] ctrl_buffer = new byte[20000];
					DatagramPacket ctrl_packet = new DatagramPacket(ctrl_buffer, ctrl_buffer.length);

					// Listen for responses and process
					while (true) {

						// Read graph
						Graph other = Graph.readFromSocket(thread_socket);

						// Merge with our current topology and check for changes
						boolean changed = this.topology.merge(other);
						if(!changed) continue;

						// Spread new topology if changed
						for (Socket out : this.connections.values())
							this.topology.writeToSocket(out);

					}

					} catch (Exception e) {
						System.out.println(e);
						System.exit(-1);
					}
				}).start();



				// Control thread for server socket (TODO: Método para isto)
				new Thread(() -> { try {

					Socket new_socket = socket_control.accept();
					String ip = new_socket.getInetAddress().toString();

					// New neighbor:
					if (!this.neighborsIP.contains(ip)) {
						this.neighborsIP.add(ip);
						this.connections.put(ip,new_socket);
						this.topology.addVertex(ip);
						this.topology.addEdge(self_ip, ip);
						// TODO: Start new thread for neighbor
					}
					

					} catch (Exception e) {
						System.out.println(e);
						System.exit(-1);
					}
				}).start();





			// Data thread

            new Thread(() -> { try {

				// Create buffer for data packets
				byte[] data_buffer = new byte[20000];
				DatagramPacket data_packet = new DatagramPacket(data_buffer, data_buffer.length);

				while (true) {

					// Receive data packet
					socket_data.receive(data_packet);
					byte[] data = data_packet.getData();

					// Add neighbor if not already a neighbor (TODO: Método para isto, ou então ignorar pacotes de hosts desconhecidos)
					String incoming_ip = data_packet.getAddress().toString();
					if(neighborsIP.contains(incoming_ip)) neighborsIP.add(incoming_ip);

					// Flood neighbors
					for (String ip : neighborsIP) {

						// (except the one who sent packet)
						if(ip.equals(incoming_ip)) continue;

						DatagramPacket out_packet = new DatagramPacket(data, data.length, InetAddress.getByName(ip), 5000);
						socket_data.send(out_packet);
					}
				}

				} catch (Exception e) {
					System.out.println(e);
					System.exit(-1);
				}
            }).start();





        } catch (Exception e) {
            System.out.println(e);
			System.exit(-1);
        }
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
		return visited.stream().toList();
	}

	public List<String> getAdjVertices(String label) {
		return adjVertices.getOrDefault(label, null);
	}

	public Set<String> getNodes() {
		return adjVertices.keySet();
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
}
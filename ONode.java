import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ONode {

    private String neighborsIP[]; // neighbor ip
	private Graph topology;

    public ONode(String ips[]) {

        this.neighborsIP = ips;


		// Start building overlay topology
		String self_ip = "self";
		try {self_ip = InetAddress.getLocalHost().toString(); topology.addVertex(self_ip);} catch (UnknownHostException ignore) {}
		for (String ip : neighborsIP) {
			topology.addVertex(ip);
			topology.addEdge(self_ip, ip);
		}



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
						if(ip.equals(data_packet.getAddress().toString())) continue;
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

				// Send initial probe request
				//Graph.writeToSocket(null, topology);

				while (true) {
					socket_control.receive(ctrl_packet);
					byte[] data = ctrl_packet.getData();

					// TODO: é preciso usar TCP aqui
					//Graph other = Graph.readFromSocket(null);
					//boolean changed = topology.merge(other));
					//if(!changed) continue;
					//Graph.writeToSocket(null, topology);

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

        } catch (Exception e) {
            System.out.println(e);
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

	public static void writeToSocket(Socket s, Graph g) throws IOException {
		ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        out.writeObject(g);
        out.close();
	}
}
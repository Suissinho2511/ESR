import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        new ONode(args);
    }
}




class Graph implements Serializable {
	private class Vertex {
		String label;
		Vertex(String label) {
			this.label = label;
		}
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

	private List<String> getAdjVertices(String label) {
		return adjVertices.getOrDefault(label, null);
	}
}
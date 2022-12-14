import CAB.CABPacket;
import CAB.MessageType;
import CAB.CABHelloPacket;
import CAB.CABControlPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ONode {

	private Map<InetAddress, List<InetAddress>> serverToActiveNeighbours;
	// name of server and class that belongs to it
	private Map<InetAddress, AddressTable> addressTable;

	private final List<InetAddress> neighbours;

	private final ReadWriteLock neighbourIP_lock = new ReentrantReadWriteLock();
	private DatagramSocket socket_data;
	private ServerSocket socket_control;

	public ONode(String[] ips) throws UnknownHostException {

		// format: server a(node) b(efore)node server anode bnode...
		this.neighbours = Arrays.stream(ips).map((name) -> {
			try {
				return InetAddress.getByName(name);
			} catch (UnknownHostException e1) {
				return null;
			}
		}).collect(Collectors.toList());

		this.addressTable = new HashMap<>();

		// Just for etapa 3
		/*
		for (int i = 0; i < ips.length;) {
			InetAddress serverIP = InetAddress.getByName(ips[i]);
			i++;
			InetAddress sourceIP = InetAddress.getByName(ips[i]);
			i ++;
			List<InetAddress> destinationsIP = new ArrayList<>();

			while (!ips[i].equals("fim")) {
				destinationsIP.add(InetAddress.getByName(ips[i]));
				i++;
			}
			i++;

			addConnection(serverIP, sourceIP, destinationsIP);
		}*/



		System.out.println("[INFO] Address Table:\n" + this.addressTable.toString());

		serverToActiveNeighbours = new HashMap<>();

		try {

			// Listen for UDP traffic on port 5000 - Data
			this.socket_data = new DatagramSocket(5000);
			System.out.println("[INFO] Data socket created");

			// Listen for TCP connections on port 5001 - Control
			this.socket_control = new ServerSocket(5001);
			System.out.println("[INFO] Control server socket created");

			// Connect to neighbours
			/*
			 * for (InetAddress ip : this.neighboursIP) {
			 * try {
			 * sendPing(ip, "Hello!");
			 * System.out.println("[INFO] Connected to neighbour " + ip.toString());
			 * } catch (Exception offline) {
			 * System.out.println("[WARNING] Neighbour " + ip.toString() +
			 * " offline! (May be an endpoint)");
			 * }
			 * }
			 */

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
		return new Thread(() -> {
			try {

				// Process
				DataInputStream in = new DataInputStream(s.getInputStream());
				CABPacket packet = new CABPacket(in);
				InetAddress neighbourIP = s.getInetAddress();

				System.out.println("[DEBUG] Received " + packet.type.toString() + " from "+ neighbourIP.toString());

				if(!isNeighbour(neighbourIP)) {
					addNeighbour(neighbourIP);
					System.out.println("[INFO] New neighbour: " + neighbourIP);
				}

				switch (packet.type) {

					case HELLO:
						if (!(packet.message instanceof CABHelloPacket)) {
							System.out.println("[DEBUG] This packet doesn't contain the correct information");
							break;
						}

						CABHelloPacket helloPacket = (CABHelloPacket) packet.message;

						String str = helloPacket.getMessage();
						System.out.println("[DEBUG] Received ping message from " + neighbourIP.toString()
								+ ":\n" + str);
						break;

					case CHOOSE_SERVER:
						if (!(packet.message instanceof CABControlPacket)) {
							System.out.println("[DEBUG] This packet doesn't contain the correct information");
							break;
						}
						if(addressTable.isEmpty()){
							System.out.println("[DEBUG] Topology not done yet :/");
							break;
						}

						CABControlPacket controlPacket = (CABControlPacket) packet.message;

						/*if (controlPacket.getAvailableJumps() <= 0
								|| controlPacket.getPathAsInetAddress().contains(s.getLocalAddress()))
							break;*/

						InetAddress serverIP = controlPacket.getServer();

						controlPacket.addNode(s.getLocalAddress());

						// sent to those that hasn't passed through
						for (InetAddress ip : addressTable.get(serverIP).getDestinations()) {
							Socket newSocket = new Socket(ip, 5001);
							if (!controlPacket.getPathAsInetAddress().contains(ip)) {
								new CABPacket(MessageType.CHOOSE_SERVER, controlPacket)
										.write(new DataOutputStream(newSocket.getOutputStream()));
							}
							newSocket.close();
						}

						break;

					case REPLY_CHOOSE_SERVER:

						if (!(packet.message instanceof CABControlPacket)) {
							System.out.println("[DEBUG] This packet doesn't contain the correct information");
							break;
						}

						if(addressTable.isEmpty()){
							System.out.println("[DEBUG] Topology not done yet :/");
							break;
						}

						CABControlPacket replyPacket = (CABControlPacket) packet.message;

						serverIP = replyPacket.getLast().getKey();

						removeActiveNeighbour(serverIP, neighbourIP);

						// If server stops being active, then we need to opt-out in previous node
						if (serverToActiveNeighbours.get(serverIP).isEmpty()) {
							Socket newSocket = new Socket(getSourceByServer(serverIP), 5001);
							DataOutputStream out = new DataOutputStream(newSocket.getOutputStream());
							new CABPacket(MessageType.OPTOUT, new CABHelloPacket(serverIP.toString())).write(out);
							newSocket.close();
							serverToActiveNeighbours.remove(serverIP);
						}

						InetAddress newServerIp = replyPacket.getServer();

						// If server doesn't exist, we add a new key
						if (!serverToActiveNeighbours.containsKey(newServerIp)) {
							serverToActiveNeighbours.put(newServerIp, new ArrayList<>());

							// if a new server is added, we need to inform the source node
							Socket newSocket = new Socket(getSourceByServer(serverIP), 5001);
							DataOutputStream out = new DataOutputStream(newSocket.getOutputStream());
							new CABPacket(MessageType.OPTIN, new CABHelloPacket(newServerIp.toString())).write(out);
							newSocket.close();
						}

						addActiveNeighbour(newServerIp, neighbourIP);

						System.out.println("[DEBUG] Active neighbours: "+this.serverToActiveNeighbours.toString());

						break;
					case TOPOLOGY:
						if (!(packet.message instanceof CABControlPacket)) {
							System.out.println("[DEBUG] This packet doesn't contain the correct information");
							break;
						}

						CABControlPacket topologyPacket = (CABControlPacket) packet.message;

						serverIP = topologyPacket.getServer();

						// If there is already a connection with this server..
						if(addressTable.containsKey(serverIP)){
							AddressTable connection = addressTable.get(serverIP);

							//Then we need to see if the new packet has a bigger delay
							// or if it has the same, compares the number of jumps
							if(connection.getExpectedDelay() < topologyPacket.getDelay() ||
									(connection.getExpectedDelay() == topologyPacket.getDelay() &&
									connection.getJumps() < topologyPacket.getCurrentJumps()))
								break; // breaks if its worse than what we have

						}

						AddressTable connection = new AddressTable(
								serverIP, neighbourIP, topologyPacket.getDelay(), topologyPacket.getCurrentJumps());

						addressTable.put(serverIP, connection);

						// First we send packet to all neighbours except the source
						for (InetAddress neighbour : neighbours) {

							//if the neighbour is the same we don't do anything
							if(neighbour.equals(neighbourIP)) continue;

							// open socket that connects with neighbour
							Socket newSocket = new Socket(neighbour, 5001);
							DataOutputStream out = new DataOutputStream(newSocket.getOutputStream());

							//add our own IP
							CABControlPacket toSend = topologyPacket;
							toSend.addNode(newSocket.getLocalAddress());
							packet.message = toSend;

							//Send and close
							packet.write(out);
							newSocket.close();

							System.out.println("[DEBUG] TOPOLOGY packet sent to " + neighbour);
						}


						//then we give a response to source
						Socket newSocket = new Socket(neighbourIP, 5001);
						DataOutputStream out = new DataOutputStream(newSocket.getOutputStream());

						new CABPacket(MessageType.REPLY_TOPOLOGY, topologyPacket).write(out);

						newSocket.close();

						System.out.println("[DEBUG] Confirmation of connection sent to " + neighbourIP);

						break;

					case REPLY_TOPOLOGY:
						if (!(packet.message instanceof CABControlPacket)) {
							System.out.println("[DEBUG] This packet doesn't contain the correct information");
							break;
						}

						CABControlPacket replyTopologyPacket = (CABControlPacket) packet.message;

						serverIP = replyTopologyPacket.getServer();

						if(!addressTable.containsKey(serverIP)){
							System.out.println("[DEBUG] Server " + serverIP + " doesn't exist in my address table");
							break;
						}

						addressTable.get(serverIP).addConnection(neighbourIP);

						System.out.println("[DEBUG] Connection with " + neighbourIP + " confirmed");

						break;

					case OPTIN:
						if (!(packet.message instanceof CABHelloPacket)) {
							System.out.println("[DEBUG] This packet doesn't contain the correct information");
							break;
						}
						if(addressTable.isEmpty()){
							System.out.println("[DEBUG] Topology not done yet :/");
							break;
						}
						CABHelloPacket optinPacket = (CABHelloPacket) packet.message;

						String message = optinPacket.getMessage();

						if (message.equals("Im a client")) {
							// if it's a client, then the default server will be the first one
							serverIP = getServers().get(0);
						} else {
							// if it's a node, then its requesting a specific server
							serverIP = InetAddress.getByName(message);
						}

						if(!addressTable.containsKey(serverIP)){
							System.out.println("[DEBUG] " + serverIP + " is not on my address table");
							break;
						}

						if(!addressTable.get(serverIP).isConnection(neighbourIP))
							addressTable.get(serverIP).addConnection(neighbourIP);

						if(isActiveNeighbour(serverIP, neighbourIP)){
							System.out.println("[DEBUG] " + neighbourIP + " is already an active neighbour");
							break;
						}

						if (!serverToActiveNeighbours.containsKey(serverIP)) {
							serverToActiveNeighbours.put(serverIP, new ArrayList<>());

							// if a new server is added, we need to send this reply to before node of this
							// thing
							newSocket = new Socket(getSourceByServer(serverIP), 5001);
							out = new DataOutputStream(newSocket.getOutputStream());
							new CABPacket(MessageType.OPTIN, optinPacket).write(out);
							newSocket.close();
						}

						addActiveNeighbour(serverIP, neighbourIP);

						System.out.println("[DEBUG] Active neighbours: "+this.serverToActiveNeighbours.toString());

						break;
					case OPTOUT:
						if (!(packet.message instanceof CABHelloPacket)) {
							System.out.println("[DEBUG] This packet doesn't contain the correct information");
							break;
						}
						if(addressTable.isEmpty()){
							System.out.println("[DEBUG] Topology not done yet :/");
							break;
						}
						CABHelloPacket optoutPacket = (CABHelloPacket) packet.message;

						serverIP = InetAddress.getByName(optoutPacket.getMessage());

						if (!isActiveNeighbour(serverIP, neighbourIP)){
							System.out.println("[DEBUG] Neighbour " + neighbourIP +
									" is already not active to Server " + serverIP);
							break;
						}



						removeActiveNeighbour(serverIP, neighbourIP);
						if (serverToActiveNeighbours.get(serverIP).isEmpty()) {
							newSocket = new Socket(getSourceByServer(serverIP), 5001);
							out = new DataOutputStream(newSocket.getOutputStream());
							new CABPacket(MessageType.OPTOUT, optoutPacket).write(out);
							newSocket.close();
						}

						break;

					default:
						System.out.println("[DEBUG] Received unknown message from " + neighbourIP);
						break;
				}

				in.close();

			} catch (Exception e) {
				System.out
						.println("[ERROR] Control thread for neighbour " + s.getInetAddress().toString() + " crashed!");
				e.printStackTrace();


				//System.out.println("[INFO] Neighbour removed: " + s.getInetAddress().toString());
				//removeNeighbour(s.getInetAddress());
			}
		});
	}

	private Thread newControlThread() {
		return new Thread(() -> {
			try {

				while (true) {

					Socket new_socket = socket_control.accept();
					// InetAddress ip = new_socket.getInetAddress();

					// New neighbour: (Vamos considerar algo estático na topologia)
					/*
					 * if (!isNeighbour(ip)) {
					 * System.out.println("[INFO] New neighbour: " + ip.toString());
					 * addNeighbour(ip);
					 * System.out.println("[DEBUG] Neighbours:\n" + neighboursIP.toString());
					 * sendPing(ip, "Hello!");
					 * }
					 */

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
		return new Thread(() -> {
			try {

				// Create buffer for data packets
				byte[] data_buffer = new byte[20000];
				DatagramPacket data_packet = new DatagramPacket(data_buffer, data_buffer.length);

				while (true) {

					// Receive data packet
					socket_data.receive(data_packet);

					// We need to know the serverIP address
					byte[] data = data_packet.getData();
					RTPpacket packet = new RTPpacket(data, data.length);
					InetAddress serverIP = packet.getServerIP();
					System.out.println("[DEBUG] Received data from "+ data_packet.getAddress() +" (server "+serverIP+")");

					// Flood neighbours
					this.neighbourIP_lock.readLock().lock();
					// just sends packets to whomever wants

					if (serverToActiveNeighbours.get(serverIP) != null){

						for (InetAddress ip : serverToActiveNeighbours.get(serverIP)) {

							DatagramPacket out_packet = new DatagramPacket(data, data.length, ip, 5000);
							socket_data.send(out_packet);
							System.out.println("[DEBUG] Sent data to "+ip);
						}
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
/*
	private void addConnection(InetAddress serverIP, InetAddress destinationIP) {
		this.neighbourIP_lock.writeLock().lock();

		this.addressTable.get(serverIP).getValue().add(destinationIP);

		this.neighbourIP_lock.writeLock().unlock();

	}

	private void createConnection(InetAddress serverIP, InetAddress sourceIP) {
		this.neighbourIP_lock.writeLock().lock();
		Map.Entry<InetAddress, List<InetAddress>> conections = new AbstractMap.SimpleEntry<InetAddress, List<InetAddress>>(
				sourceIP, new ArrayList<>());

		this.addressTable.put(serverIP, conections);

		this.neighbourIP_lock.writeLock().unlock();

	}*/


	private boolean isNeighbour(InetAddress ip) {
		this.neighbourIP_lock.readLock().lock();
		boolean result = this.neighbours.contains(ip);
		this.neighbourIP_lock.readLock().unlock();
		return result;
	}

	private void addNeighbour(InetAddress ip) {
		this.neighbourIP_lock.readLock().lock();
		this.neighbours.add(ip);
		this.neighbourIP_lock.readLock().unlock();
	}

	private void removeNeighbour(InetAddress ip) {
		this.neighbourIP_lock.readLock().lock();
		if(this.neighbours.contains(ip)) this.neighbours.remove(ip);
		//TODO: remover dos outros mapas?
		this.neighbourIP_lock.readLock().unlock();
	}

	private List<InetAddress> getServers() {
		return this.addressTable.keySet().stream().collect(Collectors.toList());
	}

	/*
	private List<InetAddress> getDestinationsByServer(InetAddress serverIP) {
		return this.addressTable.get(serverIP).getValue();
	}

	/*
	 * private List<InetAddress> getDestinations() {
	 * Set<InetAddress> destinations = new LinkedHashSet<InetAddress>();
	 * Collection<Map<InetAddress, InetAddress>> values =
	 * this.addressTable.values();
	 * for (Map<InetAddress, InetAddress> value : values) {
	 * destinations.addAll(value.keySet());
	 * }
	 * return destinations.stream().collect(Collectors.toList());
	 * }
	 */

	public InetAddress getSourceByServer(InetAddress serverIP) {
		return this.addressTable.get(serverIP).getSourceIP();
	}

	/*
	 * private List<InetAddress> getSources() {
	 * Set<InetAddress> sources = new LinkedHashSet<InetAddress>();
	 * Collection<Map<InetAddress, InetAddress>> values =
	 * this.addressTable.values();
	 * for (Map<InetAddress, InetAddress> value : values) {
	 * sources.addAll(value.values());
	 * }
	 * return sources.stream().collect(Collectors.toList());
	 * }
	 

	private List<InetAddress> getNeighbours() {
		Set<InetAddress> neighbours = new LinkedHashSet<InetAddress>();
		neighbours.addAll(getDestinations());
		neighbours.addAll(getSources());
		return neighbours.stream().collect(Collectors.toList());
	}*/

	private boolean isActiveNeighbour(InetAddress serverIP, InetAddress neighbourIP) {
		if(!serverToActiveNeighbours.containsKey(serverIP)) return false;
		return serverToActiveNeighbours.get(serverIP).contains(neighbourIP);
	}

	private void addActiveNeighbour(InetAddress serverIP, InetAddress neighbourIP) {
		serverToActiveNeighbours.get(serverIP).add(neighbourIP);
	}

	private void removeActiveNeighbour(InetAddress serverIP, InetAddress neighbourIP) {
		serverToActiveNeighbours.get(serverIP).remove(neighbourIP);
	}

	public static void main(String[] args) throws UnknownHostException {

		new ONode(args);
	}
}

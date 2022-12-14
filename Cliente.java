/* ------------------
   Cliente
   usage: java Cliente
   adaptado dos originais pela equipa docente de ESR (nenhumas garantias)
   colocar o cliente primeiro a correr que o servidor dispara logo!
---------------------- */

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

import CAB.CABControlPacket;
import CAB.CABHelloPacket;
import CAB.CABPacket;
import CAB.MessageType;

import static CAB.MessageType.OPTOUT;
import static CAB.MessageType.TOPOLOGY;

public class Cliente {

  // GUI
  // ----
  JFrame f = new JFrame("Cliente de Testes");
  JButton setupButton = new JButton("Setup");
  JButton playButton = new JButton("Play");
  JButton pauseButton = new JButton("Pause");
  JButton tearButton = new JButton("Teardown");
  JPanel mainPanel = new JPanel();
  JPanel buttonPanel = new JPanel();
  JLabel iconLabel = new JLabel();
  ImageIcon icon;

  // RTP variables:
  // ----------------
  DatagramPacket rcvdp; // UDP packet received from the server (to receive)
  DatagramSocket RTPsocket; // socket to be used to send and receive UDP packet
  static int RTP_RCV_PORT = 5000; // port where the client will receive the RTP packets

  Timer cTimer; // timer used to receive data from the UDP socket
  byte[] cBuf; // buffer used to store data received from the server

  // Server information
  InetAddress activeNeighbour;
  static InetAddress bestServer;
  static Long bestDelay;

  // --------------------------
  // Constructor
  // --------------------------
  public Cliente(String activeNeighbour) throws UnknownHostException {

    this.activeNeighbour = InetAddress.getByName(activeNeighbour);

    // build GUI
    // --------------------------

    // Frame
    f.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    // Buttons
    buttonPanel.setLayout(new GridLayout(1, 0));
    buttonPanel.add(setupButton);
    buttonPanel.add(playButton);
    buttonPanel.add(pauseButton);
    buttonPanel.add(tearButton);

    // handlers... (so dois)
    playButton.addActionListener(new playButtonListener());
    tearButton.addActionListener(new tearButtonListener());
    pauseButton.addActionListener(new pauseButtonListener());

    // Image display label
    iconLabel.setIcon(null);

    // frame layout
    mainPanel.setLayout(null);
    mainPanel.add(iconLabel);
    mainPanel.add(buttonPanel);
    iconLabel.setBounds(0, 0, 380, 280);
    buttonPanel.setBounds(0, 280, 380, 50);

    f.getContentPane().add(mainPanel, BorderLayout.CENTER);
    f.setSize(new Dimension(390, 370));
    f.setVisible(true);

    // init para a parte do cliente
    // --------------------------
    cTimer = new Timer(20, new clientTimerListener());
    cTimer.setInitialDelay(0);
    cTimer.setCoalesce(true);
    cBuf = new byte[15000]; // allocate enough memory for the buffer used to receive data from the server

    try {
      // socket e video
      RTPsocket = new DatagramSocket(RTP_RCV_PORT); // init RTP socket (o mesmo para o cliente e servidor)
      RTPsocket.setSoTimeout(5000); // setimeout to 5s
    } catch (SocketException e) {
      System.out.println("Cliente: erro no socket: " + e.getMessage());
    }

    controlPackets();
  }

  // ------------------------------------
  // main
  // ------------------------------------
  public static void main(String argv[]) throws Exception {
    // send SETUP message to the server
    Cliente cliente = new Cliente(argv[0]);
  }

  private void controlPackets() {
	
	ServerSocket serverSocket = null;
    try {serverSocket = new ServerSocket(5001);} catch(IOException e) {System.out.println("ServerSocket woopsie");System.exit(-1);}

      // receive packet
	while(true) { try {
		Socket socket = serverSocket.accept();
		DataInputStream in = new DataInputStream(socket.getInputStream());
		CABPacket packet = new CABPacket(in);
		InetAddress ip = socket.getInetAddress();

		System.out.println("[DEBUG] Received " + packet.type.toString() + " from "+ ip.toString());

		// process packet
		switch (packet.type) {
			// Will just print the message
			case HELLO:
			if (packet.message instanceof CABHelloPacket) {
				CABHelloPacket helloPacket = (CABHelloPacket) packet.message;
				
				String str = helloPacket.getMessage();
				System.out.println("[DEBUG] Received ping message from " + socket.getInetAddress().toString()
					+ ":\n" + str);

				//reply
				sendControlPacket(new CABPacket(MessageType.HELLO, new CABHelloPacket("Yes, I'm still alive...")));

			} else {
				System.out.println("Something's wrong with this HELLO packet");
			}
			break;

			// Will compare with current server and reply if anything changed
			case CHOOSE_SERVER:
			if (packet.message instanceof CABControlPacket) {
				CABControlPacket controlPacket = (CABControlPacket) packet.message;
				InetAddress packetServer = controlPacket.getServer();
				Long packetDelay = controlPacket.getDelay();

				if (bestServer == null || bestDelay > packetDelay) {

				// append old best server
				if(bestServer != null) controlPacket.addNode(bestServer);

				packet.message = controlPacket;
				packet.type = MessageType.REPLY_CHOOSE_SERVER;
				socket = new Socket(socket.getInetAddress(), 5001);
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				packet.write(out);

				bestServer = packetServer;
				bestDelay = packetDelay;
				System.out.println("Best server: "+bestServer.toString()+"; Best delay: "+bestDelay);

				} else if (bestServer == packetServer) {
				bestDelay = packetDelay;
				}

			} else {
				System.out.println("Something's wrong with this CHOOSE_SERVER packet");
			}

			break;

			case TOPOLOGY:
			if (!(packet.message instanceof CABControlPacket)) {
				System.out.println("[DEBUG] This packet doesn't contain the correct information");
				break;
			}

			CABControlPacket topologyPacket = (CABControlPacket) packet.message;

			InetAddress neighbourIP = socket.getInetAddress();

			Socket newSocket = new Socket(neighbourIP, 5001);
			DataOutputStream out = new DataOutputStream(newSocket.getOutputStream());


			new CABPacket(MessageType.REPLY_TOPOLOGY,
					new CABHelloPacket("a" + topologyPacket.getServer().toString().substring(1))).write(out);


			newSocket.close();

			System.out.println("[DEBUG] Confirmation of connection sent to " + neighbourIP);
			break;

			default:
			break;
		}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
  }

  // ------------------------------------
  // Send control packet
  // ------------------------------------
  private void sendControlPacket(CABPacket packet) {
    // send RTSP request
    try (Socket socket = new Socket(this.activeNeighbour, 5001)) {
      // use the RTSPBufferedWriter to write to the RTSP socket
      DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      packet.write(out);
      System.out.println("Cliente: Enviado comando " + packet);
    } catch (IOException ex) {
      System.out.println("Cliente: Erro no envio do comando ");
    }
  }

  // ------------------------------------
  // Handler for buttons
  // ------------------------------------

  // Handler for Play button
  // -----------------------
  class playButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {

      System.out.println("Play Button pressed !");
	  
      if (bestServer == null)
        sendControlPacket(new CABPacket(MessageType.OPTIN, new CABHelloPacket("Im a client")));
      else
        sendControlPacket(new CABPacket(MessageType.OPTIN, new CABHelloPacket(bestServer.toString())));

      // start the timers ...
      cTimer.start();
    }
  }

  // Handler for Pause button
  // -----------------------
  class pauseButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {

      System.out.println("Pause Button pressed !");

      // stop the timers ...
      cTimer.stop();
    }
  }

  // Handler for tear button
  // -----------------------
  class tearButtonListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {

      System.out.println("Teardown Button pressed !");

      // optout
      sendControlPacket(new CABPacket(MessageType.OPTOUT, new CABHelloPacket(bestServer.toString())));

      // stop the timer
      cTimer.stop();

      // exit
      System.exit(0);
    }
  }

  // ------------------------------------
  // Handler for timer (para cliente)
  // ------------------------------------

  class clientTimerListener implements ActionListener {

    public void actionPerformed(ActionEvent e) {

      // Construct a DatagramPacket to receive data from the UDP socket
      rcvdp = new DatagramPacket(cBuf, cBuf.length);

      try {
        // receive the DP from the socket:
        RTPsocket.receive(rcvdp);

        // create an RTPpacket object from the DP
        RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
		if(bestServer == null) bestServer = rtp_packet.getServerIP();
		if(bestDelay == null) bestDelay = new Long(99999999);

        // print important header fields of the RTP packet received:
        //System.out.println("Got RTP packet with SeqNum # " + rtp_packet.getsequencenumber() + " TimeStamp "
        //    + rtp_packet.gettimestamp() + " ms, of type " + rtp_packet.getpayloadtype());

        // print header bitstream:
        //rtp_packet.printheader();

        // get the payload bitstream from the RTPpacket object
        int payload_length = rtp_packet.getpayload_length();
        byte[] payload = new byte[payload_length];
        rtp_packet.getpayload(payload);

        // get an Image object from the payload bitstream
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.createImage(payload, 0, payload_length);

        // display the image as an ImageIcon object
        icon = new ImageIcon(image);
        iconLabel.setIcon(icon);
      } catch (InterruptedIOException iioe) {
        System.out.println("Nothing to read");
      } catch (IOException ioe) {
        System.out.println("Exception caught: " + ioe);
      }
    }
  }

}// end of Class Cliente

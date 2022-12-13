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
  static int RTP_RCV_PORT = 25000; // port where the client will receive the RTP packets

  Timer cTimer; // timer used to receive data from the UDP socket
  byte[] cBuf; // buffer used to store data received from the server

  // Server information
  InetAddress activeNeighbour;
  static InetAddress bestServer;
  static Long bestDelay;

  // --------------------------
  // Constructor
  // --------------------------
  public Cliente() {

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
  }

  // ------------------------------------
  // main
  // ------------------------------------
  public static void main(String argv[]) throws Exception {
    // send SETUP message to the server
    Cliente t = new Cliente();
    controlPackets();
  }

  private static void controlPackets() {

    try (ServerSocket serverSocket = new ServerSocket(5001)) {
      // receive packet
      Socket socket = serverSocket.accept();
      DataInputStream in = new DataInputStream(socket.getInputStream());
      CABPacket packet = new CABPacket(in);

      // process packet
      switch (packet.type) {
        // Will just print the message
        case HELLO:
          if (packet.message instanceof CABHelloPacket) {
			CABHelloPacket helloPacket = (CABHelloPacket) packet.message;
			
            String str = helloPacket.getMessage();
            System.out.println("[DEBUG] Received ping message from " + socket.getInetAddress().toString()
                + ":\n" + str);
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
              bestServer = packetServer;
              bestDelay = packetDelay;

			  // append new best server
			  controlPacket.addNode(bestServer);
			  packet.message = controlPacket;
              packet.type = MessageType.REPLY_CHOOSE_SERVER;
              DataOutputStream out = new DataOutputStream(socket.getOutputStream());
              packet.write(out);

            } else if (bestServer == packetServer) {
              bestDelay = packetDelay;
            }

          } else {
            System.out.println("Something's wrong with this CHOOSE_SERVER packet");
          }

          break;

        // IDK
        case TOPOLOGY:

          break;
        default:
          break;
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // ------------------------------------
  // Send control packet
  // ------------------------------------
  private static void sendControlPacket(InetAddress neighbour, CABPacket packet) {
    // send RTSP request
    try (Socket socket = new Socket(neighbour, 5001)) {
      // use the RTSPBufferedWriter to write to the RTSP socket
      CABControlPacket command = new CABControlPacket(99, socket.getLocalAddress(), System.currentTimeMillis());
      DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      command.write(out);
      System.out.println("Cliente: Enviado comando " + command);
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
      // stop the timer
      cTimer.stop();

      // optout
      CABPacket optout = new CABPacket(OPTOUT, null);
      // TODO:
      // optout.write(out);

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

        // print important header fields of the RTP packet received:
        System.out.println("Got RTP packet with SeqNum # " + rtp_packet.getsequencenumber() + " TimeStamp "
            + rtp_packet.gettimestamp() + " ms, of type " + rtp_packet.getpayloadtype());

        // print header bitstream:
        rtp_packet.printheader();

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

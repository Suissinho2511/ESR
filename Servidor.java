
import CAB.CABControlPacket;
import CAB.CABPacket;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.swing.*;
import javax.swing.Timer;

import static CAB.MessageType.CHOOSE_SERVER;
import static CAB.MessageType.TOPOLOGY;

public class Servidor extends JFrame implements ActionListener {

  // GUI:
  // ----------------
  JLabel label;

  // RTP variables:
  // ----------------
  DatagramPacket senddp; // UDP packet containing the video frames (to send)A
  DatagramSocket RTPsocket; // socket to be used to send and receive UDP packet
  int RTP_dest_port = 5000; // destination port for RTP packets
  InetAddress ClientIPAddr; // Client IP address

  static String VideoFileName; // video file to request to the server

  // Video constants:
  // ------------------
  int imagenb = 0; // image nb of the image currently transmitted
  VideoStream video; // VideoStream object used to access video frames
  static int MJPEG_TYPE = 26; // RTP payload type for MJPEG video
  static int FRAME_PERIOD = 42; // Frame period of the video to stream, in ms
  static int VIDEO_LENGTH = 500; // length of the video in frames

  Timer sTimer; // timer used to send the images at the video frame rate
  byte[] sBuf; // buffer used to store the images to send to the client

  private ServerSocket socketControl; // for sending control packets

  // --------------------------
  // Constructor
  // --------------------------
  public Servidor(String argv) throws IOException {
    // init Frame
    super("Servidor");

    // Topology constructor

    this.socketControl = new ServerSocket(5001);

    topologyConstructor(InetAddress.getByName(argv));
    controlSendThread(new Socket(argv, 5001)).start();
    // System.out.println("ola");

    // init para a parte do servidor
    sTimer = new Timer(FRAME_PERIOD, this); // init Timer para servidor
    sTimer.setInitialDelay(0);
    sTimer.setCoalesce(true);
    sBuf = new byte[15000]; // allocate memory for the sending buffer

    try {
      RTPsocket = new DatagramSocket(); // init RTP socket
      ClientIPAddr = InetAddress.getByName(argv);
      RTPsocket.connect(ClientIPAddr, RTP_dest_port);
      System.out.println("Servidor: socket " + ClientIPAddr);
      video = new VideoStream(VideoFileName); // init the VideoStream object:
      System.out.println("Servidor: vai enviar video da file " + VideoFileName);

    } catch (SocketException e) {
      System.out.println("Servidor: erro no socket: " + e.getMessage());
    } catch (Exception e) {
      System.out.println("Servidor: erro no video: " + e.getMessage());
    }

    // Handler to close the main window
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        // stop the timer and exit
        sTimer.stop();
        System.exit(0);
      }
    });

    // GUI:
    label = new JLabel("Send frame #        ", JLabel.CENTER);
    getContentPane().add(label, BorderLayout.CENTER);

    sTimer.start();

  }

  // ------------------------------------
  // main
  // ------------------------------------
  public static void main(String argv[]) throws Exception {
    // get video filename to request:
    VideoFileName = "movie.Mjpeg";

    File file = new File(VideoFileName);
    if (file.exists()) {
      // Create a Main object

      Servidor server = new Servidor(argv[0]);
      server.pack();
      server.setVisible(false);
    } else
      System.out.println("Ficheiro de video não existe: " + VideoFileName);
  }

  // ------------------------
  // Handler for timer
  // ------------------------
  public void actionPerformed(ActionEvent e) {

    // if the current image nb is less than the length of the video
    if (imagenb < VIDEO_LENGTH) {
      // update current imagenb
      imagenb++;

      try {
        // get next frame to send from the video, as well as its size
        int image_length = video.getnextframe(sBuf);

        // Builds an RTPpacket object containing the frame
        InetAddress ip = RTPsocket.getLocalAddress();
        System.out.println("IP in string format: " + ip.getHostAddress());
        int serverIP = 0;
        for (byte b : ip.getAddress()) {
          serverIP = serverIP << 8 | (b & 0xFF);
        }
        System.out.println("IP in int format: " + serverIP);

        RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb * FRAME_PERIOD, sBuf, image_length, serverIP);

        System.out.println("IP in string format: " + rtp_packet.getServerIP());
        // get to total length of the full rtp packet to send
        int packet_length = rtp_packet.getlength();

        // retrieve the packet bitstream and store it in an array of bytes
        byte[] packet_bits = new byte[packet_length];
        rtp_packet.getpacket(packet_bits);

        // send the packet as a DatagramPacket over the UDP socket
        senddp = new DatagramPacket(packet_bits, packet_length, ClientIPAddr, RTP_dest_port);
        RTPsocket.send(senddp);

        System.out.println("Send frame #" + imagenb);
        // print the header bitstream
        rtp_packet.printheader();

        // update GUI
        // label.setText("Send frame #" + imagenb);
      } catch (Exception ex) {
        System.out.println("Exception caught: " + ex);
        System.exit(0);
      }
    } else {
      // if we have reached the end of the video file, stop the timer
      sTimer.stop();
    }
  }

  private void topologyConstructor(InetAddress ip) throws IOException {
    // Time to make a tree :D

    Socket new_socket = new Socket(ip, 5001);
    CABPacket topologyConstrutor = new CABPacket(
            TOPOLOGY,
            new CABControlPacket(10, new_socket.getLocalAddress()));


    topologyConstrutor.write(new DataOutputStream(new_socket.getOutputStream()));
    System.out.println("[DEBUG] Sent first topology packet");

    // é preciso receber algo?
    // DataInputStream in = new DataInputStream(new_socket.getInputStream());

    new_socket.close();
  }

  private Thread controlSendThread(Socket socket) {
    return new Thread(() -> {
      try {
        while (true) {
          CABPacket controlPacket = new CABPacket(
              CHOOSE_SERVER,
              new CABControlPacket(10, socket.getLocalAddress()));

          controlPacket.write(new DataOutputStream(socket.getOutputStream()));
          Thread.sleep(1000);
        }

      } catch (UnknownHostException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

    });
  }

  private Thread controlReceiveTread(Socket s) {
    return new Thread(() -> {

    });
  }

}// end of Class Servidor

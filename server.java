import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public Server() {
        try {
            ServerSocket server = new ServerSocket(5000);
            Socket socket = server.accept();
            // DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            while (true) {
                out.writeUTF("Hello Client");
                out.flush();
            }
            // String str = in.readUTF();
            // System.out.println("Client: " + str);
            // out.writeUTF("Hello Client");
            // out.flush();
            out.close();
            // in.close();
            socket.close();
            server.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
    }

}

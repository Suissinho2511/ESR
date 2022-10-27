import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ONode {
    public ONode() {
        try {
            ServerSocket server = new ServerSocket(5000);
            Socket socket = server.accept();
            Socket socketS = new Socket("10.0.0.10", 5000);
            DataInputStream in = new DataInputStream(socketS.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            while (true) {
                String str = in.readUTF();
                System.out.println("Server: " + str);
                out.writeUTF(str);
                Thread.sleep(1000);
                out.flush();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        ONode oNode = new ONode();
    }
}

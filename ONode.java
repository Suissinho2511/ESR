import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ONode {
    public ONode(String ip) {
        try {
            ServerSocket serverR = new ServerSocket(5000);
            Socket socketR = serverR.accept();
            Socket socketS = new Socket(ip, 5000);
            DataInputStream in = new DataInputStream(socketR.getInputStream());
            DataOutputStream out = new DataOutputStream(socketS.getOutputStream());
            while (true) {
                String str = in.readUTF();
                System.out.println("Server: " + str);
                out.writeUTF(str);
                out.flush();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        ONode oNode = new ONode(args[0]);
    }
}

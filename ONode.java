import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ONode {
    public ONode(String ip1, String ip2) {
        try {
            try (Socket socketR = new Socket(ip1, 5000)) {
                try (Socket socketS = new Socket(ip2, 5000)) {
                    DataInputStream in = new DataInputStream(socketR.getInputStream());
                    DataOutputStream out = new DataOutputStream(socketS.getOutputStream());
                    while (true) {
                        String str = in.readUTF();
                        System.out.println("Server: " + str);
                        out.writeUTF(str);
                        out.flush();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        ONode oNode = new ONode(args[1], args[2]);
    }
}

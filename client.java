import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class client {
    public void cliente() {
        try {
            Socket socket = new Socket("10.0.0.10", 5000);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("Hello Server");
            out.flush();
            String str = in.readUTF();
            System.out.println("Server: " + str);
            out.close();
            in.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        client client = new client();
        client.cliente();
    }
}

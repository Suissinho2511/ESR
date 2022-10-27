import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class Client {
    public Client() {
        try {
            Socket socket = new Socket("localhost", 5000);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            // DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            while (true) {
                String str = in.readUTF();
                System.out.println("Server: " + str);
            }
            // out.close();
            in.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
    }
}

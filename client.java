import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class Client {
    public Client() {
        try {
            Socket socket = new Socket("10.0.0.11", 5000);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            while (true) {
                String str = in.readUTF();
                System.out.println("Server: " + str);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
    }
}

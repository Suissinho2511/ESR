import java.io.DataInputStream;
import java.net.Socket;

public class Client {
    public Client(String ip) {
        try {
            try (Socket socket = new Socket(ip, 5000)) {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                while (true) {
                    String str = in.readUTF();
                    System.out.println("Server: " + str);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        Client client = new Client(args[1]);
    }
}

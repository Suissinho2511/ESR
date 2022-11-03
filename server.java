import java.io.DataOutputStream;
import java.net.Socket;

public class Server {
    public Server(String ip) {
        try {
            try (Socket socket = new Socket(ip, 5000)) {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                int i = 0;
                while (true) {
                    out.writeUTF("Hello Client : " + i++);
                    out.flush();
                }
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        Server server = new Server(args[0]);
    }

}

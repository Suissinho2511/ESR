import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public Server() {
        try {
            ServerSocket server = new ServerSocket(5000);
            Socket socket = server.accept();
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            int i = 0;
            while (true) {
                out.writeUTF("Hello Client : " + i++);
                Thread.sleep(1000);
                out.flush();
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
    }

}

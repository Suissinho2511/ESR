import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class server {
    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(5000);
            Socket socket = server.accept();
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String str = "", str2 = "";
            while (!str.equals("stop")) {
                str = input.readUTF();
                System.out.println("Client says: " + str);
                str2 = br.readLine();
                output.writeUTF(str2);
                output.flush();
            }
            input.close();
            socket.close();
            server.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

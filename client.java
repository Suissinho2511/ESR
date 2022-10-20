import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class client {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5000);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String str = "", str2 = "";
            while (!str.equals("stop")) {
                str = br.readLine();
                output.writeUTF(str);
                output.flush();
                str2 = input.readUTF();
                System.out.println("Server says: " + str2);
            }
            input.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
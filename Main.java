import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            CABp cabp = new CABp(InetAddress.getByName("127.0.0.1"), 5);
            String[] path = cabp.getPath();
            for (String ip : path) {
                System.out.println(ip);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}

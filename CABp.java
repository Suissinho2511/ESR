import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

public class CABp {

    // ------------------------------------
    // Confusing and bloated protocol
    // ------------------------------------

    private final InetAddress serverIP;
    private final long initTimestamp;
    private HashMap<InetAddress, Long> path;
    private int availableJumps;

    public CABp(InetAddress serverIP, int maxJumps) {
        this.serverIP = serverIP;
        this.initTimestamp = System.currentTimeMillis();
        this.path = new HashMap<>();
        this.availableJumps = maxJumps;
    }

    public int getAvailableJumps() {
        return this.availableJumps;
    }

    public InetAddress getServerIP() {
        return this.serverIP;
    }

    public void addNode(InetAddress ip) {
        path.put(ip, System.currentTimeMillis());
        availableJumps--;
    }

    public long getDelay() {
        return System.currentTimeMillis() - initTimestamp;
    }

    public String[] getPath() {
        return path.keySet().stream().map(InetAddress::getHostAddress).toArray(String[]::new);
    }

}

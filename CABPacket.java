import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class CABPacket implements Serializable{

    private String type;
    private int availableJumps;

    private LinkedHashMap<InetAddress, Long> path;
    


    public CABPacket(String type, int maxJumps, InetAddress currAddress, Long timestamp) {
        this.type = type;
        this.availableJumps = maxJumps;
        this.path = new LinkedHashMap<>();
        this.path.put(currAddress, timestamp);
    }

    public CABPacket(DatagramPacket packet, InetAddress currAddress, Long timestamp){
        byte[] payload = packet.getData();


    }
    

    public <K, V> Entry<InetAddress, Long> getFirst() {
        if (this.path.isEmpty()) return null;
        return this.path.entrySet().iterator().next();
    }

    public <K, V> Entry<InetAddress, Long> getLast() throws NoSuchFieldException, IllegalAccessException{
        Field tail = this.path.getClass().getDeclaredField("tail");
        tail.setAccessible(true);
        return (Entry<InetAddress, Long>) tail.get(this.path);
      }



    public int getAvailableJumps() {
        return this.availableJumps;
    }

    public void addNode(InetAddress ip) {
        path.put(ip, System.currentTimeMillis());
        availableJumps--;
    }

    public long getDelay() {
        return System.currentTimeMillis() - this.getFirst().getValue();
    }

    public long getDelay(InetAddress source) {
        return System.currentTimeMillis() - this.path.get(source);
    }


    public String[] getPath() {
        return path.keySet().stream().map(InetAddress::getHostAddress).toArray(String[]::new);
    }

}

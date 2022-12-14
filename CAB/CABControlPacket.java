package CAB;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.ArrayList;

public class CABControlPacket {

    private int availableJumps;
    private int currentJumps;
    private LinkedHashMap<InetAddress, Long> path;

    public CABControlPacket(int maxJumps, InetAddress currAddress) {
        this.availableJumps = maxJumps;
        this.currentJumps = 0;
        this.path = new LinkedHashMap<>();
        this.path.put(currAddress, System.currentTimeMillis());
    }


    public CABControlPacket(DataInputStream in) throws IOException {
        read(in);
    }

    public <K, V> Entry<InetAddress, Long> getFirst() {
        if (this.path.isEmpty())
            return null;
        return this.path.entrySet().iterator().next();
    }

    //@SuppressWarnings("unchecked")
    public <K, V> Entry<InetAddress, Long> getLast() throws NoSuchFieldException, IllegalAccessException {
        /*Field tail = this.path.getClass().getDeclaredField("tail");
        tail.setAccessible(true);
        return (Entry<InetAddress, Long>) tail.get(this.path);*/
		List<Entry<InetAddress, Long>> lentries = new ArrayList<Entry<InetAddress, Long>>(this.path.entrySet());
		return lentries.get(lentries.size() - 1);
    }

    public InetAddress getServer() {
        return getFirst().getKey();
    }

    public int getAvailableJumps() {
        return this.availableJumps;
    }

    public void addNode(InetAddress ip) {
        this.path.put(ip, System.currentTimeMillis());
        this.availableJumps--;
        this.currentJumps++;
    }

    public long getDelay() {
        return System.currentTimeMillis() - this.getFirst().getValue();
    }

    public long getDelay(InetAddress source) {
        return System.currentTimeMillis() - this.path.get(source);
    }

    public int getCurrentJumps() {
        return currentJumps;
    }

    public void setCurrentJumps(int currentJumps) {
        this.currentJumps = currentJumps;
    }

    public String[] getPath() {
        return path.keySet().stream().map(InetAddress::getHostAddress).toArray(String[]::new);
    }

    public List<InetAddress> getPathAsInetAddress() {
        return List.copyOf(this.path.keySet());
    }

    public void read(DataInputStream in) throws IOException {
        this.availableJumps = in.readInt();
        this.currentJumps = in.readInt();

		this.path = new LinkedHashMap<>();

        for (int i = 0; i <= this.currentJumps; i++) {
            this.path.put(InetAddress.getByName(in.readUTF().substring(1)), in.readLong());
        }
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(this.availableJumps);
        out.writeInt(this.currentJumps);
        for (Entry<InetAddress, Long> entry : this.path.entrySet()) {
            out.writeUTF(entry.getKey().toString());
            out.writeLong(entry.getValue());
        }
        out.flush();
    }

    @Override
    public String toString(){
        return "Available Jumps: " + this.availableJumps + ",\n" +
                "Current Jumps: " + this.currentJumps + ",\n" +
                "Path: " + this.path.toString();
    }
}


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.nio.ByteBuffer;


public class CABControlPacket extends CABPacket {

    private int availableJumps;
    private int currentJumps;
    private LinkedHashMap<InetAddress, Long> path;

	

    public CABControlPacket(int maxJumps, InetAddress currAddress, Long timestamp) {
        this.type = 0;
        this.availableJumps = maxJumps;
		this.currentJumps = 0;
        this.path = new LinkedHashMap<>();
        this.path.put(currAddress, timestamp);
    }

    public CABControlPacket(DataInputStream in) throws IOException {

        type = in.readInt();
        availableJumps = in.readInt();
		currentJumps = in.readInt();

        for(int i = 0; i < currentJumps; i++)
        {
			String ip = in.readUTF();
			Long time = in.readLong();

            InetAddress add = InetAddress.getByAddress(ip.getBytes());
            path.put(add, time);
        }

		in.close();
    }
    


    public <K, V> Entry<InetAddress, Long> getFirst() {
        if (this.path.isEmpty()) return null;
        return this.path.entrySet().iterator().next();
    }

	@SuppressWarnings("unchecked")
    public <K, V> Entry<InetAddress, Long> getLast() throws NoSuchFieldException, IllegalAccessException {
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
		currentJumps++;
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


	public void write(DataOutputStream out) throws IOException {
		out.writeInt(type);
		out.writeInt(availableJumps);
		out.writeInt(currentJumps);
		for(Entry<InetAddress, Long> entry : path.entrySet()) {
			out.writeUTF(entry.getKey().toString());
			out.writeUTF(entry.getValue().toString());
		}
		out.flush();
		out.close();
	}
}

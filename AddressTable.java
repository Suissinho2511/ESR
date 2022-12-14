import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class AddressTable {
    private InetAddress serverIP;
    private InetAddress sourceIP;
    private Long expectedDelay;
    private int jumps;
    private boolean state;
    private List<InetAddress> destinations;


    //Servidor, Fluxo, Custo, Origem, Destinos, Estado


    public AddressTable(InetAddress serverIP, InetAddress sourceIP, Long expectedDelay, int jumps) {
        this.serverIP = serverIP;
        this.sourceIP = sourceIP;
        this.expectedDelay = expectedDelay;
        this.jumps = jumps;
        this.destinations = new ArrayList<>();
    }

    public InetAddress getServerIP() {
        return serverIP;
    }

    public void setServerIP(InetAddress serverIP) {
        this.serverIP = serverIP;
    }

    public InetAddress getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(InetAddress sourceIP) {
        this.sourceIP = sourceIP;
    }

    public Long getExpectedDelay() {
        return expectedDelay;
    }

    public void setExpectedDelay(Long expectedDelay) {
        this.expectedDelay = expectedDelay;
    }

    public int getJumps() {
        return jumps;
    }

    public void setJumps(int jumps) {
        this.jumps = jumps;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public List<InetAddress> getDestinations() {
        return destinations;
    }

    public void setDestinations(List<InetAddress> destinations) {
        this.destinations = destinations;
    }

    public void addConnection(InetAddress destination){
        this.destinations.add(destination);
    }

    public boolean isConnection(InetAddress destination){return this.destinations.contains(destination);}

}

import java.io.Serializable;

public abstract class CABPacket implements Serializable {

    // -------------------------------------
    // Confusing And Bloated protocol Packet
    // -------------------------------------


	/*
	 * +----------------+----------------------------------------------------------+
	 * | 	  type		|							...							   |
	 * +----------------+----------------------------------------------------------+
	 * 
	 */
    public String type;
}

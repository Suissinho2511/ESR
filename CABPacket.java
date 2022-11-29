import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CABPacket {

	/*
		0: Hello
		1: Probe path
		2: Reply path
	*/
    public int type;



	public CABPacket() {
	}



	public void read(DataInputStream in) throws IOException {
		this.type = in.readInt();
	}

	public void write(DataOutputStream out) throws IOException {
		out.writeInt(this.type);
		out.flush();
	}
}

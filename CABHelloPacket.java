import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CABHelloPacket extends CABPacket {
	
	private String message;



	public CABHelloPacket(String message) {
		this.type = 0;
		this.message = message;
	}

	public CABHelloPacket(DataInputStream in) throws IOException {
		this.message = in.readUTF();
	}



	public String getMessage() {return this.message;}



	public void write(DataOutputStream out) throws IOException {
		super.write(out);
		out.writeUTF(this.message);
		out.flush();
	}
}

package CAB;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CABHelloPacket {
	private String message;

	public CABHelloPacket(String message) {
		this.message = message;
	}

	public CABHelloPacket(DataInputStream in) throws IOException {
		read(in);
	}

	public String getMessage() {
		return this.message;
	}

	public void read(DataInputStream in) throws IOException {
		this.message = in.readUTF();
	}

	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(this.message);
		out.flush();
	}

	@Override
	public String toString(){
		return this.message;
	}
}

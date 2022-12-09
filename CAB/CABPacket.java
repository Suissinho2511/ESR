package CAB;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CABPacket {
    public MessageType type;
	public Object message;

	public CABPacket() {
	}



	public void read(DataInputStream in) throws IOException {
		this.type = MessageType.fromInteger(in.readInt());
		switch (type){
			case HELLO:
				message = new CABHelloPacket(in);
				break;
			case PROBE_PATH:
			case REPLY_PATH:
				message = new CABControlPacket(in);
				break;

		}



	}

	public void write(DataOutputStream out) throws IOException {
		out.writeInt(MessageType.toInteger(this.type));
		switch (type){
			case HELLO:
				if (message instanceof CABHelloPacket helloMessage){
					helloMessage.write(out);
				}
				break;
			case PROBE_PATH:
				if (message instanceof CABControlPacket controlMessage){
					controlMessage.write(out);
				}
				break;

		}
		out.flush();
	}
}

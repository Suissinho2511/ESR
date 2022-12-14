package CAB;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CABPacket {
	public MessageType type;
	public Object message;

	public CABPacket() {
	}

	public CABPacket(MessageType type, Object message) {
		this.type = type;
		this.message = message;
	}

	public CABPacket(DataInputStream in) throws IOException {
		read(in);
	}

	public void read(DataInputStream in) throws IOException {
		this.type = MessageType.fromInteger(in.readInt());
		switch (type) {
			case HELLO:
			case OPTIN:
			case OPTOUT:
				message = new CABHelloPacket(in);
				break;
			case CHOOSE_SERVER:
			case REPLY_CHOOSE_SERVER:
			case TOPOLOGY:
			case REPLY_TOPOLOGY:
				message = new CABControlPacket(in);
				break;

		}

	}

	public void write(DataOutputStream out) throws IOException {
		out.writeInt(MessageType.toInteger(this.type));
		switch (type) {
			case HELLO:
			case OPTIN:
			case OPTOUT:
				if (message instanceof CABHelloPacket) {
					CABHelloPacket helloMessage = (CABHelloPacket) message;
					helloMessage.write(out);
				}
				break;
			case CHOOSE_SERVER:
			case REPLY_CHOOSE_SERVER:
			case TOPOLOGY:
			case REPLY_TOPOLOGY:
				if (message instanceof CABControlPacket) {
					CABControlPacket controlMessage = (CABControlPacket) message;
					controlMessage.write(out);
				}
				break;

		}
		out.flush();
	}

	@Override
	public String toString(){
		return "Type: " + this.type.toString() + ",\n" +
				this.message.toString();
	}
}

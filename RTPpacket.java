//class RTPpacket

public class RTPpacket {

  // size of the RTP header:
  static int HEADER_SIZE = 12;

  // Fields that compose the RTP header
  // TODO descrição dos campos
  public int version;

  public int padding;
  public int extension;
  public int cc;
  public int marker;
  public int payloadType;
  public int sequenceNumber;
  public int timeStamp;
  public int ssrc;

  // Bitstream of the RTP header
  public byte[] header;

  // size of the RTP payload
  public int payload_size;
  // Bitstream of the RTP payload
  public byte[] payload;

  // --------------------------
  // Constructor of an RTPpacket object from header fields and payload bitstream
  // --------------------------
  public RTPpacket(int PType, int Framenb, int Time, byte[] data, int data_length) {
    // fill by default header fields:
    this.version = 2;
    padding = 0;
    extension = 0;
    cc = 0;
    marker = 0;
    ssrc = 0;

    // fill changing header fields:
    sequenceNumber = Framenb;
    timeStamp = Time;
    payloadType = PType;

    // build the header bistream:
    // --------------------------
    header = new byte[HEADER_SIZE];

    // .............
    // TO COMPLETE
    // .............
    // fill the header array of byte with RTP header fields
    header[0] = (byte) (version << 6 | padding << 5 | extension << 4 | cc);
    header[1] = (byte) (marker << 7 | payloadType & 0x000000FF);
    header[2] = (byte) (sequenceNumber >> 8);
    header[3] = (byte) (sequenceNumber & 0xFF);
    header[4] = (byte) (timeStamp >> 24);
    header[5] = (byte) (timeStamp >> 16);
    header[6] = (byte) (timeStamp >> 8);
    header[7] = (byte) (timeStamp & 0xFF);
    header[8] = (byte) (ssrc >> 24);
    header[9] = (byte) (ssrc >> 16);
    header[10] = (byte) (ssrc >> 8);
    header[11] = (byte) (ssrc & 0xFF);

    // fill the payload bitstream:
    // --------------------------
    payload_size = data_length;
    payload = new byte[data_length];

    // fill payload array of byte from data (given in parameter of the constructor)
    // ......
    for (int i = 0; i < data_length; i++)
      payload[i] = data[i];

    // ! Do not forget to uncomment method printheader() below !

  }

  // --------------------------
  // Constructor of an RTPpacket object from the packet bistream
  // --------------------------
  public RTPpacket(byte[] packet, int packet_size) {
    // fill default fields:
    version = 2;
    padding = 0;
    extension = 0;
    cc = 0;
    marker = 0;
    ssrc = 0;

    // check if total packet size is lower than the header size
    if (packet_size >= HEADER_SIZE) {
      // get the header bitsream:
      header = new byte[HEADER_SIZE];
      for (int i = 0; i < HEADER_SIZE; i++)
        header[i] = packet[i];

      // get the payload bitstream:
      payload_size = packet_size - HEADER_SIZE;
      payload = new byte[payload_size];
      for (int i = HEADER_SIZE; i < packet_size; i++)
        payload[i - HEADER_SIZE] = packet[i];

      // interpret the changing fields of the header:
      payloadType = header[1] & 127;
      sequenceNumber = unsigned_int(header[3]) + 256 * unsigned_int(header[2]);
      timeStamp = unsigned_int(header[7]) + 256 * unsigned_int(header[6]) + 65536 * unsigned_int(header[5])
          + 16777216 * unsigned_int(header[4]);
    }
  }

  // --------------------------
  // getpayload: return the payload bistream of the RTPpacket and its size
  // --------------------------
  public int getpayload(byte[] data) {

    for (int i = 0; i < payload_size; i++)
      data[i] = payload[i];

    return (payload_size);
  }

  // --------------------------
  // getpayload_length: return the length of the payload
  // --------------------------
  public int getpayload_length() {
    return (payload_size);
  }

  // --------------------------
  // getlength: return the total length of the RTP packet
  // --------------------------
  public int getlength() {
    return (payload_size + HEADER_SIZE);
  }

  // --------------------------
  // getpacket: returns the packet bitstream and its length
  // --------------------------
  public int getpacket(byte[] packet) {
    // construct the packet = header + payload
    for (int i = 0; i < HEADER_SIZE; i++)
      packet[i] = header[i];
    for (int i = 0; i < payload_size; i++)
      packet[i + HEADER_SIZE] = payload[i];

    // return total size of the packet
    return (payload_size + HEADER_SIZE);
  }

  // --------------------------
  // gettimestamp
  // --------------------------

  public int gettimestamp() {
    return (timeStamp);
  }

  // --------------------------
  // getsequencenumber
  // --------------------------
  public int getsequencenumber() {
    return (sequenceNumber);
  }

  // --------------------------
  // getpayloadtype
  // --------------------------
  public int getpayloadtype() {
    return (payloadType);
  }

  // --------------------------
  // print headers without the SSRC
  // --------------------------
  public void printheader() {
    System.out.print("[RTP-Header] ");
    System.out.println("Version: " + version
        + ", Padding: " + padding
        + ", Extension: " + extension
        + ", CC: " + cc
        + ", Marker: " + marker
        + ", PayloadType: " + payloadType
        + ", SequenceNumber: " + sequenceNumber
        + ", TimeStamp: " + timeStamp);
  }

  // return the unsigned value of 8-bit integer nb
  static int unsigned_int(int nb) {
    if (nb >= 0)
      return (nb);
    else
      return (256 + nb);
  }

}

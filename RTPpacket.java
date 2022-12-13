//class RTPpacket

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RTPpacket {

  // size of the RTP header:
  static int HEADER_SIZE = 12;

  // Fields that compose the RTP header
  // TODO descrição dos campos
  // This 2-bit field defines version number.
  public int version;
  // The length of this field is 1-bit. If value is 1,
  // then it denotes presence of padding at end of packet and
  // if value is 0, then there is no padding.
  public int padding;
  // The length of this field is also 1-bit. If value of this field
  // is set to 1, then its indicates an extra extension header between
  // data and basic header and if value is 0 then, there is no extra extension.
  public int extension;
  // Contributor count – This 4-bit field indicates number of contributors.
  // Here maximum possible number of contributor is 15 as a 4-bit field
  // can allow number from 0 to 15.
  public int cc;
  // The length of this field is 1-bit, and it is used as end marker by
  // application to indicate end of its data.
  public int marker;
  // This field is of length 7-bit to indicate type of payload.
  // We list applications of some common types of payload.
  public int payloadType;
  // The length of this field is 16 bits. It is used to give
  // serial numbers to RTP packets. It helps in sequencing.
  // The sequence number for first packet is given a random number and
  // then every next packet’s sequence number is incremented by 1.
  // This field mainly helps in checking lost packets and order mismatch.
  public int sequenceNumber;
  // The length of this field is 32-bit. It is used to find relationship between
  // times of different RTP packets. The timestamp for first packet is given
  // randomly
  // and then time stamp for next packets given by sum of previous timestamp
  // and time taken to produce first byte of current packet. The value of 1 clock
  // tick is varying from application to application.
  public int timeStamp;
  // This is a 32-bit field used to identify and define the source.
  // The value for this source identifier is a random number that
  // is chosen by source itself. This mainly helps in solving conflict
  // arises when two sources started with the same sequencing number.
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
  public RTPpacket(int PType, int Framenb, int Time, byte[] data, int data_length, int serverIP) {
    // fill by default header fields:
    this.version = 2;
    padding = 0;
    extension = 0;
    cc = 0;
    marker = 0;
    ssrc = serverIP;

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
      ssrc = unsigned_int(header[11]) + 256 * unsigned_int(header[10]) + 65536 * unsigned_int(header[9])
      + 16777216 * unsigned_int(header[8]);
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

  public InetAddress getServerIP() throws UnknownHostException {
    //byte[] ip = BigInteger.valueOf(ssrc).toByteArray();
    String ipStr = String.format("%d.%d.%d.%d",
          (ssrc >> 24 & 0xff),   
          (ssrc >> 16 & 0xff), 
          (ssrc >> 8 & 0xff),    
          (ssrc & 0xff));
    
    InetAddress result = InetAddress.getByName(ipStr);
    return result;
  }

  // return the unsigned value of 8-bit integer nb
  static int unsigned_int(int nb) {
    if (nb >= 0)
      return (nb);
    else
      return (256 + nb);
  }

}

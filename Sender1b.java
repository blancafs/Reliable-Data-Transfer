import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Date;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;


public class Sender1b {

	private SenderState senderState;
	int datasize = 1024;
	int packetsize = 1027;
	private Date start;
	private Date end;
	public int retransmissions;
	private int port;
	private int timeout;
	private String serverName;
	private String filename;
	private InetAddress address;


	public Sender1b(String servername, String serverport, String filename, String timeout) throws Exception {
		// Communications will always start in sender state 0
		senderState = new SenderState0();
		retransmissions = 0;
		this.serverName = servername;
		this.address = InetAddress.getByName(serverName);
		this.port = Integer.parseInt(serverport);
		this.filename = filename;
		this.timeout = Integer.parseInt(timeout);

	}

	public Sender1b sendFile() throws Exception {
		// Creating socket, and setting correct timeout
		DatagramSocket socket = new DatagramSocket();
		socket.setSoTimeout(timeout);

		// Read wanted file and turn into bytes
		File f = new File(this.filename);
		byte[] filebytes = new byte[(int)f.length()];
		FileInputStream fis = new FileInputStream(f);
		fis.read(filebytes);
		fis.close();

		// Send each packet made with packify
		ArrayList<DatagramPacket> packetsToSend = packify(filebytes);
		// Start timer to measure throughput
		start = new Date();
		for (DatagramPacket packet : packetsToSend) {
			senderState.sendPacket(this, packet, socket);
		}
		end = new Date();
		long time = end.getTime()-start.getTime();
		double tput = f.length()/time;
		// Only printed line from Sender1b
		System.out.println(retransmissions + " " + tput);
		// Close socket when all packages sent
		socket.close();
		
		// Return sender object
		return this;

	}
	
	public ArrayList<DatagramPacket> packify(byte[] topackify) {
		ArrayList<byte[]> packets = new ArrayList<byte[]>();
		ArrayList<DatagramPacket> finalpackets = new ArrayList<DatagramPacket>();
		
		// Nos is the number of full packets of size 1027 made, while dif is the length of the last packet
		int length = topackify.length;
		int nos = (int) Math.floor(length/datasize);
		int dif = length - (nos*datasize);
		byte[] packet = new byte[datasize];

		// Loop iterates through the number of packages, copying the correct amount of data into each one,
		// taking particular care for the last packet which can have a different size
		for (int i=0; i<nos+1; i++) {
			if (i==nos) { // when i==nos, we are packing the last packet
				packet = new byte[dif];
				packet = Arrays.copyOfRange(topackify, nos*datasize, length);
				packets.add(packet);
				break;
			}
			packet = Arrays.copyOfRange(topackify, i*datasize, (i+1)*datasize);
			packets.add(packet);
			packet = new byte[datasize];
		}

		// For each 1024 byte packet to send, we add the correct sequence number and eof
		for (byte[] packeta : packets) {
			byte[] finalpacket = new byte[packeta.length+3];
			byte[] eof = new byte[1];
			byte[] sequenceno = new byte[2];
			if (packets.indexOf(packeta)==packets.size()-1) { // If its the last packet
				eof[0] |= (1<<0);
			}
			if (packets.indexOf(packeta)%2==1) { // Packets should be sequenced 0,1,0,1 etc starting with 0
				sequenceno[1] |= (1<<0);
			}
			for (int i=3; i<finalpacket.length; i++) { // The rest of the packet is the data
				finalpacket[i] = packeta[i-3];
			}
			finalpacket[0] = sequenceno[0];
			finalpacket[1] = sequenceno[1];
			finalpacket[2] = eof[0];

			// We add the finalised datagram packet to the final arraylist
			DatagramPacket fpacket = new DatagramPacket(finalpacket, finalpacket.length, address, port);
			finalpackets.add(fpacket);
		}
		return finalpackets;
	}

	public void changeState(SenderState senderstate) {
		// Used when a package is sent correctly
		this.senderState=senderstate;
	}

	// Main
	public static void main(String[] args) throws Exception, Exception {
		if (args.length<2) {
			// When wrong arguments given, simple error message
			System.out.println("Incorrect arguments given.");
			return;
		}
		Sender1b newsender = new Sender1b(args[0],args[1], args[2], args[3]).sendFile();
	}
	
	//////////////////////////////////////
	// ////////// FSM CLASSES ////////////
	//////////////////////////////////////
	
	
	public abstract class SenderState {

		boolean done = false;

		public abstract void sendPacket(Sender1b sender, DatagramPacket packet, DatagramSocket socket) throws Exception;

	}
	class SenderState0 extends SenderState{

		@Override
		public void sendPacket(Sender1b sender, DatagramPacket packet, DatagramSocket socket) throws Exception {

			boolean sending = true;
			boolean last = true;
			int lasttries = 0;
			DatagramPacket emptyack = null;
			int packetsize = 1027;
			int datasize = 1024;
			int acksize = 2;

			while (sending) {
				// If fails to receive ack 10 times, we end loop and assume it was the last one and ack got lost but receiver finished
				if (lasttries==10) {
					return;
				}
				
				socket.send(packet); 

				// To know if last packet
				byte[] fulldata = packet.getData();
				byte eof = fulldata[2];
				String eofs = Integer.toBinaryString((eof & 0xFF) + 0x100).substring(1);
				
				// Receive ack, catching the timeout exception
				byte[] empty2 = new byte[acksize];
				emptyack = new DatagramPacket(empty2, empty2.length);
				try {
					socket.receive(emptyack);
				} catch (SocketTimeoutException e) {
					if (eofs.contains("1")) {
						lasttries++; // If last packet, and timeout occurs, only let it happen 10 times
					}
					sender.retransmissions++;
					continue; // Timeout reached, resend packet!
				}

				// Verify ack received
				byte[] fulldata2 = emptyack.getData();
				byte seq2 = fulldata2[1];
				String seqs2 = Integer.toBinaryString((seq2 & 0xFF) + 0x100).substring(1);
				if (!(seqs2.contains("1"))) {  // If no 1, correct ack received in senderstate0, we end while loop and move on to state 1
					sending = false;
					sender.changeState(new SenderState1());
				} else if (seqs2.contains("1")) { // If incorrect ack, resend
					sender.retransmissions++;
					continue;
				}
				// If sending = true, the ack was not right, and so packet is sent again
			}
			return;
		}
	}
	class SenderState1 extends SenderState{
		

		@Override
		public void sendPacket(Sender1b sender, DatagramPacket packet, DatagramSocket socket) throws Exception {

			boolean sending = true;
			DatagramPacket emptyack = null;
			int packetsize = 1027;
			int datasize = 1024;
			int acksize = 2;
			int lasttries = 0;

			while (sending) {
				// If fails to receive ack 10 times, we end loop and assume it was the last one and ack got lost but receiver finished
				if (lasttries==10) {
					return;
				}
				socket.send(packet); // Timer started

				// To know if last packet
				byte[] fulldata = packet.getData();
				byte eof = fulldata[2];
				String eofs = Integer.toBinaryString((eof & 0xFF) + 0x100).substring(1);
				
				// Receive ack, catching the timeout exception
				byte[] empty2 = new byte[acksize];
				emptyack = new DatagramPacket(empty2, empty2.length);
				try {
					socket.receive(emptyack);
				} catch (SocketTimeoutException e) {
					if (eofs.contains("1")) {
						lasttries++; // If last packet, and timeout occurs, only let it happen 10 times
					}
					sender.retransmissions++;
					continue; // Resend packet!
				}

				// Verify ack
				byte[] fulldata2 = emptyack.getData();
				byte seq2 = fulldata[1];
				String seqs2 = Integer.toBinaryString((seq2 & 0xFF) + 0x100).substring(1);
				if (seqs2.contains("1")) { // If 1 is in sq, correct ack received in senderstate1, we end while loop and move on to state 0
					sending = false;
					sender.changeState(new SenderState0());
				} else if (!(seqs2.contains("1"))) { // If incorrect ack, resend
					sender.retransmissions++;
					continue; 
				}
				// If sending = true, the ack was not right, and so packet is sent again
			}
			return;
		}
	}

}

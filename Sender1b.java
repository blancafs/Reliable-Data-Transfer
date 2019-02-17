import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;


public class Sender1b {
	
	private SenderState senderState;
	int datasize = 1024;
	int packetsize = 1027;
	private int port;
	private int timeout;
	private String serverName;
	private String filename;
	private InetAddress address;
	
	

	public Sender1b(String servername, String serverport, String filename, String timeout) throws Exception {
		// Communications will always start in sender state 0
		senderState = new SenderState0();
		this.serverName = servername;
		this.address = InetAddress.getByName(serverName);
		this.port = Integer.parseInt(serverport);
		this.filename = filename;
		this.timeout = Integer.parseInt(timeout);
	
	}
	
	public Sender1b sendFile() throws Exception {
		// Creating socket, and setting input timeout
		DatagramSocket socket = new DatagramSocket();
		//socket.setSoTimeout(timeout);
		
		// Read wanted file and turn into bytes, then packets, and send one by one
		File f = new File(this.filename);
		byte[] filebytes = new byte[(int)f.length()];
		FileInputStream fis = new FileInputStream(f);
		fis.read(filebytes);
		fis.close();
		
		// Send each packet made with packify
		ArrayList<DatagramPacket> packetsToSend = packify(filebytes);
		for (DatagramPacket packet : packetsToSend) {
			System.out.println("Sending packet " + packetsToSend.indexOf(packet)+ " to senderstate method.");
			senderState.sendPacket(this, packet, socket);
		}
		System.out.println("All packets sent!");
		// Close socket when all packages sent
		socket.close();
		
		return this;
		
	}
	
	public ArrayList<DatagramPacket> packify(byte[] topackify) {
		ArrayList<byte[]> packets = new ArrayList<byte[]>();
		ArrayList<DatagramPacket> finalpackets = new ArrayList<DatagramPacket>();
		
		int length = topackify.length;
		int nos = (int) Math.floor(length/datasize);
		int dif = length - (nos*datasize);
		byte[] packet = new byte[datasize];
		
		// Iterate through number of packages, copying correct amount of data into each one,
		// taking particular care for the last packet which can have a different size
		for (int i=0; i<nos+1; i++) {
			if (i==nos) {
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
			if (packets.indexOf(packeta)==packets.size()-1) {
				eof[0] |= (1<<0);
			}
			if (packets.indexOf(packeta)%2==1) {
				sequenceno[1] |= (1<<0);
			}
			for (int i=3; i<finalpacket.length; i++) {
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
		this.senderState=senderstate;
		
	}
	
	// Arrify is a simple method to append every element of each byte array in an Arraylist to one final byte array
	public byte[] arrify(ArrayList<byte[]> tosend) {
		int size = 0;
		for (int i=0; i<tosend.size(); i++) {
			size += tosend.get(i).length;
		}
		
		byte[] fin = new byte[size];
		for (int i=0; i<tosend.size(); i++) {
			int length = tosend.get(i).length;
			System.arraycopy(tosend.get(i), 0, fin, i*1024, length);
		}
		
		return fin; 
	}
	
	// Main
	public static void main(String[] args) throws Exception, Exception {
		if (args.length<2) {
			System.out.println("Incorrect arguments given.");
			return;
		}
		Sender1b newsender = new Sender1b(args[0],args[1], args[2], args[3]).sendFile();
		System.out.println("Sent all packets successfully.");
	}
}

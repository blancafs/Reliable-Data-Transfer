import java.io.File;
import java.io.FileInputStream;
import java.lang.Math;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

class Sender1a{
	
	final int packetsize = 1027; // 1027bytes a packet
	final int datasize = 1024; // data section is 1024 bytes
	private int serverPort;
	private String fileName;
	private InetAddress address;
	private DatagramSocket socket;

	public Sender1a(String servername, String serverport, String filename) throws Exception {
		this.serverPort = Integer.parseInt(serverport);
		this.address = InetAddress.getByName(servername);
		this.fileName = filename;
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
			finalpacket[0] = sequenceno[0];
			finalpacket[1] = sequenceno[1];
			finalpacket[2] = eof[0];
			for (int i=3; i<finalpacket.length; i++) {
				finalpacket[i] = packeta[i-3];
			}
			// We add the finalised datagram packet to the final arraylist
			DatagramPacket fpacket = new DatagramPacket(finalpacket, finalpacket.length, address, serverPort);
			finalpackets.add(fpacket);
		}
		return finalpackets;
	}

	public Sender1a sendFile() throws Exception {
		// Creating socket
		socket = new DatagramSocket();
		
		// Read wanted file and turn into bytes, then packets, and send one by one
		File f = new File(this.fileName);
		byte[] filebytes = new byte[(int)f.length()];
		FileInputStream fis = new FileInputStream(f);
		fis.read(filebytes);
		fis.close();
		
		// Send each packet made with packify
		ArrayList<DatagramPacket> packed = this.packify(filebytes);
		for (DatagramPacket packet : packed) {
			TimeUnit.MILLISECONDS.sleep(20);
			socket.send(packet);
		}
		// Close socket when all packages sent
		socket.close();
		return this;
	}


	public static void main(String[] args) throws Exception {
		// A couple of possible argument corrections to avoid errors
		if (args.length<3) {
			System.out.println("Incorrect arguments given, try again.");
			return;
		}
		if (!(args[2].contains("jpg"))) {
			System.out.println("Filename must be a jpeg file.");
			return;
		}
		Sender1a newsender = (new Sender1a(args[0], args[1], args[2])).sendFile();
		System.out.println("Sent all packets successfully.");
	}
}

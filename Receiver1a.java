
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;

public class Receiver1a {
	private static DatagramSocket socket;
	private static boolean running;
	
	private File file;
	private ArrayList<byte[]> recPackets;
	private OutputStream os;

	public Receiver1a(int port, String filename) throws Exception {
		socket = new DatagramSocket(port);
		running = true;
		recPackets = new ArrayList<byte[]>();

		// to write sent packets to file
		file = new File(filename);
		os = new FileOutputStream(file);
	}

	public void run() throws Exception {
		while (running) {
			byte[] empty = new byte[1027];
			DatagramPacket recPacket = new DatagramPacket(empty, empty.length);
			System.out.println("Waiting for a packet....");
			socket.receive(recPacket);
			System.out.println("Received packet from " + recPacket.getSocketAddress());
			byte[] packet = recPacket.getData();

			// Checking if eof contains a 1, indicating last packet, so we make packet the right size
			String eofs = Integer.toBinaryString((packet[2] & 0xFF) + 0x100).substring(1);
			if (eofs.contains("1")) {
				int finallength = recPacket.getLength();
				packet = Arrays.copyOfRange(packet, 3, finallength);
				recPackets.add(packet);
				socket.close();
				running = false;
				break;
			}
			// if not the last one, simply get data with same size allocation for all
			byte[] data = Arrays.copyOfRange(packet, 3, packet.length);
			recPackets.add(data);
		}
		// When all packets received and socket closed, make a big array of the received packets and write to file
		byte[] tofile = arrify(recPackets);
		os.write(tofile);
		os.close();
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


public static void main(String[] args) throws Exception {
	if (args.length<2) {
		System.out.println("Incorrect arguments given.");
		return;
	}
	Receiver1a receiver = new Receiver1a(Integer.parseInt(args[0]), args[1]);
	receiver.run();
}
}

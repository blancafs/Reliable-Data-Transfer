import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.util.ArrayList;


public class Receiver1b {
	
	private ReceiverState receiverState;
	
	int port;
	private File file;
	private OutputStream os;
	private DatagramSocket socket;
	
	
	public Receiver1b(int port, String filename) throws Exception {
		// Both sender and receiver will start at state 0 (awaiting/sending packages with sequence number of 0)
		receiverState = new ReceiverState0();
		this.port = port;
		file = new File(filename);
		os = new FileOutputStream(file);
	}
	
	public void receive() throws Exception {
		socket = new DatagramSocket(port);
		ArrayList<byte[]> recPackets = new ArrayList<byte[]>();
		System.out.println("Socket open.");
		
		// Until eof with a 1 is found, will keep receiving packages.
		// The if loop within makes sure no duplicates are added to the final arraylist.
		while(!receiverState.isFinished()) {
			byte[] currpacket = receiverState.getPacket(this,socket);
			System.out.println("Actually received package in Receiver1b!");
			recPackets.add(currpacket);
		}
		socket.close();
		
		// Create final image byte array and write to file
		byte[] tofile = arrify(recPackets);
		os.write(tofile);
		System.out.println("Writing to final file ...");
		os.close();
		return;
	}

	public void changeState(ReceiverState receiverState) {
		this.receiverState=receiverState;
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
	public static void main(String[] args) throws Exception, Exception {
		if (args.length<2) {
			System.out.println("Incorrect arguments given.");
			return;
		}
		// Begin receiving
		Receiver1b receiver = new Receiver1b(Integer.parseInt(args[0]), args[1]);
		receiver.receive();
		System.out.println("All done.");
	}
}

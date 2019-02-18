import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.Arrays;


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

		// Until eof with a 1 is found, will keep receiving packages.
		while(!receiverState.isFinished()) {
			byte[] currpacket = receiverState.getPacket(this,socket);
			recPackets.add(currpacket);
		}
		// Close socket when done
		socket.close();

		// Create final image byte array and write to file
		byte[] tofile = arrify(recPackets);
		os.write(tofile);
		os.close();
		return;
	}

	public void changeState(ReceiverState receiverState) {
		// Changes receiver state from 0 to 1 or vice versa, every time after a correctly received package
		this.receiverState=receiverState;
	}

	// Arrify turns an arraylist into a single byte array we can write to file
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
		// Begin receiving
		Receiver1b receiver = new Receiver1b(Integer.parseInt(args[0]), args[1]);
		receiver.receive();
	}
	
	//////////////////////////////////////
	///////////// FSM CLASSES ////////////
	//////////////////////////////////////

	abstract class ReceiverState {

		Boolean done = false;

		public abstract byte[] getPacket(Receiver1b receiver, DatagramSocket socket) throws Exception;

		public Boolean isFinished() {
			return done;
		}
	}

	class ReceiverState0 extends ReceiverState{

		public byte[] getPacket(Receiver1b receiver, DatagramSocket socket) throws Exception {

			DatagramPacket packet = null;	// current packet retrieved
			boolean lastpacket = false;		// see if last
			boolean correct = false;
			final int packetsize = 1027;
			int datasize = 1024;
			byte[] returndata= null;       // returndata will hold final data to return to receiver class

			while (!correct) {
				returndata = new byte[datasize];

				// Receiving packet
				byte[] empty = new byte[packetsize];
				packet = new DatagramPacket(empty, empty.length);
				socket.receive(packet);

				// Interpreting data
				byte[] fulldata = packet.getData();
				byte[] seq = Arrays.copyOfRange(fulldata, 0, 2);
				byte eof = fulldata[2];
				String eofs = Integer.toBinaryString((eof & 0xFF) + 0x100).substring(1);
				String seqs = Integer.toBinaryString((seq[1] & 0xFF) + 0x100).substring(1);

				// Sending correct ack back
				SocketAddress address = packet.getSocketAddress();
				DatagramPacket ackp = new DatagramPacket(seq, seq.length, address);
				socket.send(ackp);

				// If wrong sequence number, send ack and keep going, if right number set correct to true to break while loop
				if (seqs.contains("1")) {
					continue;
				} else if (!(seqs.contains("1"))) {
					correct = true;
				}
				// Only correct package possible after above if loop, now simply have to distinguish whether last packet or not
				// and if last packet, account for different size
				if (eofs.contains("1")) {
					lastpacket = true;
					returndata = Arrays.copyOfRange(fulldata, 3, packet.getLength());
				} else {
					returndata = Arrays.copyOfRange(fulldata, 3, packetsize);
				}
			}

			if(lastpacket) {
				this.done = true;
			}else {
				receiver.changeState(new ReceiverState1());
			}
			// Returns received data as byte array
			return returndata;
		}

	}
	class ReceiverState1 extends ReceiverState{

		public byte[] getPacket(Receiver1b receiver, DatagramSocket socket) throws Exception {

			DatagramPacket packet = null;	// current packet retrieved
			boolean lastpacket = false;		// see if last
			boolean correct = false;
			final int packetsize = 1027;
			int datasize = 1024;
			byte[] returndata= null;       // returndata will hold final data to return to receiver class

			while (!correct) {
				returndata = new byte[datasize];

				// Receiving packet
				byte[] empty = new byte[packetsize];
				packet = new DatagramPacket(empty, empty.length);
				socket.receive(packet);

				// Interpreting data
				byte[] fulldata = packet.getData();
				byte[] seq = Arrays.copyOfRange(fulldata, 0, 2);
				byte eof = fulldata[2];
				String eofs = Integer.toBinaryString((eof & 0xFF) + 0x100).substring(1);
				String seqs = Integer.toBinaryString((seq[1] & 0xFF) + 0x100).substring(1);

				// Sending correct ack back
				SocketAddress address = packet.getSocketAddress();
				DatagramPacket ackp = new DatagramPacket(seq, seq.length, address);
				socket.send(ackp);

				// If wrong sequence number, send ack and keep going, if right number set correct to true to break while loop
				if (!(seqs.contains("1"))) {
					continue;
				} else {
					correct = true;
				}
				// Only correct package possible after above if loop, now simply have to distinguish whether last packet or not
				// and if last packet, account for different size
				if (eofs.contains("1")) {
					lastpacket = true;
					returndata = Arrays.copyOfRange(fulldata, 3, packet.getLength());
				} else {
					returndata = Arrays.copyOfRange(fulldata, 3, packetsize);
				}
			}

			if(lastpacket) {
				this.done = true;
			}else {
				receiver.changeState(new ReceiverState0());
			}
			// Returns received data as byte array
			return returndata;
		}

	}

}

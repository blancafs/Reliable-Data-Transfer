import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Arrays;

public class ReceiverState0 extends ReceiverState{
	
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
			System.out.println("Waiting for a packet 0....");
			socket.receive(packet);
			System.out.println("Received packet from " + packet.getSocketAddress());
			
			// Interpreting data 
			byte[] fulldata = packet.getData();
			byte[] seq = Arrays.copyOfRange(fulldata, 0, 2);
			byte eof = fulldata[2];
			String eofs = Integer.toBinaryString((eof & 0xFF) + 0x100).substring(1); 
			String seqs = Integer.toBinaryString((seq[1] & 0xFF) + 0x100).substring(1);
			
			// Sending correct ack back
			System.out.println("eofs :" + eofs);
			System.out.println("seqs :" + seqs);
			SocketAddress address = packet.getSocketAddress();
			DatagramPacket ackp = new DatagramPacket(seq, seq.length, address);
			socket.send(ackp);
			System.out.println("Sent ack.");
			
			// If wrong sequence number, send ack and keep going, if right number set correct to true to break while loop
			if (seqs.contains("1")) {
				System.out.println("Miscommunication, should have been 0. Sending ack1.");
				continue;
			} else if (!(seqs.contains("1"))) {
				correct = true;
				System.out.println("Received correct package seq=0.");
			}
			// Only correct package possible after above if loop, now simply have to distinguish whether last packet or not
			// and if last packet, account for different size
			if (eofs.contains("1")) {
				lastpacket = true;
				returndata = Arrays.copyOfRange(fulldata, 3, packet.getLength());
				System.out.print("Sample of returned data: ");
				System.out.println(Integer.toBinaryString((returndata[4] & 0xFF) + 0x100).substring(1));
				System.out.println("Final packet received.");
			} else {
				returndata = Arrays.copyOfRange(fulldata, 3, packetsize);
				System.out.print("Sample of returned data: ");
				System.out.println(Integer.toBinaryString((returndata[4] & 0xFF) + 0x100).substring(1));
			}
			System.out.println("Correct state: " + correct);
		}
		
		if (lastpacket) {
			this.done = true;
		}else {
			receiver.changeState(new ReceiverState1());
			System.out.println("Receiver received full packet 0, switching to ReceiverState1.");
		}
		// Returns received data as byte array
		return returndata;
	}
	
}

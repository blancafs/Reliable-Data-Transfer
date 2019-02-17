import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class SenderState0 extends SenderState{
	
		int packetsize = 1027;
		int datasize = 1024;
		int acksize = 2;

	@Override
	public void sendPacket(Sender1b sender, DatagramPacket packet, DatagramSocket socket) throws Exception {
		
		boolean sending = true;
		DatagramPacket emptyack = null;
		
		while (sending) {
			System.out.println("Sending packet seq=0 ....");
//			// Receiving old ack ???
//			byte[] empty = new byte[packetsize];
//			packet = new DatagramPacket(empty, empty.length);
//			System.out.println("Received late packet?....");
//			
//			System.out.println("Received packet from " + packet.getSocketAddress());
			
			socket.send(packet); // Timer started
			System.out.println("Packet sent, timer started.");

			// Testing seq being sent
			byte[] fulldata = packet.getData();
			byte[] seq = Arrays.copyOfRange(fulldata, 0, 2);
			byte eof = fulldata[2];
			String eofs = Integer.toBinaryString((eof & 0xFF) + 0x100).substring(1); 
			String seqs = Integer.toBinaryString((seq[1] & 0xFF) + 0x100).substring(1);
			System.out.println("eofs of packet sent: " + eofs);
			System.out.println("seqs of packet sent: " + seqs);
			
			// Receive ack
			byte[] empty2 = new byte[acksize];
			emptyack = new DatagramPacket(empty2, empty2.length);
			System.out.println("Waiting for ACK....");
			
			// Catch clause implements timeout
			try {
				socket.receive(emptyack);
				System.out.println("Received ACK from " + emptyack.getSocketAddress());
			} catch (SocketTimeoutException e) {
				System.out.println("Timeout reached! Have to resend packet ....");
				continue; // Resend packet!
			}
			
			// Verify ack
			byte[] fulldata2 = emptyack.getData();
			byte seq2 = fulldata2[1];
			String seqs2 = Integer.toBinaryString((seq2 & 0xFF) + 0x100).substring(1);
			
			// The first means correct acknowledgement sent, we have sent the right packet!
			if (!(seqs2.contains("1"))) {
				sending = false; // To stop while loop!
				sender.changeState(new SenderState1());
				System.out.println("Correct packet sent! Moved on to SenderState1.");
			} else if (seqs2.contains("1")) {
				System.out.println("Wrong ack=1 received, resending packet.");
				continue; // Packet got lost as ack is wrong, so we resend packet
			}
			// If sending = true, the ack was not right, and so packet is sent again
		}	
		return;
	}
	}

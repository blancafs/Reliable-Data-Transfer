import java.net.DatagramPacket;
import java.net.DatagramSocket;

public abstract class SenderState {
	
	boolean done = false;
	
	public abstract void sendPacket(Sender1b sender, DatagramPacket packet, DatagramSocket socket) throws Exception;
	
}

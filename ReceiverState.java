import java.net.DatagramSocket;

public abstract class ReceiverState {
	
	Boolean done = false;
	
	public abstract byte[] getPacket(Receiver1b receiver, DatagramSocket socket) throws Exception;
	
	public Boolean isFinished() {
		return done;
	}
}

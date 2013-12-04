import java.io.IOException;
import java.nio.channels.SelectionKey;


public interface SocketReadWriteHandler extends ChannelHandler {

	public enum State {
		READING_REQUEST, GENERATING_RESPONSE, SENDING_RESPONSE, SOCKET_CLOSED
	}
	
	public State state();
	public void setState(State s);
	public int initialOperations();
	public void handleRead(SelectionKey key) throws IOException;
	public void handleWrite(SelectionKey key) throws IOException;
}

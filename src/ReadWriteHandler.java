import java.io.IOException;
import java.nio.channels.SelectionKey;


public interface ReadWriteHandler extends Handler {
	
	public enum State {
		READING_REQUEST, PREPARING_RESPONSE, WRITING_RESPONSE, CLOSED
	}
	
	public State state();

	public void handleRead(SelectionKey key) throws IOException;
	
	public void handleWrite(SelectionKey key) throws IOException;
}

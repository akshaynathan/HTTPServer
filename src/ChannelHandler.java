import java.nio.channels.SelectionKey;

public interface ChannelHandler {
	public void handleException(SelectionKey key);
}

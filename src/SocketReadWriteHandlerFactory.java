import java.nio.channels.SocketChannel;

public interface SocketReadWriteHandlerFactory {
	public SocketReadWriteHandler createHandler(Dispatcher d, SocketChannel client);
}

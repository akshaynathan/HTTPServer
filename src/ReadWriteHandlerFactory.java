import java.nio.channels.SocketChannel;


public interface ReadWriteHandlerFactory {

	ReadWriteHandler build(Dispatcher d, SocketChannel sock);
}

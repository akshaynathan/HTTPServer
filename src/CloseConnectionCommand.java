import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class CloseConnectionCommand implements DispatcherCommand {

	SelectionKey k;

	public CloseConnectionCommand(SelectionKey key) {
		this.k = key;
	}

	@Override
	public void run(Dispatcher dispatcher) throws IOException {
		System.err.println("Client: "
				+ ((SocketChannel) k.channel()).socket().getInetAddress()
				+ " timed out. Closing Connection.");
		k.cancel();
		((SocketReadWriteHandler) k.attachment())
				.setState(SocketReadWriteHandler.State.SOCKET_CLOSED);
		k.channel().close();
	}
}

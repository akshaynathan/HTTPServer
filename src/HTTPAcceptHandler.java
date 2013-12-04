import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public class HTTPAcceptHandler implements AcceptHandler {

	
	int currentOps;
	private ServerSocketChannel server;
	private Dispatcher d;
	private HTTPReadWriteHandlerFactory factory;
	private TimeoutThread tt;
	
	public HTTPAcceptHandler(ServerSocketChannel server, Dispatcher d,
			HTTPReadWriteHandlerFactory s, TimeoutThread t) {
		this.server = server;
		this.d = d;
		this.factory = s;
		this.tt = t;
		currentOps = SelectionKey.OP_ACCEPT;
	}
	
	@Override
	public void handleAccept() throws IOException {
		SocketChannel sock = server.accept();
		sock.configureBlocking(false);

		HTTPReadWriteHandler rwh = factory.build(d, sock);
		d.registerSelection(sock, rwh, rwh.getCurrentOps());
		
		// Register timeout
		tt.registerTimeout(sock);
	}
	
	@Override
	public int getCurrentOps() {
		return currentOps;
	}
	
	@Override
	public void handleException() {
		System.err.println("Exception in Accept Handler.");
	}

}

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;


public class HTTPReadWriteHandlerFactory implements ReadWriteHandlerFactory {

	public String documentRoot, serverName;
	public Map<String, ByteBuffer> cache;

	public HTTPReadWriteHandlerFactory(String droot, String sName,
			Map<String, ByteBuffer> cache) {
		this.documentRoot = droot;
		this.serverName = sName;
		this.cache = cache;
	}
	
	@Override
	public HTTPReadWriteHandler build(Dispatcher d, SocketChannel sock) {
		return new HTTPReadWriteHandler(d, sock, documentRoot, serverName,
				cache);
	}

}

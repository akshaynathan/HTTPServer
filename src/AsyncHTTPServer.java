import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;

public class AsyncHTTPServer {

	public static void main(String args[]) {
		Map<String, String[]> config = null;
		int port;
		String documentRoot;
		int cacheSize;
		long timeout = 3000;

		try {
			config = ServerUtil.readConfiguration(args);
			try {
				// Required Arguments
				port = Integer.parseInt(config.get("Listen")[0]);
				documentRoot = config.get("DocumentRoot")[0];
				cacheSize = Integer.parseInt(config.get("CacheSize")[0]);
			} catch (NullPointerException e) {
				throw new InvalidConfigException();
			}
			// Optional Arguments
			String tmp[] = config.get("IncompleteTimeout");
			timeout = (tmp == null) ? timeout : Long.parseLong(tmp[0]);
		} catch (InvalidConfigException e) {
			System.err.println("Invalid Configuration or no config file.");
			return;
		}

		try {
			Dispatcher d = new Dispatcher();
			TimeoutThread t = new TimeoutThread(d, timeout);
			Map<String, ByteBuffer> cache = new CacheMap<String, ByteBuffer>(
					cacheSize);

			ServerSocketChannel server = ServerSocketChannel.open();
			server.socket().bind(new InetSocketAddress(port));
			server.configureBlocking(false);
			HTTPReadWriteHandlerFactory s = new HTTPReadWriteHandlerFactory(
					documentRoot, "AsyncHTTPServer", cache);
			AcceptHandler a = new HTTPAcceptHandler(server, d, s, t);
			d.registerSelection(server, a, SelectionKey.OP_ACCEPT);
			System.out.println("Listening at: " + port);
			
			new Thread(d).start();
			t.start();
			
			int cores = Runtime.getRuntime().availableProcessors();
			System.err.println(cores);
			for(int i = 0; i < cores; i++) {
				new Thread(new WorkerThread(d)).start();
			}
		} catch (IOException e) {
			System.err.println("Server initialization failed.");
			e.printStackTrace();
			return;
		}

	}
}
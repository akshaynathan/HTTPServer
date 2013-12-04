import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class SimpleThreadedHTTPServer {

	public static void main(String[] args) {

		Map<String, String[]> config = null;
		int port, cacheSize;
		String documentRoot;
		
		try {
			try {
				config = ServerUtil.readConfiguration(args);
				port = Integer.parseInt(config.get("Listen")[0]);
				documentRoot = config.get("DocumentRoot")[0];
				cacheSize = Integer.parseInt(config.get("CacheSize")[0]);
			} catch (NullPointerException e) {
				throw new InvalidConfigException();
			}
		} catch (InvalidConfigException e) {
			System.err.println("Invalid Configuration or no config file.");
			return;
		}

		try {
			ServerSocket socket = new ServerSocket(port);
			System.out.println("Listening at: " + port);
			while (true) {
				Socket connection = socket.accept();
				Map<String, byte[]> cache = new CacheMap<String, byte[]>(cacheSize);
				System.err.println("Request recieved from: "
						+ connection.getInetAddress().toString());
				SyncHttpRequestHandler rh = new SyncHttpRequestHandler(
						connection, documentRoot, "SimpleThreadedHTTPServer", cache);

				Thread t = new Thread(rh);
				t.start();

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

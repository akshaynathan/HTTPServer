import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

public class SharedSocketTPHTTPServer {

	public static void main(String[] args) {
		Map<String, String[]> config = null;
		int port;
		String documentRoot;
		int threadCount, cacheSize;

		try {
			try {
				config = ServerUtil.readConfiguration(args);
				port = Integer.parseInt(config.get("Listen")[0]);
				documentRoot = config.get("DocumentRoot")[0];
				threadCount = Integer.parseInt(config.get("ThreadPoolSize")[0]);
				cacheSize = Integer.parseInt(config.get("CacheSize")[0]);
			} catch (NullPointerException e) {
				throw new InvalidConfigException();
			}
		} catch (InvalidConfigException e) {
			System.err.println("Invalid Configuration or no config file.");
			return;
		}

		try {
			ServerSocket sock = new ServerSocket(port);
			Map<String, byte[]> cache = new CacheMap<String, byte[]>(cacheSize);
			System.out.println("Listening at: " + port);

			SharedSocketThread[] pool = new SharedSocketThread[threadCount];

			for (int i = 0; i < pool.length; i++) {
				pool[i] = new SharedSocketThread(sock, documentRoot,
						"SharedSocketTPHTTPServer", cache);
				pool[i].start();
			}
			for (int i = 0; i < pool.length; i++) {
				pool[i].join();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

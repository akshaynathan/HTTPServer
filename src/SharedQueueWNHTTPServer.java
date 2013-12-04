import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class SharedQueueWNHTTPServer {
  
	public static void main(String args[]) {
		
		Map<String, String[]> config = null;
		int port;
		String documentRoot;
		int threadCount, cacheSize;
		Queue<Socket> pool;

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
			pool = new LinkedList<Socket>();
			System.out.println("Listening at: " + port);
			SharedQueueWNThread[] threads = new SharedQueueWNThread[threadCount];
			for(int i = 0; i < threads.length; i++) {
				threads[i] = new SharedQueueWNThread(pool, documentRoot, "SharedQueueHTTPServer", cache);
				threads[i].start();
			}
			while(true) {
				Socket conn = sock.accept();
				System.out.println("Request recieved from: "
						+ conn.getInetAddress().toString());
				synchronized (pool) {
					pool.add(conn);
					pool.notifyAll();
				}
			}
 		} catch (IOException e) {
 			e.printStackTrace();
 		}
	}
} 
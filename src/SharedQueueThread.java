import java.net.Socket;
import java.util.Map;
import java.util.Queue;


public class SharedQueueThread extends Thread {

	private Queue<Socket> pool;
	private String documentRoot;
	private Map<String, byte[]> cache;
	private String serverName;
	
	public SharedQueueThread(Queue<Socket> pool, String documentRoot,
			String serverName, Map<String, byte[]> cache) {
		this.pool = pool;
		this.documentRoot = documentRoot;
		this.cache = cache;
		this.serverName = serverName;
	}

	public void run() {
		while(true) {
			Socket s = null;
			while(s == null) {
				synchronized (pool) {
					if(!pool.isEmpty())
						s = pool.remove();
				}
			}
			new SyncHttpRequestHandler(s, documentRoot, serverName, cache).run();
		}
	}

}

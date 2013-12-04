import java.net.Socket;
import java.util.Map;
import java.util.Queue;


public class SharedQueueWNThread extends Thread {

	private Queue<Socket> pool;
	private String documentRoot;
	private Map<String, byte[]> cache;
	private String serverName;
	
	public SharedQueueWNThread(Queue<Socket> pool, String documentRoot,
			String serverName, Map<String, byte[]> cache) {
		this.pool = pool;
		this.documentRoot = documentRoot;
		this.cache = cache;
		this.serverName = serverName;
	}

	public void run() {
		while(true) {
			Socket s = null;
				synchronized (pool) {
					while(pool.isEmpty()) {
						try {
							pool.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					s = pool.remove();
				}
			new SyncHttpRequestHandler(s, documentRoot, serverName, cache).run();
		}
	}

}

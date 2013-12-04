import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class SharedSocketThread extends Thread {

	ServerSocket sock;
	String documentRoot, serverName;
	Map<String, byte[]> cache;

	public SharedSocketThread(ServerSocket sock, String documentRoot,
			String serverName, Map<String, byte[]> cache) {
		this.sock = sock;
		this.documentRoot = documentRoot;
		this.serverName = serverName;
		this.cache = cache;
	}

	public void run() {
		try {
			while (true) {
				Socket s;
				synchronized (sock) {
					s = sock.accept();
					System.out.println("Request recieved from: "
							+ s.getInetAddress().toString());
				}
				new SyncHttpRequestHandler(s, documentRoot, serverName, cache).run();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

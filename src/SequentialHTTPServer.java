import java.io.*;
import java.net.*;
import java.util.*;

public class SequentialHTTPServer {

	public static void main(String args[]) {

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
			ServerSocket sock = new ServerSocket(port);
			Map<String, byte[]> cache = new CacheMap<String, byte[]>(cacheSize);
			System.out.println("Listening at port: " + port);

			while (true) {
				Socket connection = sock.accept();
				SyncHttpRequestHandler sq = new SyncHttpRequestHandler(connection, documentRoot, "SequentialHTTPServer", cache);
				System.out.println("Request recieved from: "
						+ connection.getInetAddress());
				sq.run();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
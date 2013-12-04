import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class HTTPReadWriteHandler implements ReadWriteHandler {

	private State state;
	private int currentOps;

	private Dispatcher dispatch;
	private SocketChannel client;

	String documentRoot;
	String serverName;

	Map<String, ByteBuffer> cache;

	ByteBuffer in, out; // Hold request/response
	StringBuffer ln; // Read lines of header
	HTTPRequestV2 req;
	
	private boolean isCGI;
	private String pathInfo;
	private String queryString;
	
	private String fPath;
	private ByteBuffer mapped;
	private FileInputStream inputStream;
	private FileChannel f;
	private long fileSize, pos;
	
	private boolean inCache;
	private boolean sendFile;
	
	private SimpleDateFormat s;

	public HTTPReadWriteHandler(Dispatcher d, SocketChannel sock,
			String documentRoot, String serverName,
			Map<String, ByteBuffer> cache) {
		currentOps = SelectionKey.OP_READ;
		this.client = sock;
		this.dispatch = d;
		this.documentRoot = documentRoot;
		this.serverName = serverName;
		this.cache = cache;

		in = ByteBuffer.allocate(4096);
		out = ByteBuffer.allocate(4096);
		ln = new StringBuffer();
		req = new HTTPRequestV2();
		
		state = State.READING_REQUEST;
		
		s = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z"); // date format

		inCache = false;
		pos = 0;
		fileSize = 0;
	}

	public void handleRead(SelectionKey key) throws IOException {
		int read = client.read(in);
		if (read != -1) { // We actually read something
			in.flip();
			while (state == State.READING_REQUEST && in.hasRemaining()
					&& ln.length() < 8096) {
				char c = (char) in.get();
				ln.append(c);
				if (c == '\n' && ln.charAt(ln.length() - 2) == '\r') {
					// We've got a line from the header
					int k = req.append(ln.toString());
					ln.setLength(0);
					if (k == 0 && req.isComplete()) {
						state = State.PREPARING_RESPONSE;
						break;
					}
				}
			}
		}
		in.clear();
		if (state == State.PREPARING_RESPONSE)
			generateResponse();
	}
	
	public void handleWrite(SelectionKey key) throws IOException {
		client.write(out); // Write the headers to the stream
		if (state == State.WRITING_RESPONSE && out.remaining() == 0) {
			// We are done writing the headers
			if (sendFile && (mapped == null)) {
				// Send the file via direct transfer
				long transferred = f.transferTo(pos, fileSize, client);
				pos += transferred;
			} else if (mapped != null){
				// Send the file (in mapped from either filesystem or from
				// cache)
				client.write(mapped);
			}
		}
		if (out.remaining() == 0
				&& ((mapped == null && pos == fileSize) || (mapped != null && mapped
						.remaining() == 0))) {
			// We are done transferring the file!
			assert (pos == fileSize) || (mapped.position() == mapped.limit()) : "File not sent.";
			// Must reset position if from cache
			if (inCache) {
				mapped.position(0);
			}
			if (sendFile) {
				cache();
				f.close();
				inputStream.close();
			}
			state = State.CLOSED;
		}
	}

	public void handleException() {
		System.err.println("Exception in read/write handler.");
		dispatch.getKeyForChannel(client).cancel();
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getCurrentOps() {
		return currentOps;
	}

	public State state() {
		return state;
	}

	private void generateResponse() throws IOException,
			InvalidCGIResponseException {
		try {
			getFile();
			processFile();
		} catch (FileNotFoundException e) {
			try {
				writeResponse(404, "Not Found");
				writeHeaderField("", "");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
		}
		// Done generating the header
		state = State.WRITING_RESPONSE;
		out.flip();
		currentOps = SelectionKey.OP_WRITE;
	}
	
	private void getFile() throws IOException {
		String[] parts = req.filePath.split("/");
		String possible = documentRoot;
		File f;
		int i = 0;
		
		// find the first actual file
		while (!(f = new File(possible)).isFile() && i < parts.length)
			possible += "/" + parts[i++];

		if (f.isDirectory()) {
			// We have to resolve to index
			if (isMobile()) // mobile)
				f = new File(f.getAbsolutePath() + "/" + "m_index.html");
			else
				f = new File(f.getAbsolutePath() + "/" + "index.html");
		}

		if (!f.exists() || !(f.canRead() || f.canExecute()))
			throw new FileNotFoundException();
		
		// possible now is /../../..File
		fPath = f.getAbsolutePath();
		
		if (f.canExecute()) {
			isCGI = true;
			for (; i < parts.length; i++)
				pathInfo += "/" + parts[i];
			queryString = parts[i - 1].split("\\?").length > 1 ? parts[i - 1]
					.split("\\?")[1] : "";
		} else {
			if (i != parts.length)
				throw new FileNotFoundException();
			if ((this.mapped = cache.get(fPath)) == null) {
				this.inputStream = new FileInputStream(fPath);
				this.f = inputStream.getChannel();
				this.fileSize = this.f.size();
				if(this.fileSize > 50000) {	// We're only going to map large files, we can direct transfer everything else
					this.mapped = this.f.map(FileChannel.MapMode.READ_ONLY, 0,
							this.fileSize);
				}
				inCache = false;
			} else {
				this.mapped = this.mapped.duplicate(); // we don't want to use the buffer from the cache, or requests will clash on the buffer
				inCache = true;
			}
		}
	}

	private void processFile() throws IOException {
		if (isCGI) {
			processCGI();
			sendFile = false;
		} else {
			sendFile = true;
			// Check the date
			if (!hasBeenUpdated()) {
				writeResponse(304, "Not Modified");
				writeHeaderField("Date", s.format(new Date())); // RFC 1123
				writeHeaderField("Server", serverName);
				writeHeaderField("Connection", "close");
				writeHeaderField("Last-Modified",
						s.format(new Date(new File(fPath).lastModified())));
				writeHeaderField("", "");
				sendFile = false;
				return;
			}
			// In cache
			if (inCache) {
				writeResponse(200, "OK");
				writeHeaderField("Date", s.format(new Date())); // RFC 1123 time
				writeHeaderField("Server", serverName);
				writeHeaderField("Connection", "close");
				writeHeaderField("Last-Modified",
						s.format(new Date(new File(fPath).lastModified())));
				writeHeaderField("Content-Length", mapped.limit() + "");
				writeHeaderField("", "");
				sendFile = false;
				return;
			}

			// Other wise we're sending the file normally
			writeResponse(200, "OK");
			writeHeaderField("Date", s.format(new Date())); // RFC 1123 time
			writeHeaderField("Server", serverName);
			writeHeaderField("Connection", "close");
			writeHeaderField("Last-Modified",
					s.format(new Date(new File(fPath).lastModified())));
			writeHeaderField("Content-Length", f.size() + "");
			writeHeaderField("", "");
			return;
		}
	}
	
	private void processCGI() throws IOException {
		ProcessBuilder p = new ProcessBuilder(fPath);
		p.redirectErrorStream(true);
		Map<String, String> env = p.environment();
		env.put("GATEWAY_INTERFACE", "CGI/1.1");
		env.put("REMOTE_ADDR", client.socket().getInetAddress().toString());
		env.put("QUERY_STRING", queryString);
		env.put("REQUEST_METHOD", "GET");
		env.put("SERVER_NAME", serverName);
		env.put("SERVER_PORT", client.socket().getPort() + "");
		env.put("SERVER_PROTOCOL", "HTTP");
		env.put("PATH_INFO", pathInfo);
		Iterator<String> it = req.header.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String val = req.header.get(key);
			env.put("HTTP_" + key.replace('-', '_').toUpperCase(), val);
		}
		Process proc = p.start();
		DataInputStream ds = new DataInputStream(proc.getInputStream());

		// The CGI file MUST output at least the content type header
		int c;
		String ln = "";
		while ((c = ds.read()) != '\n') {
			ln += (char) c;
		}
		if (!ln.startsWith("HTTP")) // Is it the correct response?
			writeResponse(200, "OK");
		else
			out.put((ln + "\r\n").getBytes("US-ASCII"));
		while (true) { // Read the rest of the headers
			ln = "";
			while ((c = ds.read()) != '\n') {
				if (c == -1)
					throw new InvalidCGIResponseException();
				ln += (char) c;
			}
			if (ln.equals("")) // end of header
				break;
			out.put((ln + "\r\n").getBytes("US-ASCII"));
		}
		writeHeaderField("", ""); // writes \r\n
		int count;
		byte[] buffer = new byte[8192];
		while ((count = ds.read(buffer)) != -1) {
			out.put(buffer, 0, count);
		}
	}

	private boolean isMobile() {
		String ua = "";
		if ((ua = req.header.get("user-agent")) != null) {
			if (ua.contains("iPhone"))
				return true;
		}
		return false;
	}

	private void cache() throws IOException {
		synchronized (cache) {
			cache.put(
					fPath,
					(mapped != null) ? (ByteBuffer)mapped.position(0) : f.map(
							FileChannel.MapMode.READ_ONLY, 0, this.fileSize));
		}
	}
	
	private boolean hasBeenUpdated() {
		String date;
		if ((date = req.header.get("if-modified-since")) != null) {
			try {
				Date dt = s.parse(date);
				if (!dt.after(new Date())) {
					if (dt.after(new Date(new File(fPath).lastModified()))
							|| dt.equals(new Date(new File(fPath)
									.lastModified()))) {
						return false;
					}
				}
			} catch (ParseException e) {
			}
		}
		return true;
	}
	
	private void writeResponse(int code, String message)
			throws UnsupportedEncodingException {
		out.put(("HTTP/1.0 " + code + " " + message + "\r\n")
				.getBytes("US-ASCII"));
	}

	private void writeHeaderField(String arg, String val)
			throws UnsupportedEncodingException {
		if (arg.equals(""))
			out.put("\r\n".getBytes("US-ASCII"));
		else
			out.put((arg + ": " + val + "\r\n").getBytes("US-ASCII"));
	}
}

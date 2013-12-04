
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

public class SyncHttpRequestHandler implements Runnable {

	private String documentRoot = null;
	private DataOutputStream out = null;
	private String serverType = null;
	private final SimpleDateFormat s;
	private Socket connection;
	private Map<String, byte[]> cache;

	public SyncHttpRequestHandler(Socket connection,
									String documentRoot, 
									String serverType, Map<String, byte[]> cache) {
		this.documentRoot = documentRoot;
		this.serverType = serverType;
		this.connection = connection;
		this.cache = cache;
		s = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z"); // date format
	}

	public void run() {
		try {
			out = new DataOutputStream(connection.getOutputStream());
			try {
				HTTPRequest req = new HTTPRequest(connection, documentRoot);
				if (req.cgi) {
					sendExecutableOutput(req);
					// Heartbeat monitor is implemented as another executable called load, can be switched in and out at runtime.
				} else {
					String d = "";
					if ((d = req.header.get("if-modified-since")) != null) {
						try {
							Date dt = s.parse(d);
							if (!dt.after(new Date())) {
								if (dt.after(new Date(req.requestFile
										.lastModified()))
										|| dt.equals(new Date(req.requestFile
												.lastModified()))) {
									sendResponse(304, "Not Modified");
									sendCommonHeaderFields(req);
									return;
								}
							}
						} catch (ParseException e) {
							System.err
									.println("Invalid date in request, ignoring.");
						}
					}
					sendResponse(200, "OK");
					sendCommonHeaderFields(req);
					sendHeaderField("Content-Length",
							new Long(getFileLength(req.requestFile)).toString());
					sendHeaderField("Content-Type",
							getContentType(req.requestFile));
					sendFile(req.requestFile);
				}
			} catch (FileNotFoundException e) {
				System.err.println("File not found.");
				sendResponse(404, "Not Found");
			} catch (InvalidRequestException e) {
				e.printStackTrace();
				System.err.println("Invalid Request.");
			} catch (InvalidCGIResponseException e) {
				System.err.println("Invalid CGI Response.");
			} finally {
				connection.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private long getFileLength(File requestFile) {
		//byte[] f = cache.get(requestFile.getAbsolutePath());
		//return f == null ? requestFile.length() : f.length;
		return requestFile.length();
	}

	private void sendExecutableOutput(HTTPRequest req) throws IOException, InvalidCGIResponseException {
		ProcessBuilder p = new ProcessBuilder(req.requestFile.getAbsolutePath());
		p.redirectErrorStream(true);
		Map<String, String> env = p.environment();
		env.put("GATEWAY_INTERFACE", "CGI/1.1");
		env.put("REMOTE_ADDR", req.clientIP.toString());
		env.put("QUERY_STRING", req.queryString);
		env.put("REQUEST_METHOD", "GET");
		env.put("SERVER_NAME", serverType);
		env.put("SERVER_PORT", req.serverPort + "");
		env.put("SERVER_PROTOCOL", "HTTP");
		env.put("PATH_INFO", req.pathInfo);
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
			sendResponse(200, "OK");
		else
			out.writeBytes(ln + "\r\n");
		while (true) { // Read the rest of the headers
			ln = "";
			while ((c = ds.read()) != '\n') {
				if(c == -1)
					throw new InvalidCGIResponseException();
				ln += (char) c;
			}
			if (ln.equals("")) // end of header
				break;
			out.writeBytes(ln + "\r\n");
		}
		out.writeBytes("\r\n");
		int count;
		byte[] buffer = new byte[8192];
		while ((count = ds.read(buffer)) != -1) {
			out.write(buffer, 0, count);
		}
	}

	private String getContentType(File requestFile) {
		if (requestFile.getName().endsWith(".jpg"))
			return "image/jpeg";
		else if (requestFile.getName().endsWith(".gif"))
			return "image/gif";
		else if (requestFile.getName().endsWith(".html")
				|| requestFile.getName().endsWith(".htm"))
			return "text/html";
		else
			return "text/plain";
	}

	private void sendCommonHeaderFields(HTTPRequest req) throws IOException {
		s.setTimeZone(TimeZone.getTimeZone("GMT"));
		sendHeaderField("Date", s.format(new Date())); // RFC 1123 time
		sendHeaderField("Server", serverType);
		sendHeaderField("Connection", "close");
		sendHeaderField("Last-Modified",
				s.format(new Date(req.requestFile.lastModified())));
	}

	private void sendResponse(int i, String message) throws IOException {
		out.writeBytes("HTTP/1.0 " + i + " " + message + "\r\n");
	}

	private void sendHeaderField(String arg, String val) throws IOException {
		out.writeBytes(arg + ": " + val + "\r\n");
	}

	private void sendFile(File f) throws IOException {
		out.writeBytes("\r\n");
		synchronized (cache) {
			byte [] file;
			if((file = cache.get(f.getAbsolutePath())) != null) {
				out.write(file, 0, file.length);
				return;
			}
		}
		byte[] buffer = new byte[8192];
		byte[] file = new byte[(int) f.length()];
		int count;
		DataInputStream fr = new DataInputStream(new FileInputStream(f));
		int off = 0;
		
		while ((count = fr.read(buffer)) != -1) {
			out.write(buffer, 0, count);
			off = addToBuffer(file, off, buffer, count);
		}
		out.flush();
		synchronized (cache) {
			cache.put(f.getAbsolutePath(), trimByteArray(file, off));
		}
		fr.close();
	}
	
	private byte[] trimByteArray(byte[] b, int len) {
		byte[] tmp = new byte[len];
		for(int i = 0; i < len; i++) tmp[i] = b[i];
		return tmp;
	}
	
	private int addToBuffer(byte[] buff, int off, byte[] src, int count) {
		if(off + count >= buff.length) {
			byte[] buff2 = new byte[buff.length * 2];
			for(int i = 0; i < off; i++) buff2[i] = buff[i];
			buff = buff2;
		}
		for(int i = off; i < off + count; i++)
			buff[i] = src[i - off];
		return off + count;
	}
}

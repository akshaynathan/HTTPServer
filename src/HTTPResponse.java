import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class HTTPResponse {
	
	public String responseLine;
	public int status;
	public String message;
	public long time;
	public int bodySize;
	
	public Map<String, String> header;
	
	public HTTPResponse(Socket connection) throws IOException, InvalidResponseException {
		header = new HashMap<String, String>();
		
		String h = readHeader(connection.getInputStream());
		if(h == null)
			throw new InvalidResponseException();
		
		String[] arr = h.split("\\r\\n(?!=[\\s\\t])");
		
		if(arr == null)
			throw new InvalidResponseException();
		
		String responseLine = arr[0];
		time = System.currentTimeMillis();
		
		System.err.println(responseLine);
		String[] a = responseLine.split("\\s");
		if (a.length < 3 || !(a[0].equals("HTTP/1.0") || a[0].equals("HTTP/1.1")))
			throw new InvalidResponseException();
		
		status = Integer.parseInt(a[1]);
		message = a[2];
		
		for(int i = 1; i < arr.length; i++) {
			a = arr[i].split(":", 2);
			if(a.length < 2)
				continue;
			header.put(a[0].toLowerCase(), a[1].trim());
		}
		
		String s;
		if((s = header.get("content-length")) != null) {
			this.bodySize = Integer.parseInt(s);
			byte[] buff = new byte[(int) bodySize];
			int read = 0; int k;
			while((k = connection.getInputStream().read(buff, read, bodySize - read)) != -1 && read < bodySize)
				read += k;
			assert read == bodySize : "File not fully read" + read + " " + bodySize;
		}
	}
	
	private String readHeader(InputStream in) throws IOException {
		int c;
		StringBuffer header = new StringBuffer();
		int i = 0;
		while((c = in.read()) != -1) {
			if(c == '\n') {
				if(i > 3) {
					if(header.charAt(i - 1) == '\r') {
						if(header.charAt(i - 2) == '\n') {
							if(header.charAt(i - 3) == '\r') {
								header.append((char)c);
								return header.toString();
							}
						}
					}
				}
			}
			header.append((char)c);
			i++;
		}
		return null;
	}
}

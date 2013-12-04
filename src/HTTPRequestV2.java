import java.util.HashMap;
import java.util.Map;


public class HTTPRequestV2 {
	
	public String requestType = "";
	public String filePath = "";
	public String httpVersion = "";
	private boolean complete;
	
	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public Map<String, String> header;
	
	public HTTPRequestV2() {
		header = new HashMap<String, String>();
		complete = false;
	}
	
	/*
	 * Appends one line to the request. Returns 0 when header is complete, 1 otherwise.
	 */
	public int append(String line) throws InvalidRequestException {
		if(line.equals("\r\n") || complete) {
			complete = true;
			return 0;
		}
		line = line.trim();
		if(requestType.isEmpty()) // this is the first request line
			return parseRequestLine(line);
		String tmp[] = line.split(":", 2);
		if(tmp.length < 2)
			throw new InvalidRequestException();
		header.put(tmp[0].toLowerCase(), tmp[1].trim());
		return 1;
	}

	private int parseRequestLine(String line) throws InvalidRequestException {
		System.err.println("REQUEST: " + line);
		String[] a = line.split("\\s");
		if (a.length < 3 || !a[0].equals("GET"))
			throw new InvalidRequestException();
		requestType = a[0];
		filePath = a[1];
		httpVersion = a[2];
		if(filePath.charAt(0) != '/')
			throw new InvalidRequestException();
		return 1;
	}
}

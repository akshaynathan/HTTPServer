import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class HTTPRequest {

	public String requestType = "";
	public File requestFile;
	
	public String pathInfo = ""; // For CGI
	public String queryString = "";
	public boolean cgi;
	public InetAddress clientIP;
	public int serverPort;
	
	public Map<String, String> header;
	
	public HTTPRequest(Socket connection, String docRoot) throws IOException {
		BufferedReader bis = new BufferedReader(
				new InputStreamReader(connection.getInputStream()));
		clientIP = connection.getInetAddress();
		serverPort = connection.getLocalPort();
		header = new HashMap<String, String>();
		String requestLine = bis.readLine();
		if(requestLine == null)
			throw new InvalidRequestException();
		System.err.println(requestLine);
		String[] a = requestLine.split("\\s");
		if (a.length < 2 || !a[0].equals("GET"))
			throw new InvalidRequestException();
		requestType = a[0];
		String requestURL = a[1];
		if(requestURL.charAt(0) != '/')
			throw new InvalidRequestException();
		
		while(!(requestLine = bis.readLine()).equals("")) {
			a = requestLine.split(":", 2);
			if(a.length < 2)
				continue;
			header.put(a[0].toLowerCase(), a[1].trim());
		}
		
		String[] parts = requestURL.split("/");
		String possible = docRoot;
		File f; int i = 0;
		while(!(f = new File(possible)).isFile() && i < parts.length)
			possible += "/" + parts[i++];
		
		if(f.isDirectory()) {
			// We have to resolve to index
			if(isMobile()) //mobile)
				f = new File(f.getAbsolutePath() + "/" + "m_index.html");
			else
				f = new File(f.getAbsolutePath() + "/" + "index.html");
		}
		
		if(!f.exists() || !(f.canRead() || f.canExecute()))
			throw new FileNotFoundException();
		// possible now is /../../..File
		requestFile = f;
		if(requestFile.canExecute()) {
			cgi = true;
			for(;i < parts.length; i++)
				pathInfo += "/" + parts[i];
			queryString = parts[i-1].split("\\?").length > 1 ? parts[i-1].split("\\?")[1] : "";
		} else
			if(i != parts.length)
				throw new FileNotFoundException();
	}

	private boolean isMobile() {
		String ua = "";
		if((ua = header.get("user-agent")) != null) {
			if(ua.contains("iPhone"))
				return true;
		}
		return false;
	}
	
}

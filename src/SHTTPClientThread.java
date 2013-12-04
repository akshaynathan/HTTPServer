import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SHTTPClientThread extends Thread {

	AtomicBoolean stop;
	int index;
	ClientStats cs;
	Socket s;
	String files[];
	long filesRead, bytesRead;
	long avgWaitTime;
	String server;
	int port;

	public SHTTPClientThread(String server, int port, String files[],
			int index, ClientStats cs) throws UnknownHostException, IOException {
		stop = new AtomicBoolean();
		stop.set(false);
		this.index = index;
		this.cs = cs;
		this.files = files;
		filesRead = 0;
		bytesRead = 0;
		avgWaitTime = 0;
		this.server = server;
		this.port = port;
	}

	public void run() {
		int i = 0;
		try {
			while (!stop.get()) {
				s = new Socket(server, port);
				String file = files[i % files.length];
				sendRequest(file);
				readResponse();
				i++;
				s.close();
			}
			synchronized (cs) {
				cs.dataRateThroughPut[index] = bytesRead;
				cs.waitTimes[index] = avgWaitTime / filesRead;
				cs.totalThroughPut[index] = filesRead;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void readResponse() throws IOException {
		long stime = System.currentTimeMillis();
		try {
			HTTPResponse r = new HTTPResponse(s);
			bytesRead += (long) r.bodySize;
			filesRead += 1;
			avgWaitTime += r.time - stime;
		} catch (InvalidResponseException e) {
			System.err.println("Invalid Response.");
			e.printStackTrace();
		}
	}

	private void sendRequest(String file) throws IOException {
		s.getOutputStream().write(
				("GET /" + file + " HTTP/1.0\r\n\r\n").getBytes("US-ASCII"));
	}

	public void done() {
		stop.set(true);
	}

}

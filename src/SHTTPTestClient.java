import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

public class SHTTPTestClient {

	public static void main(String args[]) throws InterruptedException {
		int port, threads, time;
		String server, filename;

		try {
			Map<String, String[]> argmap = ServerUtil.readVars(args, "-");
			port = Integer.parseInt(argmap.get("port")[0]);
			threads = Integer.parseInt(argmap.get("parallel")[0]);
			time = Integer.parseInt(argmap.get("T")[0]);
			server = argmap.get("server")[0];
			filename = argmap.get("files")[0];
		} catch (NullPointerException e) {
			System.err
					.println("Usage: java SHTTPTestClient -server <server> -port <server port> -parallel <# of threads> -files <file name> -T <time of test in seconds>");
			return;
		}

		ArrayList<String> arrFiles = new ArrayList<String>();
		Scanner scan;
		try {
			scan = new Scanner(new File(filename));
		} catch (FileNotFoundException e1) {
			System.err.println("File does not exist.");
			return;
		}
		
		while(scan.hasNextLine())
			arrFiles.add(scan.nextLine());

		String files[] = null;
		if(arrFiles.size() == 0) {
			System.err.println("Empty File.");
			return;
		} else {
			files = arrFiles.toArray(new String[0]);
		}
		
		ClientStats cs = new ClientStats(threads, time);
		SHTTPClientThread threadSet[] = new SHTTPClientThread[threads];

		try {
			for (int i = 0; i < threads; i++) {
				threadSet[i] = new SHTTPClientThread(server, port, files, i, cs);
				threadSet[i].start();
			}
		} catch (Exception e) {
			System.err.println("Unknown host or IO error.");
			return;
		}

		Thread.sleep(time * 1000);

		for (int i = 0; i < threads; i++)
			threadSet[i].done();
		for (int i = 0; i < threads; i++)
			threadSet[i].join();

		cs.printStats();
	}
}

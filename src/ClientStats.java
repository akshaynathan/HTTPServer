
public class ClientStats {

	long waitTimes[];
	long dataRateThroughPut[];
	long totalThroughPut[];
	int time;
	
	public ClientStats(int threads, int time) {
		waitTimes = new long[threads];
		dataRateThroughPut = new long[threads];
		totalThroughPut = new long[threads];
		this.time = time;
		for(int i = 0; i < threads; i++) {
			waitTimes[i] = 0;
			dataRateThroughPut[i] = 0;
			totalThroughPut[i] = 0;
		}
	}
	
	public void printStats() {
		long avgWaitTime = 0, dataTP = 0, totalTP = 0;
		for(int i = 0; i < waitTimes.length; i++) {
			avgWaitTime = ((avgWaitTime * (long)i) + waitTimes[i])/(long)(i + 1);
			dataTP += dataRateThroughPut[i];
			totalTP += totalThroughPut[i];
		}
		System.out.println("Average Wait Time: " + avgWaitTime);
		System.out.println("Data Rate Throughput (total bytes recieved/time): " + dataTP/(long)time);
		System.out.println("Total Transaction Throughput (# of downloaded files/time): " + totalTP/(long)time);
	}
}

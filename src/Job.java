import java.nio.channels.SelectionKey;


public class Job {
	SelectionKey k;
	int originalOps;
	
	public Job(SelectionKey k, int ops) {
		this.k = k;
		this.originalOps = ops;
	}
}

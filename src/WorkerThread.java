import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Queue;

public class WorkerThread implements Runnable {

	Dispatcher d;
	private Queue<Job> jobsQueue;

	public WorkerThread(Dispatcher d) {
		this.d = d;
		this.jobsQueue = d.jobsQueue;
	}

	@Override
	public void run() {
		while (true) {
			Job j;
			synchronized (jobsQueue) {
				while (jobsQueue.isEmpty()) {
					try {
						jobsQueue.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				j = jobsQueue.remove();
			}
			try {
				handle(j);
				unpause(j);
			} catch (IOException e) {
				((Handler) j.k.attachment()).handleException();
			}
		}
	}

	private void handle(Job j) throws IOException {
		if (!j.k.isValid())
			return; // This key could have been cancelled already (timed out)
		SelectionKey k = j.k;
		Handler h = (Handler) k.attachment();
		switch (j.originalOps) {
		case SelectionKey.OP_ACCEPT:
			((AcceptHandler) h).handleAccept();
			break;
		case SelectionKey.OP_READ:
			((ReadWriteHandler) h).handleRead(j.k);
			break;
		case SelectionKey.OP_WRITE:
			((ReadWriteHandler) h).handleWrite(j.k);
			break;
		}
	}

	private void unpause(Job j) throws IOException {
		if (j.k.isReadable() || j.k.isWritable()) {
			if (((ReadWriteHandler) j.k.attachment()).state() == ReadWriteHandler.State.CLOSED) {
				j.k.cancel();
				j.k.channel().close();
			}
		}
		d.addTask(new UpdateInterestTask(j.k, ((Handler) j.k.attachment())
				.getCurrentOps()));
	}

	class UpdateInterestTask implements DispatcherTask {

		SelectionKey k;
		int newOps;

		public UpdateInterestTask(SelectionKey k, int newOps) {
			this.k = k;
			this.newOps = newOps;
		}

		public void run(Dispatcher d) {
			synchronized (k) {
				if (k.isValid()) {
					k.interestOps(newOps);
				}
			}
		}

	}
}

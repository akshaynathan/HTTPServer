import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

public class TimeoutThread extends Thread {
	private Dispatcher d;
	private Queue<TimeoutEvent> nextEvents;
	private long timeout;

	public TimeoutThread(Dispatcher d, long timeout) {
		this.d = d;
		this.timeout = timeout;
		nextEvents = new LinkedList<TimeoutEvent>();
	}

	public void registerTimeout(SocketChannel sock) {
		synchronized (nextEvents) {
			nextEvents.add(new TimeoutEvent(System.currentTimeMillis()
					+ timeout, sock));
			nextEvents.notifyAll();
		}
	}

	public void run() {
		while (true) {
			TimeoutEvent t;
			synchronized (nextEvents) {
				while (nextEvents.isEmpty()) {
					try {
						nextEvents.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				t = nextEvents.remove();
			}
			try {
				if (t.time - System.currentTimeMillis() > 0)
					Thread.sleep(t.time - System.currentTimeMillis());
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			SelectionKey key = d.getKeyForChannel(t.sock);
			if (key != null && key.isValid()) { // this key could have already been cancelled
				HTTPReadWriteHandler r = (HTTPReadWriteHandler) key.attachment();
				if (r.state() == HTTPReadWriteHandler.State.READING_REQUEST) {
					d.addTask(new TimeoutTask(key));
				}
			}
		}
	}

	class TimeoutEvent {
		long time;
		SocketChannel sock;

		public TimeoutEvent(long time, SocketChannel sock) {
			this.time = time;
			this.sock = sock;
		}
	}

	class TimeoutTask implements DispatcherTask {

		private SelectionKey key;

		public TimeoutTask(SelectionKey key) {
			this.key = key;
		}

		@Override
		public void run(Dispatcher d) {
			key.cancel();
			try {
				key.channel().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}

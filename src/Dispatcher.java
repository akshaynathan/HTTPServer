import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class Dispatcher implements Runnable {

	public Queue<Job> jobsQueue; // Queue for worker threads
	// Queue's for dispatcher tasks
	private Queue<DispatcherTask> tasksQueue;

	private Selector selector;

	public Dispatcher() throws IOException {
		try {
			selector = Selector.open();
		} catch (IOException e) {
			System.err.println("Unable to open selector.");
			return;
		}
		jobsQueue = new LinkedList<Job>(); // Queue for worker threads

		tasksQueue = new LinkedList<DispatcherTask>();
	}

	public void run() {
		/*
		 * Dispatcher control flow:
		 * 
		 * while true
		 * 
		 * Register all things that need to be registered Deregister all things
		 * that need to be deregistered Update interests on all things that need
		 * to be updated
		 * 
		 * select
		 * 
		 * for each key add the key to the jobs queue
		 */
		while (true) {
			// Process events off the task queues first
			processTasks();

			try {
				selector.selectNow();
			} catch (IOException e) {
				System.err.println("I/O Error in Selector.");
				e.printStackTrace();
				return;
			}

			Iterator<SelectionKey> it = selector.selectedKeys().iterator();

			while (it.hasNext()) {
				SelectionKey key = it.next();
				it.remove();
				synchronized (key) {
					if (key.isValid()) {
						synchronized (jobsQueue) {
							jobsQueue.add(new Job(key, key.interestOps()));
							pause(key);
							jobsQueue.notifyAll();
						}
					}
				}
			}
		}
	}

	public void registerSelection(SelectableChannel channel, Handler h, int ops) {
		synchronized (tasksQueue) {
			tasksQueue.add(new SimpleRegisterTask(channel, h, ops));
		}
	}

	private void pause(SelectionKey key) {
		addTask(new PauseKeyTask(key));
	}

	private void processTasks() {
		synchronized (tasksQueue) {
			while (!tasksQueue.isEmpty())
				tasksQueue.remove().run(this);
		}
	}

	public void addTask(DispatcherTask d) {
		synchronized (tasksQueue) {
			tasksQueue.add(d);
		}
	}

	class SimpleRegisterTask implements DispatcherTask {

		private SelectableChannel channel;
		private Handler handler;
		private int ops;

		public SimpleRegisterTask(SelectableChannel channel, Handler h, int ops) {
			this.channel = channel;
			this.handler = h;
			this.ops = ops;
		}

		@Override
		public void run(Dispatcher d) {
			SelectionKey key;
			try {
				key = channel.register(d.selector, ops);
				key.attach(handler);
			} catch (ClosedChannelException e) {
				e.printStackTrace();
			}
		}
	}

	class PauseKeyTask implements DispatcherTask {

		SelectionKey k;

		public PauseKeyTask(SelectionKey k) {
			this.k = k;
		}

		@Override
		public void run(Dispatcher d) {
			synchronized (k) {
				if (k.isValid()) {// This key may have already been cancelled by
									// timeout thread {
					k.interestOps(0);
				}
			}
		}

	}

	public SelectionKey getKeyForChannel(SocketChannel sock) {
		return sock.keyFor(selector);
	}

}

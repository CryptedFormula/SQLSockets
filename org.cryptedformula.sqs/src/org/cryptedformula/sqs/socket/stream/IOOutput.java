package org.cryptedformula.sqs.socket.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cryptedformula.sqs.socket.Client;

/**
 * @author Giovanni
 */
public class IOOutput extends OutputStream {

	private ByteArrayOutputStream holder = null;
	private Client client = null;
	private boolean stremClosed = false;
	private Boolean isWriteWait = new Boolean(false);

	private Lock writeLock = new ReentrantLock();
	private Condition cond = writeLock.newCondition();

	private static final int MAX_BUFF_SIZE = 1024;

	public IOOutput(Client cli) {
		holder = new ByteArrayOutputStream();
		client = cli;
	}

	@Override
	public void write(int b) throws IOException {
		if (stremClosed) {
			throw new IOException("Write stream closed");
		}
		synchronized (holder) {
			holder.write(b);
			checkAndFlush();
		}

	}

	@Override
	public void write(byte[] arg0) throws IOException {
		if (stremClosed) {
			throw new IOException("Write stream closed");
		}
		synchronized (holder) {
			holder.write(arg0);
			checkAndFlush();
		}

	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (stremClosed) {
			throw new IOException("Write stream closed");
		}
		synchronized (holder) {
			holder.write(b, off, len);
			checkAndFlush();
		}

	}

	@Override
	public void flush() throws IOException {

		if (stremClosed) {
			throw new IOException("Write stream closed");
		}
		try {
			writeLock.lock();
			synchronized (isWriteWait) {
				isWriteWait = true;
			}
			client.triggerWrite();
			while (isWriteWait) {
				cond.await();
			}

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			writeLock.unlock();
		}

	}

	public void notifyWrite() {
		try {
			writeLock.lock();
			synchronized (isWriteWait) {
				isWriteWait = false;
			}
			cond.signal();
		} finally {
			writeLock.unlock();
		}

	}

	protected ByteBuffer getByteBuffer() {
		ByteBuffer buff = null;
		synchronized (holder) {
			buff = ByteBuffer.wrap(holder.toByteArray());
			holder.reset();
		}
		return buff;
	}

	@Override
	public void close() throws IOException {
		if (!stremClosed) {
			stremClosed = true;
		}

	}

	private void checkAndFlush() throws IOException {
		if (holder.size() > MAX_BUFF_SIZE)
			flush();
	}

	public boolean isWriteWait() {
		return isWriteWait;
	}
}

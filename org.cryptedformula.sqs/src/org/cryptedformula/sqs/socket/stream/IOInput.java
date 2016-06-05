package org.cryptedformula.sqs.socket.stream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cryptedformula.sqs.socket.Client;

/**
 * @author Giovanni
 */
public class IOInput extends InputStream {

	private ByteBuffer streamBuffer;
	private ByteBuffer channelBuffer;

	private Client readClient;
	private boolean stremClosed = false;
	private Boolean isReadWait = false;
	private Lock readLock = new ReentrantLock();

	public IOInput(Client client) {
		readClient = client;
		streamBuffer = ByteBuffer.allocate(1024);
		streamBuffer.flip();
		channelBuffer = ByteBuffer.allocate(1024);
	}

	@Override
	public int read() throws IOException {

		byte b[] = new byte[1];
		read(b, 0, b.length);
		return (0xFF & b[0]);

	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {

		try {
			readLock.lock();
			int read_size = 0;
			int read_length = 0;
			read_length = len;
			while (true) {

				if (stremClosed) {
					throw new IOException("Read stream closed");
				}

				if (streamBuffer.remaining() > 0) {
					int toread = Math.min(read_length, streamBuffer.remaining());
					streamBuffer.get(b, off, toread);
					read_size += toread;
					off = read_size;
					if (read_size == len) {
						break;
					}
				} else {
					int available = available();
					if (available > 0) {
						continue;
					} else if (available == 0) {
						// Block the caller
						try {
							if (read_size == 0) {
								synchronized (isReadWait) {
									isReadWait = true;
								}
								synchronized (this) {
									while (isReadWait()) {
										wait();
									}
								}
							} else {
								break;
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						return -1;
					}
				}

			}
			return read_size;
		} finally {
			readLock.unlock();
		}

	}

	@Override
	public synchronized int available() throws IOException {
		while (true) {

			if (stremClosed) {
				throw new IOException("Read stream closed");
			}

			int available = streamBuffer.remaining();
			if (available == 0) {
				streamBuffer.rewind();
				streamBuffer.limit(streamBuffer.capacity());
				// Read it from the stream add it to the stream buffer
				channelBuffer.rewind();
				channelBuffer.limit(channelBuffer.capacity());
				if (readClient.readToBuffer(channelBuffer) < 0) {
					close();
					return -1;
				}
				channelBuffer.flip();
				streamBuffer.put(channelBuffer);
				streamBuffer.flip();
				available = streamBuffer.remaining();
			}

			return available;
		}
	}

	@Override
	public void close() throws IOException {
		if (!stremClosed) {
			stremClosed = true;
			notifyRead();
		}
	}

	public boolean isReadWait() {
		return isReadWait;
	}

	public void notifyRead() {
		synchronized (isReadWait) {
			isReadWait = false;
		}
		synchronized (this) {
			this.notifyAll();
		}

	}

	protected void lock() {
		readLock.lock();
	}

	protected void unlock() {
		readLock.unlock();
	}

	protected boolean tryLock() {
		return readLock.tryLock();
	}
}
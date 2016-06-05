package org.cryptedformula.sqs.socket;

import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import org.cryptedformula.sqs.socket.stream.IOInput;
import org.cryptedformula.sqs.socket.stream.IOOutput;

/**
 * @author Giovanni
 */
public class Client {

	// private Selector selector = null;
	private SocketChannel client = null;
	private Selector selector = null;
	private InetSocketAddress address = null;

	private SSLContext sslContext;
	private SSLEngine sslEngine = null;

	private boolean initConnDone = false;
	private boolean isClosed = false;

	private IOInput socketInputStream;
	private IOOutput socketOutputStream = null;

	public Client(InetSocketAddress address) {
		this.address = address;
		socketInputStream = new IOInput(this);
		socketOutputStream = new IOOutput(this);
	}

	protected Client(SocketChannel client, Selector key) {
		this.client = client;
		initConnDone = true;
		selector = key;
		socketInputStream = new IOInput(this);
		socketOutputStream = new IOOutput(this);
	}

	public void setSSLContext(SSLContext context) {
		this.sslContext = context;
	}

	protected void buildSSLHandler(SSLContext context, boolean clientmode) throws IOException {
		if (context != null && client != null) {
			setSSLContext(context);
			sslEngine = sslContext.createSSLEngine(client.socket().getInetAddress().getHostName(),
					client.socket().getPort());
			sslEngine.setUseClientMode(clientmode);

		}
	}

	public void connect() throws IOException {

		if (initConnDone)
			throw new IOException("Socket Already connected");

		client = SocketChannel.open();
		client.configureBlocking(false);
		client.connect(address);

		new Thread() {

			public void run() {
				try {
					selector = Selector.open();
					client.register(selector, SelectionKey.OP_CONNECT);

					while (!isClosed) {
						int nsel = selector.select();
						if (nsel == 0)
							continue;
						Set<SelectionKey> selectedKeys = selector.selectedKeys();
						Iterator<SelectionKey> it = selectedKeys.iterator();
						while (it.hasNext()) {
							SelectionKey key = it.next();
							it.remove();
							if (!key.isValid())
								continue;
							if (key.isValid() && key.isConnectable()) {
								if (client.finishConnect()) {
									if (sslContext != null) {
										// Do the SSL handshake stuff ;
										buildSSLHandler(sslContext, true);
									}
									client.register(selector, SelectionKey.OP_READ);
									initConnDone = true;

								}
							}
							if (key.isValid() && key.isReadable()) {
								unblockRead();
								if (client.isOpen())
									client.register(selector, SelectionKey.OP_READ);
							}
							if (key.isValid() && key.isWritable()) {
								unblockWrite();
								if (client.isOpen())
									client.register(selector, SelectionKey.OP_READ);
							}
						}
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						selector.close();
						client.close();
						socketInputStream.close();
						socketOutputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

		}.start();

	}

	protected SocketChannel getSocketChannel() {
		return client;
	}

	protected boolean isReadBlocked() {
		return socketInputStream.isReadWait();
	}

	protected void unblockRead() {
		socketInputStream.notifyRead();
	}

	protected boolean isWriteBlocked() {
		return socketOutputStream.isWriteWait();
	}

	protected void unblockWrite() {
		System.out.println("Client.unblockWrite()");
		socketOutputStream.notifyWrite();
	}

	public boolean isConnected() {
		return (initConnDone && client != null && client.isConnected());
	}

	public OutputStream getOutputStream() {
		return socketOutputStream;
	}

	public InputStream getInputStream() {
		return socketInputStream;
	}

	public void close() throws IOException {
		if (!isClosed) {
			isClosed = true;
			client.close();
			socketInputStream.close();
			socketOutputStream.close();
			initConnDone = false;
			if (selector != null) {
				selector.wakeup();
			}
		}
	}

	public void triggerWrite() throws IOException {
		if (client != null && client.isOpen()) {
			System.out.println("Client.triggerWrite()");
			try {
				client.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, this);
				selector.wakeup();
			} catch (ClosedChannelException e) {
				throw new IOException("Connection Closed ");
			}
		}
	}

	public InetSocketAddress getRemoteAddress() {
		if (client != null)
			return (InetSocketAddress) (client.socket().getRemoteSocketAddress());
		return null;
	}

	public InetSocketAddress getLocalAddress() {
		if (client != null)
			return (InetSocketAddress) (client.socket().getLocalSocketAddress());
		return null;
	}

	public int readToBuffer(ByteBuffer buffer) throws IOException {
		int out = 0;
		if (out < 0) {
			close();
		} else {
			client.register(selector, SelectionKey.OP_READ, this);
		}
		return out;
	}

}
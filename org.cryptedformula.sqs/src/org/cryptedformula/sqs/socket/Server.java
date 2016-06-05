package org.cryptedformula.sqs.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;

/**
 * @author Giovanni
 */
public class Server extends Thread {

	private InetSocketAddress listener;

	private SSLContext sslContext;

	private ExecutorService executer;

	private Selector selector;

	private ServerSocketChannel server;

	public Server(InetSocketAddress sockAdd) {
		this.listener = sockAdd;
	}

	public void setSSLContext(SSLContext context) {
		sslContext = context;
	}

	@Override
	public synchronized void start() {
		if (executer == null) {
			executer = Executors.newSingleThreadExecutor();
		}
		super.start();
	}

	@Override
	public void run() {

		try {
			server = ServerSocketChannel.open();
			server.configureBlocking(false);
			ServerSocket socket = server.socket();
			socket.bind(listener);
			selector = Selector.open();
			server.register(selector, server.validOps());
		} catch (ClosedChannelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}

		while (true) {
			try {
				selector.select();

				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> ite = keys.iterator();
				while (ite.hasNext()) {

					SelectionKey key = ite.next();
					System.out.println(key);
					ite.remove();

					if (!key.isValid())
						continue;

					if (key.isValid() && key.isAcceptable()) {
						System.out.println(new Date() + " Selector Accept ");
						SocketChannel channel = server.accept();
						channel.configureBlocking(false);
						Client sc = newClient(channel, selector);
						channel.finishConnect();
						if (sc != null) {
							if (sslContext != null) {
								// sc.setSSLContext(sslContext);
								sc.buildSSLHandler(sslContext, false);
							}
							channel.register(selector, SelectionKey.OP_READ, sc);
						}
						continue;
					}

					if (key.isValid() && key.isReadable()) {

						System.out.println(new Date() + " Selector Read " + key.attachment());
						key.interestOps(0);
						continue;
					}

					if (key.isValid() && key.isWritable()) {
						System.out.println(new Date() + " Selector Write ");
						key.interestOps(0);
						handleWrite(key);
						continue;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public void setExecuterService(ExecutorService executer) {
		this.executer = executer;
	}

	protected Client newClient(SocketChannel sc, Selector key) {
		return new Client(sc, key);
	}

	private void handleWrite(SelectionKey key) throws IOException {
		Client sc = (Client) key.attachment();
		if (sc.isWriteBlocked()) {

			sc.unblockWrite();
			if (sc.isConnected())
				key.interestOps(SelectionKey.OP_READ);

		}

	}
}
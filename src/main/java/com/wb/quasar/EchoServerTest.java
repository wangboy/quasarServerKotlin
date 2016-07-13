package com.wb.quasar;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.io.FiberServerSocketChannel;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import co.paralleluniverse.strands.SuspendableRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Created by wangbo on 16/6/23.
 */
public class EchoServerTest {

	static final Logger LOGGER = LoggerFactory.getLogger(TestMain.class);
	static final int PORT = 1234;

	public static void doAll() {
		new Fiber((SuspendableRunnable) () -> {
			try {
				FiberServerSocketChannel socket = FiberServerSocketChannel.open().bind(new InetSocketAddress(PORT));
				System.out.println("started");
				for (; ; ) {
					FiberSocketChannel ch = socket.accept();
					new Fiber((SuspendableRunnable) () -> {
						try {
							ByteBuffer buf = ByteBuffer.allocateDirect(1024);
							while (true) {
								buf.clear();
								ch.read(buf);
								buf.flip();
								ch.write(buf);
							}
						} catch (IOException e) {
							LOGGER.error("client fiber failed", e);
						}
					}).start();
				}
			} catch (IOException e) {
				LOGGER.error("main fiber failed", e);
			}
		}).start();
		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

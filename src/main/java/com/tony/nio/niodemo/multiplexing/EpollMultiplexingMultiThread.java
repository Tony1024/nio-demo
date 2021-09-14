package com.tony.nio.niodemo.multiplexing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实现多selector多线程模式，连接/读写事件混杂在一起，selector同时干这两个事
 *
 * @author gaoweidong
 * @date 2021/9/14
 */
public class EpollMultiplexingMultiThread {

    private NioEventLoop[] nioEventLoops = null;

    private final Integer selectorsNum;

    private final AtomicInteger count = new AtomicInteger(1);

    public void doBind(int port) {
        NioEventLoop next = next();
        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            next.register(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doAccept(SelectionKey key) {
        NioEventLoop next = next();
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            //choose a selector and register
            next.register(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private NioEventLoop next() {
        return nioEventLoops[count.getAndIncrement() % selectorsNum];
    }

    public EpollMultiplexingMultiThread(Integer selectorsNum) {
        this.selectorsNum = selectorsNum;
        if (selectorsNum <= 0) {
            throw new IllegalArgumentException("selectorsNum需要大于0");
        }
        nioEventLoops = new NioEventLoop[selectorsNum];
        for (int i = 0; i < this.selectorsNum; i++) {
            nioEventLoops[i] = new NioEventLoop(this);
            Thread thread = new Thread(nioEventLoops[i]);
            thread.start();
        }
    }

    public void readHandler(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            while (true) {
                read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (read == 0) {
                    break;
                } else {
                    client.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        EpollMultiplexingMultiThread service = new EpollMultiplexingMultiThread(2);
        service.doBind(9090);
        service.doBind(9091);
        service.doBind(9092);
        service.doBind(9093);
    }

    public static class NioEventLoop extends ThreadLocal<LinkedBlockingQueue<Channel>> implements Runnable {

        private Selector selector;

        private EpollMultiplexingMultiThread main;

        private LinkedBlockingQueue<Channel> taskQueue = get();

        public NioEventLoop(EpollMultiplexingMultiThread main) {
            this.main = main;
            try {
                selector = Selector.open();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    int num = selector.select();
                    if (num > 0) {
                        // 返回的有状态的fd集合
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iter = selectionKeys.iterator();
                        while (iter.hasNext()) {
                            SelectionKey key = iter.next();
                            // 不移除会重复循环处理
                            iter.remove();
                            if (key.isAcceptable()) {
                                main.doAccept(key);
                            } else if (key.isReadable()) {
                                main.readHandler(key);
                            }
                        }
                    }
                    // execute tasks
                    if (!taskQueue.isEmpty()) {
                        try {
                            Channel channel = taskQueue.take();
                            if (channel instanceof ServerSocketChannel) {
                                // 监听连接事件
                                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) channel;
                                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                                System.out.println("Thread id: " + Thread.currentThread().getId() + " register accept event success");
                            } else if (channel instanceof SocketChannel) {
                                // 监听读事件
                                SocketChannel socketChannel = (SocketChannel) channel;
                                ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
                                socketChannel.register(selector, SelectionKey.OP_READ, buffer);
                                System.out.println("Thread id: " + Thread.currentThread().getId() + " register read event success");
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        protected LinkedBlockingQueue<Channel> initialValue() {
            return new LinkedBlockingQueue<>(1024);
        }

        public void register(Channel channel) {
            try {
                taskQueue.put(channel);
                selector.wakeup();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}

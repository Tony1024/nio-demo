package com.tony.nio.niodemo.multiplexing.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author gaoweidong
 * @date 2021/9/13
 */
public class NioEventLoop extends ThreadLocal<LinkedBlockingQueue<Channel>> implements Runnable {

    Selector selector = null;

    LinkedBlockingQueue<Channel> taskQueue = get();

    NioEventLoopGroup group;

    NioEventLoop(NioEventLoopGroup group) {
        try {
            this.group = group;
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // Loop
        while (true) {
            try {
                // 1.select()
                // 阻塞, selector.wakeup()可以让select立马返回
                int nums = selector.select();
                // 2.处理selectedKeys
                if (nums > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = keys.iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        // 复杂,接受客户端的过程（接收之后，要注册，多线程下，新的客户端，注册到那里呢？）
                        if (key.isAcceptable()) {
                            doAccept(key);
                        } else if (key.isReadable()) {
                            readHandler(key);
                        }
                    }
                }
                // 3,处理task
                if (!taskQueue.isEmpty()) {
                    //只有方法的逻辑，本地变量是线程隔离的
                    Channel c = taskQueue.take();
                    if (c instanceof ServerSocketChannel) {
                        ServerSocketChannel server = (ServerSocketChannel) c;
                        server.register(selector, SelectionKey.OP_ACCEPT);
                        System.out.println(Thread.currentThread().getName() + " register listen");
                    } else if (c instanceof SocketChannel) {
                        SocketChannel client = (SocketChannel) c;
                        ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
                        client.register(selector, SelectionKey.OP_READ, buffer);
                        System.out.println(Thread.currentThread().getName() + " register client: " + client.getRemoteAddress());
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void readHandler(SelectionKey key) {
        System.out.println(Thread.currentThread().getName() + " read......");
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        SocketChannel client = (SocketChannel) key.channel();
        buffer.clear();
        while (true) {
            try {
                int num = client.read(buffer);
                if (num > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (num == 0) {
                    break;
                } else {
                    //客户端断开了
                    System.out.println("client: " + client.getRemoteAddress() + " closed......");
                    key.cancel();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void doAccept(SelectionKey key) {
        System.out.println(Thread.currentThread().getName() + " :accept");
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            // choose a selector and register
            group.registerSelector(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setWorker(NioEventLoopGroup worker) {
        this.group = worker;
    }

    @Override
    protected LinkedBlockingQueue<Channel> initialValue() {
        return new LinkedBlockingQueue<>();
    }

}

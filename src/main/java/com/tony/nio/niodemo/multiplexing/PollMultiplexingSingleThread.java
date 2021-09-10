package com.tony.nio.niodemo.multiplexing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 多路复用器demo
 */
public class PollMultiplexingSingleThread {

    private ServerSocketChannel server = null;
    // linux 多路复用器（select poll epoll kqueue） nginx  event{}
    private Selector selector = null;
    int port = 9090;

    public void initServer() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            // 此时server处于listen状态，listen状态的文件描述符为fd4
            // select poll下 不涉及系统调用，但epoll会调动epoll_create
            selector = Selector.open();
            /**
             * select，poll：jvm里开辟一个数组 fd4 放进去 [但不涉及系统调用]
             */
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        System.out.println("服务器启动了");
        try {
            while (true) {
                Set<SelectionKey> keys = selector.keys();
                System.out.println("key size:" + keys.size());
                /**
                 * select/poll的函数定义
                 * select(int nfds, fd_set *readfds, fd_set *writefds,
                 *                   fd_set *exceptfds, struct timeval *timeout)
                 * poll(struct pollfd *fds, nfds_t nfds, int timeout)
                 * selector.select()方法做了什么事情？
                 * 调用上述的系统函数
                 */
                while (selector.select() > 0) {
                    // 返回的有状态的fd集合
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = selectionKeys.iterator();
                    // 然而，最终还是得针对这些就绪的fd发起系统调用
                    // 尽管是多路复用器，还得一个一个的去处理它们的R/W (没办法，这就是同步)
                    // 但是对比之前NIO，NIO是需要对每一个fd发起系统调用，浪费资源，
                    // 而select/epoll这里是将所有的fd传递过去内核，让内核去轮询每一个fd是否准备就绪，然后返回
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        // 不移除会重复循环处理
                        iter.remove();
                        if (key.isAcceptable()) {
                            // 思考，accept接受连接且会返回新连接的fd对吧？那新的FD怎么办？
                            // 答案：当然是使用epoll_ctl把新的客户端fd注册到内核空间
                            // select/poll：因为这两种方式在内核都没有像epoll那样开辟了一个空间，那么在jvm中保存和前边的fd4那个listen的一起
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            // 处理读写事件
                            readHandler(key);
                            // 思考：这个处理过程，是在当前线程进行的，假设这个方法阻塞了，会存在什么问题？
                            // 答：IO被阻塞了，就算有新的读写事件来的，也因为你的这个阻塞导致大批量io事件阻塞
                            // 因此，也就提出了 IO THREAD模型
                            // 所以，为什么提出IO THREADS... 再到Netty的IO线程模型
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            // 调用accept接受客户端  fd7
            SocketChannel client = ssc.accept();
            client.configureBlocking(false);

            ByteBuffer buffer = ByteBuffer.allocate(8192);
            /**
             * select，poll：jvm里开辟一个数组 fd7 放进去
             */
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
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
        PollMultiplexingSingleThread service = new PollMultiplexingSingleThread();
        service.start();
    }
}

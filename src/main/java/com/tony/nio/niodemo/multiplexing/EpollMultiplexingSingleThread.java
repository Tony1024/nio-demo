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
 * epoll多路复用器演示程序
 */
public class EpollMultiplexingSingleThread {

    private ServerSocketChannel server = null;
    private Selector selector = null;
    int port = 9090;

    public void initServer() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            // 在epoll模型下，open意味着：epoll_create -> fd3
            selector = Selector.open();
            // 此时server处于listen状态，listen状态的文件描述符为fd4

            /*
            register
            epoll_ctl(int epfd, int op, int fd, struct epoll_event *event);
            epoll_ctl(fd3,EPOLL_CTL_ADD,fd4,EPOLLIN)
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
            //死循环
            while (true) {
                Set<SelectionKey> keys = selector.keys();
                System.out.println("key size:" + keys.size());
                /**
                 * selector.select()方法做了什么事情？
                 * 调用了epoll_wait() :
                 * When successful,epoll_wait() returns the number of file descriptors ready for the requested I/O
                 */
                while (selector.select() > 0) {
                    // 返回的有状态的fd集合
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = selectionKeys.iterator();
                    // 然而，最终还是得针对这些就绪的fd发起系统调用
                    // 尽管是多路复用器，还得一个一个的去处理它们的R/W (没办法，这就是同步)
                    // 但是对比之前NIO，NIO是需要对每一个fd发起系统调用，浪费资源，然而epoll这里仅仅调用了一次select()方法，就知道具体哪些fd可R/W了
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        // 不移除会重复循环处理
                        iter.remove();
                        if (key.isAcceptable()) {
                            // 思考，accept接受连接且会返回新连接的fd对吧？那新的FD怎么办？
                            // 答：当然是使用epoll_ctl把新的客户端fd注册到内核空间
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
            // 调用accept()接受客户端 系统调用 -> fd7
            SocketChannel client = ssc.accept();
            client.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            /**
             * 将新接受的fd7添加到红黑树
             * epoll_ctl(fd3,EPOLL_CTL_ADD,fd7,EPOLLIN)
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
        EpollMultiplexingSingleThread service = new EpollMultiplexingSingleThread();
        service.start();
    }
}

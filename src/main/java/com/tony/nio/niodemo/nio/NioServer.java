package com.tony.nio.niodemo.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * StandardSocketOptions.TCP_NODELAY
 * StandardSocketOptions.SO_KEEPALIVE
 * StandardSocketOptions.SO_LINGER
 * StandardSocketOptions.SO_RCVBUF
 * StandardSocketOptions.SO_SNDBUF
 * StandardSocketOptions.SO_REUSEADDR
 */
public class NioServer {

    // what why how
    public static void main(String[] args) throws Exception {
        LinkedList<SocketChannel> clients = new LinkedList<>();
        //服务端开启监听：接受客户端
        ServerSocketChannel ss = ServerSocketChannel.open();
        ss.bind(new InetSocketAddress(9090));
        // 重点 OS NONBLOCKING
        // 接收客户端不阻塞
        ss.configureBlocking(false);
//        ss.setOption(StandardSocketOptions.TCP_NODELAY, false);
        while (true) {
            // 接受客户端的连接
            Thread.sleep(1000);
            // 不会阻塞？-1 NULL
            SocketChannel client = ss.accept();
            //accept  调用内核了：
            //1，没有客户端连接进来，返回值？在BIO 的时候一直卡着，但是在NIO，不卡着，返回-1，NULL
            // 如果来客户端的连接，accept返回的是这个客户端的fd
            if (client == null) {
             //   System.out.println("没有客户端连接");
            } else {
                //重点socket（服务端的listen socket<连接请求三次握手后，往我这里扔，我去通过accept 得到  连接的socket>，连接socket<连接后的数据读写使用的> ）
                client.configureBlocking(false);
                int port = client.socket().getPort();
                System.out.println("client..port: " + port);
                clients.add(client);
            }
            // 可以在堆内、堆外
            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
            // 遍历已经链接进来的客户端能不能读写数据
            for (SocketChannel c : clients) {
                // >0  -1  0  不会阻塞
                int num = c.read(buffer);
                if (num > 0) {
                    buffer.flip();
                    byte[] aaa = new byte[buffer.limit()];
                    buffer.get(aaa);
                    String b = new String(aaa);
                    System.out.println(c.socket().getPort() + " : " + b);
                    buffer.clear();
                }
            }
        }
    }

}

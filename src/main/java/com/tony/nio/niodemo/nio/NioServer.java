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

    public static void main(String[] args) throws Exception {
        LinkedList<SocketChannel> clients = new LinkedList<>();
        ServerSocketChannel ss = ServerSocketChannel.open();
        ss.bind(new InetSocketAddress(9090));
        // 重点 NONBLOCKING
        // 接收客户端不阻塞
        ss.configureBlocking(false);
        while (true) {
            // 接受客户端的连接
            Thread.sleep(1000);
            // 立马返回
            SocketChannel client = ss.accept();
            if (client == null) {
                Thread.sleep(3000);
                System.out.println("no client connect");
            } else {
                client.configureBlocking(false);
                int port = client.socket().getPort();
                System.out.println("client..port: " + port);
                clients.add(client);
            }
            // 可以在堆内、堆外
            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
            // 遍历已经链接进来的客户端能不能读写数据
            for (SocketChannel c : clients) {
                // 返回值: >0 -1 0,不会阻塞
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

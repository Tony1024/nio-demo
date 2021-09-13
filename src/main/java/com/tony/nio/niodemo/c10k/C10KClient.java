package com.tony.nio.niodemo.c10k;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * 用windows启动，连接远程的vmvare linux服务器
 */
public class C10KClient {

    public static void main(String[] args) {
        LinkedList<SocketChannel> clients = new LinkedList<>();
        InetSocketAddress serverAddr = new InetSocketAddress("192.168.23.147", 9090);
        // 端口号的问题：65535
        for (int i = 10000; i < 10001; i++) {
            try {
                SocketChannel client = SocketChannel.open();
                client.bind(new InetSocketAddress("127.0.0.1", i));
                client.connect(serverAddr);

                SocketChannel client1 = SocketChannel.open();
                client1.bind(new InetSocketAddress("192.168.23.147", i));
                client1.connect(serverAddr);

                clients.add(client);
                clients.add(client1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("clients:" + clients.size());
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

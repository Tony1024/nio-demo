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
        InetSocketAddress serverAddr = new InetSocketAddress("192.168.62.128", 9090);
        // 端口号的问题：65535
        for (int i = 10000; i < 65000; i++) {
            try {
                SocketChannel client1 = SocketChannel.open();
                SocketChannel client2 = SocketChannel.open();

                /*
                linux中你看到的连接就是：
                client...port: 10508
                client...port: 10508
                 */

                client1.bind(new InetSocketAddress("192.168.150.1", i));
                //  192.168.150.1：10000   192.168.150.11：9090
                client1.connect(serverAddr);
                clients.add(client1);

                client2.bind(new InetSocketAddress("192.168.110.100", i));
                //  192.168.110.100：10000  192.168.150.11：9090
                client2.connect(serverAddr);
                clients.add(client2);

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        System.out.println("clients " + clients.size());

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

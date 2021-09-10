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
        InetSocketAddress serverAddr = new InetSocketAddress("10.152.100.45", 9090);
        // 端口号的问题：65535
        for (int i = 10000; i < 65000; i++) {
            try {
                SocketChannel client = SocketChannel.open();
                client.bind(new InetSocketAddress("10.152.211.3", i));
                client.connect(serverAddr);
                clients.add(client);
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

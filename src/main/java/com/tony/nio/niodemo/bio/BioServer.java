package com.tony.nio.niodemo.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BioServer {

    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(9090, 20);
        System.out.println("step1: new ServerSocket(9090)");
        while (true) {
            // 阻塞1
            Socket client = server.accept();
            System.out.println("step2:client:" + client.getPort());
            new Thread(() -> {
                InputStream in = null;
                try {
                    in = client.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    while (true) {
                        // 阻塞2
                        String line = reader.readLine();
                        if (null != line) {
                            System.out.println(line);
                        } else {
                            client.close();
                            break;
                        }
                    }
                    System.out.println("客户端断开");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }


}

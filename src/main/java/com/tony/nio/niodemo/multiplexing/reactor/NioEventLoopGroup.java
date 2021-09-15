package com.tony.nio.niodemo.multiplexing.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author gaoweidong
 * @date 2021/9/13
 */
public class NioEventLoopGroup {

    private final NioEventLoop[] nioEventLoops;

    private AtomicInteger count = new AtomicInteger(0);

    private NioEventLoopGroup worker = this;

    public void setWorker(NioEventLoopGroup worker) {
        this.worker = worker;
    }

    NioEventLoopGroup(int num) {
        // 线程数
        nioEventLoops = new NioEventLoop[num];
        for (int i = 0; i < num; i++) {
            nioEventLoops[i] = new NioEventLoop(this);
            new Thread(nioEventLoops[i]).start();
        }
    }

    public void bind(int port) {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            //注册到那个selector上呢？
            registerSelector(serverSocketChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registerSelector(Channel channel) {
        try {
            NioEventLoop selectedNioEventLoop;
            if (channel instanceof ServerSocketChannel) {
                // 从bossGroup选一个selector
                selectedNioEventLoop = bossNext();
                selectedNioEventLoop.taskQueue.put(channel);
                // listen 选择了 boss组中的一个线程后，要更新这个线程的work组
                selectedNioEventLoop.setWorker(worker);
            } else {
                // 从workerGroup选一个selector
                selectedNioEventLoop = workerNext();
                selectedNioEventLoop.taskQueue.add(channel);
            }
            // 让select()返回，让对应的nioEventLoop去执行自己taskQueue里的任务
            selectedNioEventLoop.selector.wakeup();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private NioEventLoop bossNext() {
        int index = count.incrementAndGet() % nioEventLoops.length;
        return nioEventLoops[index];
    }

    private NioEventLoop workerNext() {
        int index = count.incrementAndGet() % worker.nioEventLoops.length;
        return worker.nioEventLoops[index];
    }

}

package com.tony.nio.niodemo.multiplexing.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectorThreadGroup {

    private SelectorThread[] selectors;

    private ServerSocketChannel server = null;

    private AtomicInteger xid = new AtomicInteger(0);

    private SelectorThreadGroup worker = this;

    public void setWorker(SelectorThreadGroup workder) {
        this.worker = workder;
    }

    SelectorThreadGroup(int num) {
        //num  线程数
        selectors = new SelectorThread[num];
        for (int i = 0; i < num; i++) {
            selectors[i] = new SelectorThread(this);
            new Thread(selectors[i]).start();
        }
    }

    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            //注册到那个selector上呢？
            registerSelector(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registerSelector(Channel c) {
        try {
            if (c instanceof ServerSocketChannel) {
                SelectorThread st = bossNext();
                st.queue.put(c);
                // listen 选择了 boss组中的一个线程后，要更新这个线程的work组
                st.setWorker(worker);
                st.selector.wakeup();
            } else {
                // 在 main线程种，取到堆里的selectorThread对象
                SelectorThread st = workerNext();
                //1,通过队列传递数据 消息
                st.queue.add(c);
                //2,通过打断阻塞，让对应的线程去自己在打断后完成注册selector
                st.selector.wakeup();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private SelectorThread bossNext() {
        int index = xid.incrementAndGet() % selectors.length;
        return selectors[index];
    }

    private SelectorThread workerNext() {
        int index = xid.incrementAndGet() % worker.selectors.length;
        return worker.selectors[index];
    }

}

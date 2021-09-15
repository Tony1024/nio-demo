package com.tony.nio.niodemo.multiplexing.reactor;

/**
 * @author gaoweidong
 * @date 2021/9/13
 */
public class Main {

    public static void main(String[] args) {
        // boss有自己的线程组
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(4);
        // worker有自己的线程组
        NioEventLoopGroup worker = new NioEventLoopGroup(4);
        // boss得持有worker的引用：
        bossGroup.setWorker(worker);
        // 可多个端口同时开
        bossGroup.bind(9999);
        bossGroup.bind(9998);
        bossGroup.bind(9997);
        bossGroup.bind(9996);
    }
}

package com.tony.nio.niodemo.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * 文件IO
 *
 * @author gaoweidong
 * @date 2021/9/17
 */
public class OSFileIO {

    static byte[] data = "123456789\n".getBytes();
    static String path = "/root/testio/out.txt";

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "0":
                testBasicFileIO();
                break;
            case "1":
                testBufferedFileIO();
                break;
            default:
        }
    }

    /**
     * 最基本的file写
     *
     * @throws Exception
     */
    public static void testBasicFileIO() throws Exception {
        File file = new File(path);
        FileOutputStream out = new FileOutputStream(file);
        while (true) {
            out.write(data);
        }
    }

    /**
     * 测试buffer文件IO jvm  8kB   syscall  write(8KBbyte[])
     *
     * @throws Exception
     */
    public static void testBufferedFileIO() throws Exception {
        File file = new File(path);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        while (true) {
            Thread.sleep(10);
            out.write(data);
        }
    }

}
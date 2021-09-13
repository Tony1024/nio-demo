package com.tony.nio.niodemo.thread;

/**
 * @author gaoweidong
 * @date 2021/9/13 10:14
 */
public class ThreadContextChange {

    private final static Object object = new Object();
    private static boolean isA = true;
    private static boolean isB = false;
    private static boolean isC = false;

    public static void main(String[] args) {

        Thread thread1 = new Thread(() -> {
            while (true) {
                synchronized (object) {
                    if (isA) {
                        System.out.println(Thread.currentThread().getId() + "A");
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        isB = true;
                        isA = false;
                        object.notifyAll();
                    } else {
                        try {
                            object.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        thread1.start();

        Thread thread2 = new Thread(() -> {
            while (true) {
                synchronized (object) {
                    if (isB) {
                        System.out.println("B");
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        isC = true;
                        isB = false;
                        object.notifyAll();
                    } else {
                        try {
                            object.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        thread2.start();

        Thread thread3 = new Thread(() -> {
            while (true) {
                synchronized (object) {
                    if (isC) {
                        System.out.println("C");
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        isA = true;
                        isC = false;
                        object.notifyAll();
                    } else {
                        try {
                            object.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        thread3.start();
    }

}

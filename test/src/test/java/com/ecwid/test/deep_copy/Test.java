package com.ecwid.test.deep_copy;

import java.util.concurrent.*;

public class Test {

    /**
     * --add-opens=java.base/java.lang=ALL-UNNAMED
     * --add-opens=java.base/java.util=ALL-UNNAMED
     * --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
     * --add-opens=java.base/java.util.concurrent=ALL-UNNAMED
     * --add-opens=java.base/java.lang.ref=ALL-UNNAMED
     * --add-opens=java.base/jdk.internal.vm=ALL-UNNAMED
     * --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED
     *
     */
    public static void main(String[] args)
    {
        testInherited();
        testFancyTypes();
    }

    private static void testFancyTypes() {

        testConcurrentHashMap();
        testConcurrentSkipListMap();
        try {
            testThreadPool();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void testConcurrentSkipListMap() {

        ConcurrentSkipListMap<String, B> m = new ConcurrentSkipListMap<>();
        m.put("first", new B(1, 100));
        m.put("second", new B(2, 200));
        ConcurrentSkipListMap<String, B> mCopy = CopyUtils.deepCopy(m);
        System.out.println(mCopy);
        mCopy.put("third", new B(3, 300));
        m.put("third", new B(4, 400));
        System.out.println(m);
        System.out.println(mCopy);
    }

    private static void testConcurrentHashMap() {

        ConcurrentHashMap<String, B> m = new ConcurrentHashMap<>();
        m.put("first", new B(1, 100));
        m.put("second", new B(2, 200));
        ConcurrentHashMap<String, B> mCopy = CopyUtils.deepCopy(m);
        System.out.println(mCopy);
        mCopy.put("third", new B(3, 300));
        m.put("third", new B(4, 400));
        System.out.println(m);
        System.out.println(mCopy);
    }

    private static void testThreadPool() throws InterruptedException {
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
// we cannot copy running structure
//        exec.schedule(()->System.out.println("At last!"), 5, TimeUnit.SECONDS);
        ScheduledExecutorService eCopy = CopyUtils.deepCopy(exec);
        exec.shutdownNow();
        eCopy.schedule(()->System.out.println("At last!"), 10, TimeUnit.MILLISECONDS);
        Thread.sleep(20);
        eCopy.shutdownNow();
    }

    private static void testInherited() {

        B b = new B(1, 11);
        B bCopy = CopyUtils.deepCopy(b);
        System.out.println(bCopy);
        System.out.println(bCopy.getConst());
    }
}

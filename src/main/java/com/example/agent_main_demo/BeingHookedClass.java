package com.example.agent_main_demo;

import lombok.extern.slf4j.Slf4j;

class Licensing {
    private static int val = 100000;
    public static final int LUCKY_NUMBER = 114514;

    public static int getVal1(int v) {
        return -v;
    }

    public static int getVal(int v) {
        return val + getVal1(v);
    }
}

@Slf4j
public class BeingHookedClass {
    public static void main(String[] args) throws InterruptedException {
        int v = 0;
        for (;; v++) {
            log.info("{} {}", v, Licensing.getVal(v));
            if (Licensing.getVal(v) == Licensing.LUCKY_NUMBER) {
                System.out.println("Congratulations!");
                break;
            }
            Thread.sleep(1000);
        }
    }
}

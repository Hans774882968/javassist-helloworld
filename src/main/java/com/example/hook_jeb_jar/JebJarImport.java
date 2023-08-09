package com.example.hook_jeb_jar;

import com.pnfsoftware.jebglobal.sz;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;

import com.pnfsoftware.jeb.client.Licensing;

@Slf4j
public class JebJarImport {
    public static void main(String[] args) {
        String res = sz.RF(new byte[] { -123, 69, 35, 102, 121, 58, 67, 125, 47, 82, 112, 38, 84, 92, 21, 78, 99, 2, 97,
                99, 2, 97, 99, 2 }, 1, 99);
        log.info(res);
        int timestamp = Licensing.getExpirationTimestamp();
        log.info(new Date(timestamp * 1000L).toString());
    }
}

package com.example.hook_jeb_jar;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PatchExpirationTest {
    public static void callGetBuildTypeString(Class<?> licensingClass) throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method getBuildTypeString = licensingClass.getDeclaredMethod("getBuildTypeString");
        getBuildTypeString.setAccessible(true);
        String buildTypeString = (String) getBuildTypeString.invoke(null);
        log.info(buildTypeString); // "release/full/individual/air-gap/any-client/core-api/subscription" hook 成功
    }

    public static void callDecoderMethod(Class<?> decodeByteClass) throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method decoderMethod = decodeByteClass.getDeclaredMethod("RF",
                new Class[] { byte[].class, int.class, int.class });
        decoderMethod.setAccessible(true);
        String s = (String) decoderMethod.invoke(null, new byte[] { -123, 69, 35, 102, 121, 58, 67, 125, 47, 82, 112,
                38, 84, 92, 21, 78, 99, 2, 97, 99, 2, 97, 99, 2 }, 1, 99);
        log.info(s); // "检查更新中。。。" hook 成功
    }

    public static void callGetExpirationTimestamp(Class<?> licensingClass) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Method getExpirationTimestamp = licensingClass.getDeclaredMethod("getExpirationTimestamp");
        int timestamp = (int) getExpirationTimestamp.invoke(null);
        log.info(new Date(timestamp * 1000L).toString()); // Thu May 30 08:00:00 CST 2030 hook 成功
    }

    public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, IOException {
        String jarFilePath = "D:\\java-source-codes\\jeb.jar";
        File file = new File(jarFilePath);

        if (!file.exists()) {
            log.info("文件{}不存在！", jarFilePath);
            return;
        }
        if (!file.isFile()) {
            log.info("读取的 {} 为文件夹而非文件！", jarFilePath);
            return;
        }
        if (!file.canRead()) {
            log.info("当前文件{}不可读！", jarFilePath);
            return;
        }
        URL fileUrl = file.toURI().toURL();
        URLClassLoader jarUrlClassLoader = new URLClassLoader(new URL[] { fileUrl },
                Thread.currentThread().getContextClassLoader());

        Class<?> decodeByteClass = jarUrlClassLoader.loadClass("com.pnfsoftware.jebglobal.sz");
        Class<?> licensingClass = jarUrlClassLoader.loadClass("com.pnfsoftware.jeb.client.Licensing");
        callGetBuildTypeString(licensingClass);
        callDecoderMethod(decodeByteClass);
        callGetExpirationTimestamp(licensingClass);

        jarUrlClassLoader.close();
    }
}

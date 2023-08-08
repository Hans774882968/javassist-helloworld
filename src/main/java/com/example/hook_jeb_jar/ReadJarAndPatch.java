package com.example.hook_jeb_jar;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class ReadJarAndPatch {
    public static void patchLicensingClass() throws CannotCompileException, NotFoundException, IOException {
        CtClass cls = ClassPool.getDefault().get("com.pnfsoftware.jeb.client.Licensing");
        CtMethod mth = cls.getDeclaredMethod("getExpirationTimestamp");
        mth.setBody("{return real_license_ts + (864000 * license_validity);}");
        cls.writeFile("D:\\java-source-codes\\hooked_jeb");
        // cls.writeFile 入参为文件夹，只会把当前类的改动写入磁盘
        // cls.toClass();
        // 调用 cls.toClass() 后就导致 Exception in thread "main" java.lang.NoClassDefFoundError: com/pnfsoftware/jeb/util/logging/GlobalLog
        // 不调用的话发现 sz.RF hook 成功，但 licensingClass1 hook 失败
        // TODO: 这里 licensingClass1 和 sz.RF 没做到都 hook 成功，希望找到一个解决方案
    }

    public static Class<?> patchDecoderClass() throws CannotCompileException, NotFoundException, IOException {
        ClassPool pool = ClassPool.getDefault();
        CtClass cls = pool.get("com.pnfsoftware.jebglobal.sz");
        CtClass[] params = {
                pool.get("byte[]"),
                pool.get("int"),
                pool.get("int"),
        };
        CtMethod mth = cls.getDeclaredMethod("RF", params);
        // 这句 printf 会编译失败
        // mth.setBody(
        // "{String res = (new com.pnfsoftware.jebglobal.sz($1, $2, $3)).RF();System.out.printf(\"%s %s %s %s\\n\", res, $1, $2, $3);return res;}");
        // 实测 javassist 内部实现的编译器前端不支持 foreach 语法
        mth.setBody(
                "{String res = (new com.pnfsoftware.jebglobal.sz($1, $2, $3)).RF();String byteStr = \"\";for(int i = 0; i < $1.length; i++){byteStr += $1[i] + \",\";}System.out.println(res + \" \" + byteStr + \" \" + $2 + \" \" + $3);return res;}");
        cls.writeFile("D:\\java-source-codes\\hooked_jeb");
        return cls.toClass();
    }

    public static void callGetBuildTypeString(Class<?> licensingClass) throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method getBuildTypeString = licensingClass.getDeclaredMethod("getBuildTypeString");
        getBuildTypeString.setAccessible(true);
        String buildTypeString = (String) getBuildTypeString.invoke(null);
        System.out.println(buildTypeString); // "release/full/individual/air-gap/any-client/core-api/subscription" hook 成功
    }

    public static void callDecoderMethod(Class<?> decodeByteClass) throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method decoderMethod = decodeByteClass.getDeclaredMethod("RF",
                new Class[] { byte[].class, int.class, int.class });
        decoderMethod.setAccessible(true);
        String s = (String) decoderMethod.invoke(null, new byte[] { 49, 10, 28, 28, 19, 26, 2, 71 }, 2, 31);
        System.out.println(s); // "release/" hook 成功
    }

    public static void callGetExpirationTimestamp(Class<?> licensingClass) throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method getExpirationTimestamp = licensingClass.getDeclaredMethod("getExpirationTimestamp");
        int timestamp = (int) getExpirationTimestamp.invoke(null);
        System.out.println(new Date(timestamp * 1000L)); // hook 失败
    }

    public static void main(String[] args) throws CannotCompileException, NotFoundException, IOException,
            ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        String jarFilePath = "D:\\java-source-codes\\jeb.jar";

        File file = new File(jarFilePath);

        if (!file.exists()) {
            System.out.println("文件不存在！");
            return;
        }
        if (!file.isFile()) {
            System.out.println("读取的为文件夹而非文件！");
            return;
        }
        if (!file.canRead()) {
            System.out.println("当前文件不可读！");
            return;
        }
        URL fileUrl = file.toURI().toURL();
        URLClassLoader jarUrlClassLoader = new URLClassLoader(new URL[] { fileUrl },
                Thread.currentThread().getContextClassLoader());

        ClassPool.getDefault().insertClassPath(jarFilePath);

        Class<?> decodeByteClass = patchDecoderClass();
        System.out.printf("decodeByteClass %s\n", decodeByteClass.getClassLoader());// dbg jdk.internal.loader.ClassLoaders$AppClassLoader
        patchLicensingClass();
        Class<?> licensingClass1 = jarUrlClassLoader.loadClass("com.pnfsoftware.jeb.client.Licensing");
        System.out.printf("licensingClass %s\n", licensingClass1.getClassLoader());// dbg java.net.URLClassLoader
        callGetBuildTypeString(licensingClass1);

        callDecoderMethod(decodeByteClass);

        callGetExpirationTimestamp(licensingClass1);

        // Exception in thread "main" java.lang.ClassNotFoundException: com.pnfsoftware.jeb.client.Licensing
        // Class<?> licensingClass2 = Thread.currentThread().getContextClassLoader()
        // .loadClass("com.pnfsoftware.jeb.client.Licensing");
        // callGetExpirationTimestamp(licensingClass2);

        jarUrlClassLoader.close();
    }
}

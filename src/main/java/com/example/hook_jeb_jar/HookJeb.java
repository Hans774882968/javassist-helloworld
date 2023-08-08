package com.example.hook_jeb_jar;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class PatchTransformer implements ClassFileTransformer {
    public static String agentArgs = "";
    public static boolean hasInsertedPath = false;

    public static boolean shouldInsertClassPathJeb() {
        return agentArgs != null && agentArgs.equals("shouldInsertClassPathJeb");
    }

    public static void insertClassPathJebOnce() {
        if (hasInsertedPath) {
            return;
        }
        hasInsertedPath = true;
        String jarFilePath = "D:\\java-source-codes\\jeb.jar";
        try {
            ClassPool.getDefault().insertClassPath(jarFilePath);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    public byte[] hookLicensingClass(String className) {
        log.info("start hooking com.pnfsoftware.jeb.client.Licensing.getExpirationTimestamp...");
        String clsName = className.replace("/", ".");
        CtClass cls = null;
        try {
            cls = ClassPool.getDefault().get(clsName);
            CtMethod mth = cls.getDeclaredMethod("getExpirationTimestamp");
            mth.setBody("{return real_license_ts + (864000 * license_validity);}");
            log.info("hook com.pnfsoftware.jeb.client.Licensing.getExpirationTimestamp success!!!");
            return cls.toBytecode();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] hookDecoderClass() {
        String decoderClassName = "com.pnfsoftware.jebglobal.sz";
        log.info("start hooking {}.RF...", decoderClassName);
        ClassPool pool = ClassPool.getDefault();
        CtClass cls;
        try {
            cls = pool.get(decoderClassName);
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
            log.info("hook {}.RF success!!!", decoderClassName);
            return cls.toBytecode();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (shouldInsertClassPathJeb()) {
            insertClassPathJebOnce();
        }
        if (className.equals("com/pnfsoftware/jeb/client/Licensing")) {
            return hookLicensingClass(className);
        }
        if (className.equals("com/pnfsoftware/jebglobal/sz")) {
            return hookDecoderClass();
        }
        return null;
    }
}

public class HookJeb {
    public static void premain(String agentArgs, Instrumentation inst) {
        PatchTransformer.agentArgs = agentArgs;
        inst.addTransformer(new PatchTransformer());
    }
}

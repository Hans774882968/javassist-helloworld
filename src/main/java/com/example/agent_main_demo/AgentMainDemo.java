package com.example.agent_main_demo;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class PatchTransformer implements ClassFileTransformer {
    public byte[] hookBeingHookedLicensingClass(ClassLoader loader) {
        String clsName = "com.example.agent_main_demo.Licensing";
        log.info("start hooking {}.getVal1()...", clsName);
        ClassPool pool = ClassPool.getDefault();
        try {
            pool.insertClassPath(new LoaderClassPath(loader));
            CtClass cls = pool.get(clsName);
            CtMethod mth = cls.getDeclaredMethod("getVal1", new CtClass[] {
                    pool.get("int")
            });
            mth.setBody("{return 14514;}");
            log.info("hook {}.getVal1() success!!!", clsName);
            return cls.toBytecode();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className.equals("com/example/agent_main_demo/Licensing")) {
            return hookBeingHookedLicensingClass(loader);
        }
        return null;
    }
}

@Slf4j
public class AgentMainDemo {
    public static void agentmain(String agentArgs, Instrumentation inst)
            throws ClassNotFoundException, UnmodifiableClassException {
        log.info("agentArgs: {}", agentArgs);
        inst.addTransformer(new PatchTransformer(), true);
        inst.retransformClasses(Class.forName("com.example.agent_main_demo.Licensing"));
    }
}

package com.alibaba.bytekit.utils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 运行时获取 JVM 实际字节码
 * @author hengyunabc 2024-02-27
 */
public class BytecodeRetrievalTool {

    public static byte[] getClassBytecode(Instrumentation instrumentation, Class<?> clazz) {
        // 用于保存类字节码的引用
        AtomicReference<byte[]> bytecodeRef = new AtomicReference<>();

        // 创建一个 ClassFileTransformer，捕获类的字节码
        ClassFileTransformer transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain domain, byte[] classfileBuffer) throws IllegalClassFormatException {

                if (classBeingRedefined != null) {
                    if (clazz != classBeingRedefined) {
                        return null;
                    }
                }

                if (clazz.getName().replace('.', '/').equals(className)) {
                    bytecodeRef.set(classfileBuffer);
                    return null;
                }
                return null;
            }
        };

        // 注册 transformer
        instrumentation.addTransformer(transformer, true);

        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception e) {
            throw new RuntimeException("Retransformation failed", e);
        } finally {
            instrumentation.removeTransformer(transformer);
        }

        return bytecodeRef.get();
    }
}

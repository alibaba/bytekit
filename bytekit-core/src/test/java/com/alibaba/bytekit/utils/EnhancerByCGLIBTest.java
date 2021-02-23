package com.alibaba.bytekit.utils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.transform.impl.UndeclaredThrowableStrategy;

/**
 * 
 * @author hengyunabc 2021-02-23
 *
 */
public class EnhancerByCGLIBTest {

    static class CglibDemo implements MethodInterceptor {

        public static <T> T newInstance(Class<T> clazz) {
            CglibDemo interceptor = new CglibDemo();
            Enhancer e = new Enhancer();

            UndeclaredThrowableStrategy ss = new UndeclaredThrowableStrategy(
                    java.lang.reflect.UndeclaredThrowableException.class);
            e.setStrategy(ss);
            e.setSuperclass(clazz);
            e.setCallback(interceptor);
            return (T) e.create();
        }

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            return null;
        }
    }

    public static class Person {

        public Person() {
        }
    }

    public static class Student {

        public Student() {
        }
    }

    @Test
    public void test() {
        Instrumentation instrumentation = ByteBuddyAgent.install();

        ClassFileTransformer transformer = new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

                ClassNode classNode = AsmUtils.toClassNode(classfileBuffer);

                // support jdk7 test. cglib 3.3.0 only support jdk8
                if (AsmUtils.getMajorVersion(classNode.version) > 51) {
                    classNode.version = AsmUtils.setMajorVersion(classNode.version, 51);
                }

                // only process Person
                if (className.contains(Person.class.getSimpleName())) {
                    if (AsmUtils.isEnhancerByCGLIB(className)) {
                        for (MethodNode methodNode : classNode.methods) {
                            if (AsmUtils.isConstructor(methodNode)) {
                                AsmUtils.fixConstructorExceptionTable(methodNode);
                            }
                        }
                        return AsmUtils.toBytes(classNode);
                    }
                }

                return AsmUtils.toBytes(classNode);
            }

        };

        try {

            instrumentation.addTransformer(transformer, true);

            boolean cglibError = false;
            try {
                CglibDemo.newInstance(Student.class);
            } catch (Throwable e) {
                e.printStackTrace();
                cglibError = true;
            }

            Assertions.assertThat(cglibError).isTrue();

            CglibDemo.newInstance(Person.class);
        } finally {
            instrumentation.removeTransformer(transformer);
        }

    }

}

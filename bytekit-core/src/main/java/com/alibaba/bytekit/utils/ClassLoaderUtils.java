package com.alibaba.bytekit.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import sun.misc.Unsafe;


/**
 *
 * @author hengyunabc 2017-10-12
 *
 */
public class ClassLoaderUtils {
    private static final FakeBootstrapClassLoader FAKEBOOTSTRAPCLASSLOADER = new FakeBootstrapClassLoader();

    public static ClassLoader wrap(ClassLoader classLoader) {
        if (classLoader != null) {
            return classLoader;
        }
        return FAKEBOOTSTRAPCLASSLOADER;
    }

    private static class FakeBootstrapClassLoader extends ClassLoader {

        public FakeBootstrapClassLoader() {
            super(ClassLoader.getSystemClassLoader().getParent());
        }

    }

    @SuppressWarnings({ "restriction", "unchecked" })
    public static URL[] getUrls(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            return ((URLClassLoader) classLoader).getURLs();
        }

        // jdk9
        if (classLoader.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$")) {
            try {
                Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);

                Class<?> ucpOwner = classLoader.getClass();
                Field ucpField = null;

                // jdk 9~15: jdk.internal.loader.ClassLoaders$AppClassLoader.ucp
                // jdk 16~17: jdk.internal.loader.BuiltinClassLoader.ucp
                while (ucpField == null && !ucpOwner.getName().equals("java.lang.Object")) {
                    try {
                        ucpField = ucpOwner.getDeclaredField("ucp");
                    } catch (NoSuchFieldException ex) {
                        ucpOwner = ucpOwner.getSuperclass();
                    }
                }

                long ucpFieldOffset = unsafe.objectFieldOffset(ucpField);
                Object ucpObject = unsafe.getObject(classLoader, ucpFieldOffset);

                // jdk.internal.loader.URLClassPath.path
                Field pathField = ucpField.getType().getDeclaredField("path");
                long pathFieldOffset = unsafe.objectFieldOffset(pathField);
                ArrayList<URL> path = (ArrayList<URL>) unsafe.getObject(ucpObject, pathFieldOffset);

                return path.toArray(new URL[path.size()]);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * internalName 是 java/lang/String 的形式
     * @param classLoader
     * @param internalName
     * @return
     */
    public static byte[] readBytecodeByName(ClassLoader classLoader, String internalName) {
        if (internalName == null || classLoader == null) {
            return null;
        }
        try {
            InputStream inputStream = classLoader.getResourceAsStream(internalName + ".class");
            return IOUtils.getBytes(inputStream);
        } catch (IOException e) {
            // ignore
        }
        return null;
    }

    public static byte[] readBytecode(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        return readBytecodeByName(clazz.getClassLoader(), AsmUtils.internalClassName(clazz));
    }
}
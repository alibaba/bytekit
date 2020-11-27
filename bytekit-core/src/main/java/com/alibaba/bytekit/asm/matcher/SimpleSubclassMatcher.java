package com.alibaba.bytekit.asm.matcher;

import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.alibaba.bytekit.utils.ClassLoaderUtils;
import com.alibaba.deps.org.objectweb.asm.ClassReader;

/**
 * 
 * @author hengyunabc 2020-11-24
 *
 */
public class SimpleSubclassMatcher implements ClassMatcher {

    Set<String> classNames = new HashSet<String>();

    public SimpleSubclassMatcher(String... className) {
        for (String name : className) {
            this.classNames.add(name);
        }
    }

    public SimpleSubclassMatcher(Collection<String> names) {
        this.classNames.addAll(names);
    }

    @Override
    public boolean match(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        loader = ClassLoaderUtils.wrap(loader);

        if (classBeingRedefined != null) { // 在retransform 时，可以直接判断
            return match(classBeingRedefined);
        } else {
            // 读取出具体的 类名，还有父类名， 如果有匹配，则返回 true，没有，就返回 false
            ClassReader reader = new ClassReader(classfileBuffer);
            String clazzName = reader.getClassName();
            String superName = reader.getSuperName();

            if (classNames != null && classNames.contains(clazzName.replace('/', '.'))) {
                return true;
            }

            while (!superName.equals("java/lang/Object")) {
                if (classNames.contains(superName)) {
                    return true;
                }
                try {
                    Class<?> superClass = loader.loadClass(superName.replace('/', '.'));
                    return match(superClass);
                } catch (ClassNotFoundException e) {
                    // ignore
                    return false;
                }
            }
        }

        return false;
    }

    private boolean match(Class<?> clazz) {
        for (Class<?> superclass = clazz;;) {
            if (superclass == null || Object.class.equals(superclass)) {
                return false;
            }
            if (classNames != null && classNames.contains(superclass.getName().replace('/', '.'))) {
                return true;
            }
            superclass = superclass.getSuperclass();
        }
    }

}

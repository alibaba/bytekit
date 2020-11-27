package com.alibaba.bytekit.asm.matcher;

import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.alibaba.bytekit.utils.ClassLoaderUtils;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.Opcodes;

/**
 * 
 * @author hengyunabc 2020-11-25
 *
 */
public class SimpleInterfaceMatcher implements ClassMatcher {

    Set<String> interfaces = new HashSet<String>();

    public SimpleInterfaceMatcher(String... interfaces) {
        for (String name : interfaces) {
            this.interfaces.add(name);
        }
    }

    public SimpleInterfaceMatcher(Collection<String> interfaces) {
        this.interfaces.addAll(interfaces);
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
            String[] interfacesArray = reader.getInterfaces();

            // 如果是接口，则没有需要处理的地方
            if ((reader.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                return false;
            }

            if (interfaces != null && interfaces.contains(clazzName.replace('/', '.'))) {
                return true;
            }

            for (String i : interfacesArray) {
                try {
                    Class<?> interfaceClass = loader.loadClass(i.replace('/', '.'));
                    if (matchInterface(interfaceClass)) {
                        return true;
                    }
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }
            if (!"java/lang/Object".equals(superName)) {
                try {
                    Class<?> superClass = loader.loadClass(superName.replace('/', '.'));
                    if (matchClass(superClass)) {
                        return true;
                    }
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }
        }

        return false;
    }

    private boolean matchInterface(Class<?> i) {
        if (this.interfaces != null && interfaces.contains(i.getName())) {
            return true;
        }
        for (Class<?> iter : i.getInterfaces()) {
            if (matchInterface(iter)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchClass(Class<?> clazz) {
        for (Class<?> i : clazz.getInterfaces()) {
            if (matchInterface(i)) {
                return true;
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (Object.class.equals(superclass)) {
            return false;
        } else {
            if (matchClass(superclass)) {
                return true;
            }
        }
        return false;
    }

    private boolean match(Class<?> clazz) {
        if (clazz.isInterface()) {
            return matchInterface(clazz);
        }
        return matchClass(clazz);
    }

}

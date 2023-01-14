package com.alibaba.bytekit.asm.matcher;

import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.alibaba.bytekit.asm.meta.ClassMetaService;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.ClassLoaderUtils;

/**
 * 
 * @author hengyunabc 2020-11-25
 *
 */
public class SimpleInterfaceMatcher implements ClassMatcher {

    private Set<String> interfaces = new HashSet<String>();

    /**
     * 保存另一份转换为 internal 的数据，避免每次match转换
     */
    private Set<String> internalInterfaces = new HashSet<String>();

    public SimpleInterfaceMatcher(String... interfaces) {
        for (String name : interfaces) {
            add(name);
        }
    }

    public SimpleInterfaceMatcher(Collection<String> interfaces) {
        for (String name : interfaces) {
            add(name);
        }
    }

    private void add(String name) {
        interfaces.add(name);
        internalInterfaces.add(AsmUtils.internalClassName(name));
    }

    @Override
    public boolean match(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        loader = ClassLoaderUtils.wrap(loader);

        if (classBeingRedefined != null) {
            /**
             * 在retransform 时，类已经加载好，可以直接判断 TODO 哪种判断方式更快？
             */
            return match(classBeingRedefined);
        } else {
            Set<String> allInterfaces = ClassMetaService.allInterfaces(loader, className, classfileBuffer);
            for (String i : internalInterfaces) {
                if (allInterfaces.contains(i)) {
                    return true;
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

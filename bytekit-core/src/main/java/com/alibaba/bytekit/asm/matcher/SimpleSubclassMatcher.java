package com.alibaba.bytekit.asm.matcher;

import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.bytekit.asm.meta.ClassMetaService;
import com.alibaba.bytekit.utils.ClassLoaderUtils;

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

            List<String> allSuperNames = ClassMetaService.allSuperNames(loader, className, classfileBuffer);
            for (String superName : allSuperNames) {
                if (classNames.contains(superName)) {
                    return true;
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

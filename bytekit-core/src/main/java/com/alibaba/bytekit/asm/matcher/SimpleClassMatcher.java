package com.alibaba.bytekit.asm.matcher;

import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author hengyunabc 2020-11-12
 *
 */
public class SimpleClassMatcher implements ClassMatcher {

    Set<String> classNames = new HashSet<String>();

    public SimpleClassMatcher(String... className) {
        for (String name : className) {
            this.classNames.add(name);
        }
    }

    public SimpleClassMatcher(Collection<String> names) {
        this.classNames.addAll(names);
    }

    @Override
    public boolean match(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null) {
            return false;
        }
        className = className.replace('/', '.');
        if (classNames != null && classNames.contains(className)) {
            return true;
        }

        return false;
    }

}

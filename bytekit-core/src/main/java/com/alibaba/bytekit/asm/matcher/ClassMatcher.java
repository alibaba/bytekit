package com.alibaba.bytekit.asm.matcher;

import java.security.ProtectionDomain;

/**
 * 
 * @author hengyunabc
 *
 */
public interface ClassMatcher {

    boolean match(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
            byte[] classfileBuffer);

}

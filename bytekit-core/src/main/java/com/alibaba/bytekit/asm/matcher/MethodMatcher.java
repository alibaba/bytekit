package com.alibaba.bytekit.asm.matcher;

import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;

public interface MethodMatcher {

    boolean match(String className, MethodNode methodNode);
}

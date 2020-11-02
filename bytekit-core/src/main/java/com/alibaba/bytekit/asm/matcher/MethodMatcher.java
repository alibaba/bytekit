package com.alibaba.bytekit.asm.matcher;

import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;

public interface MethodMatcher {

    boolean match(String className, MethodNode methodNode);
}

package com.alibaba.bytekit.asm.matcher;

public interface ClassMatcher {

    boolean match(String name, ClassLoader classLoader);

}

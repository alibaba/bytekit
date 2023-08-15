package com.alibaba.bytekit.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;

public class ClassLoaderUtilsTest {

    @Test
    public void test() {
        byte[] bytecode = ClassLoaderUtils.readBytecode(String.class);
        ClassNode classNode = AsmUtils.toClassNode(bytecode);

        Assertions.assertThat(classNode.name).isEqualTo("java/lang/String");
    }

}

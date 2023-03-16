package com.alibaba.bytekit.asm.meta;

import java.lang.instrument.Instrumentation;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.alibaba.bytekit.asm.meta.ClassMetaServiceTest.A;
import com.alibaba.bytekit.asm.meta.ClassMetaServiceTest.B;
import com.alibaba.bytekit.asm.meta.ClassMetaServiceTest.C;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.ClassLoaderUtils;

import net.bytebuddy.agent.ByteBuddyAgent;

public class ClassMetaTest {

    @Test
    public void test() {
        String BasicErrorController = "org.springframework.boot.autoconfigure.web.BasicErrorController";

        String AbstractErrorController = "org.springframework.boot.autoconfigure.web.AbstractErrorController";

        String ErrorController = "org.springframework.boot.autoconfigure.web.ErrorController";

        ClassMeta BasicMeta = fromName(BasicErrorController);

        ClassMeta AbstractMeta = fromName(AbstractErrorController);

        ClassMeta ErrorMeta = fromName(ErrorController);

        Assertions.assertThat(AbstractMeta.isAssignableFrom(BasicMeta)).isTrue();
        Assertions.assertThat(ErrorMeta.isAssignableFrom(BasicMeta)).isTrue();
        Assertions.assertThat(ErrorMeta.isAssignableFrom(AbstractMeta)).isTrue();
        Assertions.assertThat(BasicMeta.isAssignableFrom(AbstractMeta)).isFalse();

        ClassMeta BasicMeta2 = fromName(BasicErrorController);
        Assertions.assertThat(BasicMeta2.isAssignableFrom(BasicMeta)).isTrue();
        Assertions.assertThat(BasicMeta.isAssignableFrom(BasicMeta2)).isTrue();

        // 保证 ClassMeta 的实现不会加载类
        Instrumentation instrumentation = ByteBuddyAgent.install();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            String name = clazz.getName();
            if (name.equals(BasicErrorController) || name.equals(AbstractErrorController)
                    || name.equals(ErrorController)) {
                Assertions.fail("test class loaded!");
            }
        }
    }

    @Test
    public void testObject() {
        Assertions.assertThat(String.class.isAssignableFrom(String.class)).isTrue();
        Assertions.assertThat(Object.class.isAssignableFrom(String.class)).isTrue();
        Assertions.assertThat(Object.class.isAssignableFrom(Object.class)).isTrue();
        Assertions.assertThat(Object.class.isAssignableFrom(ClassMetaTest.class)).isTrue();

        ClassMeta classMetaTestMeta = fromName(ClassMetaTest.class.getName());
        ClassMeta stringMeta = fromName(String.class.getName());
        ClassMeta objectMeta = fromName(Object.class.getName());

        Assertions.assertThat(objectMeta.isAssignableFrom(classMetaTestMeta)).isTrue();
        Assertions.assertThat(objectMeta.isAssignableFrom(stringMeta)).isTrue();
        Assertions.assertThat(stringMeta.isAssignableFrom(classMetaTestMeta)).isFalse();
    }

    @Test
    public void testInterface() {
        ClassMeta cMeta = fromName(C.class.getName());
        ClassMeta bMeta = fromName(B.class.getName());
        ClassMeta aMeta = fromName(A.class.getName());

        Assertions.assertThat(bMeta.isAssignableFrom(cMeta)).isTrue();
        Assertions.assertThat(aMeta.isAssignableFrom(cMeta)).isTrue();
        Assertions.assertThat(aMeta.isAssignableFrom(bMeta)).isTrue();

        Assertions.assertThat(bMeta.isAssignableFrom(aMeta)).isFalse();
    }

    private ClassMeta fromName(String name) {
        byte[] bytes = ClassLoaderUtils.readBytecodeByName(this.getClass().getClassLoader(),
                AsmUtils.internalClassName(name));

        ClassMeta classMeta = ClassMeta.fromByteCode(bytes, this.getClass().getClassLoader());
        return classMeta;
    }

}

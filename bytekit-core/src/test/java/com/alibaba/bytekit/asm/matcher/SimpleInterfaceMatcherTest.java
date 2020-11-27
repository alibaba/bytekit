package com.alibaba.bytekit.asm.matcher;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.alibaba.bytekit.utils.AsmUtils;

/**
 * 
 * @author hengyunabc 2020-11-26
 *
 */
public class SimpleInterfaceMatcherTest {

    public interface I1 {
        public void ttt();
    }

    public interface I2 extends I1 {

    }

    public abstract class A1 implements I2 {

    }

    public abstract class A2 extends A1 {

    }

    public class C extends A2 {

        @Override
        public void ttt() {

        }

    }

    public class C2 implements I2 {

        @Override
        public void ttt() {

        }

    }

    @Test
    public void test() {
        SimpleInterfaceMatcher matcher = new SimpleInterfaceMatcher(A1.class.getName());

        ClassLoader loader = this.getClass().getClassLoader();
        Assertions.assertThat(matcher.match(loader, C2.class.getName(), C2.class, null, null)).isFalse();
    }

    @Test
    public void test1() {
        SimpleInterfaceMatcher matcher = new SimpleInterfaceMatcher(I1.class.getName());

        ClassLoader loader = this.getClass().getClassLoader();
        Assertions.assertThat(matcher.match(loader, C.class.getName(), C.class, null, null)).isTrue();
    }

    @Test
    public void test2() {
        SimpleInterfaceMatcher matcher = new SimpleInterfaceMatcher(I2.class.getName());

        ClassLoader loader = this.getClass().getClassLoader();
        Assertions.assertThat(matcher.match(loader, C.class.getName(), C.class, null, null)).isTrue();
    }

    @Test
    public void test3() throws IOException {
        SimpleInterfaceMatcher matcher = new SimpleInterfaceMatcher(I1.class.getName());

        byte[] bytes = AsmUtils.toBytes(AsmUtils.loadClass(C.class));
        Assertions.assertThat(matcher.match(this.getClass().getClassLoader(), null, null, null, bytes)).isTrue();
    }
}

package com.alibaba.bytekit.asm.matcher;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.alibaba.bytekit.utils.AsmUtils;

/**
 * 
 * @author hengyunabc 2020-11-25
 *
 */
public class SimpleSubclassMatcherTest {

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
        SimpleSubclassMatcher matcher = new SimpleSubclassMatcher(A1.class.getName());

        ClassLoader loader = this.getClass().getClassLoader();
        Assertions.assertThat(matcher.match(loader, C2.class.getName(), C2.class, null, null)).isFalse();
    }

    @Test
    public void test2() {
        SimpleSubclassMatcher matcher = new SimpleSubclassMatcher(A1.class.getName());

        ClassLoader loader = this.getClass().getClassLoader();
        Assertions.assertThat(matcher.match(loader, C.class.getName(), C.class, null, null)).isTrue();
    }

    @Test
    public void test3() {
        // Object 本身不能被增强
        SimpleSubclassMatcher matcher = new SimpleSubclassMatcher(Object.class.getName());

        ClassLoader loader = this.getClass().getClassLoader();
        Assertions.assertThat(matcher.match(loader, Object.class.getName(), Object.class, null, null)).isFalse();
    }

    @Test
    public void test4() {
        SimpleSubclassMatcher matcher = new SimpleSubclassMatcher(Object.class.getName());
        Assertions.assertThat(matcher.match(null, Object.class.getName(), Object.class, null, null)).isFalse();
    }

    @Test
    public void test5() throws IOException {
        SimpleSubclassMatcher matcher = new SimpleSubclassMatcher(A2.class.getName());

        byte[] bytes = AsmUtils.toBytes(AsmUtils.loadClass(C.class));
        Assertions.assertThat(matcher.match(A2.class.getClassLoader(), null, null, null, bytes)).isTrue();
    }

    @Test
    public void test6() throws IOException {
        SimpleSubclassMatcher matcher = new SimpleSubclassMatcher(String.class.getName());

        byte[] bytes = AsmUtils.toBytes(AsmUtils.loadClass(C.class));
        Assertions.assertThat(matcher.match(A2.class.getClassLoader(), null, null, null, bytes)).isFalse();
    }

}

package com.alibaba.bytekit.asm.binding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExit;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.alibaba.bytekit.utils.Decompiler;
import com.alibaba.bytekit.asm.interceptor.TestHelper;

public class MethodFirstLineBindingTest {

    @Rule
    public OutputCapture capture = new OutputCapture();

    public static class Sample {

        long longField;
        String strField;
        static int intField;

        public int hello(String str, boolean exception) {
            if (exception) {
                throw new RuntimeException("test exception");
            }
            return str.length();
        }

        public long toBeInvoke(int i, long l, String s, long ll) {
            return l + ll;
        }

        public void testInvokeArgs() {
            toBeInvoke(1, 123L, "abc", 100L);
        }

    }

    public static class EnterInterceptor {

        @AtEnter(inline = true)
        public static void onEnter(
                @Binding.This Object object,
                @Binding.MethodFirstLine int firstLine,
                @Binding.MethodLastLine int lastLine,
                @Binding.MethodName String methodName
                ) {
            System.err.println("AtEnter, methodName:" + methodName + ", firstLine:" + firstLine + ", lastLine:" + lastLine);
        }

    }

    public static class ExitInterceptor {

        @AtExit(inline = true)
        public static void onExit(
                @Binding.This Object object,
                @Binding.MethodFirstLine int firstLine,
                @Binding.MethodLastLine int lastLine,
                @Binding.MethodName String methodName,
                @Binding.Return Object returnObject
                ) {
            System.err.println("AtExit, methodName:" + methodName + ", firstLine:" + firstLine + ", lastLine:" + lastLine + ", return:" + returnObject);
        }

    }

    public static class ExceptionExitInterceptor {

        @AtExceptionExit(inline = true, onException = RuntimeException.class)
        public static void onExceptionExit(
                @Binding.This Object object,
                @Binding.MethodFirstLine int firstLine,
                @Binding.MethodLastLine int lastLine,
                @Binding.MethodName String methodName,
                @Binding.Throwable RuntimeException ex
                ) {
            System.err.println("AtExceptionExit, methodName:" + methodName + ", firstLine:" + firstLine + ", lastLine:" + lastLine + ", ex:" + ex.getMessage());
        }

    }

    @Test
    public void testMethodFirstLineAtEnter() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(EnterInterceptor.class).methodMatcher("hello")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        new Sample().hello("abc", false);

        System.err.println(Decompiler.decompile(bytes));

        assertThat(capture.toString()).contains("AtEnter, methodName:hello, firstLine:");
        assertThat(capture.toString()).contains("lastLine:");
    }

    @Test
    public void testMethodFirstLineAtExit() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(ExitInterceptor.class).methodMatcher("hello")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        new Sample().hello("abc", false);

        System.err.println(Decompiler.decompile(bytes));

        assertThat(capture.toString()).contains("AtExit, methodName:hello, firstLine:");
        assertThat(capture.toString()).contains("lastLine:");
    }

    @Test
    public void testMethodFirstLineAtExceptionExit() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(ExceptionExitInterceptor.class).methodMatcher("hello")
                .reTransform(true);
        byte[] bytes = helper.process(Sample.class);

        try {
            new Sample().hello("abc", true);
        } catch (RuntimeException e) {
            // expected
        }

        System.err.println(Decompiler.decompile(bytes));

        assertThat(capture.toString()).contains("AtExceptionExit, methodName:hello, firstLine:");
        assertThat(capture.toString()).contains("lastLine:");
    }

}

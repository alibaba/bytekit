package com.alibaba.bytekit.asm.binding;

import com.alibaba.bytekit.asm.interceptor.TestHelper;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.utils.Decompiler;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.Assertions.assertThat;

public class StringBindingTest {
    @Rule
    public OutputCapture capture = new OutputCapture();

    static class Sample {

        public int testStringValue() {
            int varInt = 123;
            long varLong = 1234L;
            byte varByte = 127;
            short varShort = 1223;
            boolean varBoolean = false;
            float varFloat = 0.8f;
            double varDouble = 0.3d;
            String varString = "str-wingli";
            int[] varIArray = new int[]{2, 3, 5};
            Object varObject = new Object();
            Object[] varObjectArray = new Object[]{new Object(), new Object()};

            double total = varInt + varLong + varShort;
            System.out.println(varString + total);

            return varInt * 3;
        }

    }

    public static class TestStringBindingInterceptor {

        @AtEnter
        public static void atStringBinding(@Binding.StringValue("pass-string") String stringValue) {
            System.err.println("string value:" + stringValue);
        }

    }

    @Test
    public void testStringValue() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(TestStringBindingInterceptor.class).methodMatcher("testStringValue")
                .redefine(true);
        byte[] bytes = helper.process(Sample.class);

        new Sample().testStringValue();

        System.err.println(Decompiler.decompile(bytes));

        assertThat(capture.toString()).contains("string value:pass-string");
    }


}

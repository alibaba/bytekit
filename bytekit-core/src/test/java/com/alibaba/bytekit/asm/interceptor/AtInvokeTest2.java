package com.alibaba.bytekit.asm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.annotation.AtInvoke;
import com.alibaba.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.alibaba.bytekit.utils.Decompiler;

public class AtInvokeTest2 {
    @Rule
    public OutputCapture capture = new OutputCapture();
    
    static class Sample {
        public void test() throws InterruptedException {
            Sample a = new Sample();
            
            for(int i = 0; i < 10000; ++i) {
                TimeUnit.SECONDS.sleep(1);
                
                a.hello("hello" , i);
            }
        }
        
        
        public String hello(String str, int i) {
            return "hello" + str + i;
        }
    }
    
    public static class TestPrintSuppressHandler {

        @ExceptionHandler(inline = false)
        public static void onSuppress(@Binding.Throwable Throwable e, @Binding.Class Object clazz) {
            System.err.println("exception handler: " + clazz);
            e.printStackTrace();
        }
    }
    
    public static class TestAccessInterceptor {
        @AtInvoke(name = "hello", inline = false, whenComplete=false, excludes = {"System."})
        public static void onInvoke(
                @Binding.This Object object,
                @Binding.Class Object clazz
                , 
                @Binding.Line int line,
                @Binding.InvokeArgs Object[] args
                ) {
            System.err.println("onInvoke: line: " + line);
            System.err.println("onInvoke: this: " + object);
        }
        
        @AtInvoke(name = "toBeCall", inline = false, whenComplete = true)
        public static void onInvokeAfter(
                @Binding.This Object object,
                @Binding.Class Object clazz
                , 
                @Binding.InvokeReturn Object invokeReturn
                ,
                @Binding.InvokeMethodDeclaration String declaration
                ) {
            System.err.println("onInvokeAfter: this" + object);
            System.err.println("declaration: " + declaration);
            assertThat(declaration).isEqualTo("long toBeCall(int, long, java.lang.String)");
            
            System.err.println("invokeReturn: " + invokeReturn);
            assertThat(invokeReturn).isEqualTo(100 + 123L);
        }
    }
    
    @Test
    // TODO fix com.alibaba.bytekit.asm.location.Location.InvokeLocation satck save
    public void testInvokeBefore() throws Exception {
        TestHelper helper = TestHelper.builder().interceptorClass(TestAccessInterceptor.class).methodMatcher("*")
                .redefine(true);
        byte[] bytes = helper.process(Sample.class);

        new Sample().test();

        System.err.println(Decompiler.decompile(bytes));

        assertThat(capture.toString()).contains("onInvoke: this");
    }


}

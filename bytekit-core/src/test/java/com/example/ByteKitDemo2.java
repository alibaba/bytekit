package com.example;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExit;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.Decompiler;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * @author jiaming.lf
 * @Description a simple demo for @AnEnter and @AtExit of ByteKit: calculate the running time of a function
 * @date 2022/11/14 14:49
 */
public class ByteKitDemo2 {
    public static class Sample {
        public void hello(String name) {
            System.out.println("Hello " + name + "!");
        }
    }

    public static class SampleInterceptor {
        private static long start;

        @AtEnter(inline = true)
        public static void atEnter() {
            start = System.currentTimeMillis();
        }

        @AtExit(inline = true)
        public static void atEit() {
            System.out.println(System.currentTimeMillis() - start);
        }
    }

    public static void main(String[] args) throws Exception {
        // Parse the defined Interceptor class and related annotations
        DefaultInterceptorClassParser interceptorClassParser = new DefaultInterceptorClassParser();
        List<InterceptorProcessor> processors = interceptorClassParser.parse(SampleInterceptor.class);

        // load bytecode
        ClassNode classNode = AsmUtils.loadClass(Sample.class);

        // Enhanced process of loaded bytecodes
        for (MethodNode methodNode : classNode.methods) {
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
            for (InterceptorProcessor interceptor : processors) {
                interceptor.process(methodProcessor);
            }
        }
        // Get the enhanced bytecode
        byte[] bytes = AsmUtils.toBytes(classNode);

        // View decompilation results
        System.out.println(Decompiler.decompile(bytes));
    }
}
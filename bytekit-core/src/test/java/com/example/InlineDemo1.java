package com.example;

import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.bytekit.utils.AgentUtils;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.Decompiler;
import com.alibaba.bytekit.utils.VerifyUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class InlineDemo1 {

    public static class SampleInterceptor {

        @AtEnter(inline = true
//                , suppressHandler = PrintExceptionSuppressHandler.class
                )
        public static void atEnter(
//                @Binding.This Object object,
//                                   @Binding.LocalVars Object[] vars,
                                   @Binding.LocalVarNames String[] varNames
//                                   ,
//                                   @Binding.MethodName String methodName
                
                ) {
            
//            System.out.println(varNames);
            
//            System.out.println("atEnter, vars: " + Arrays.asList(vars)+", vars len: "+vars.length);
//
//            Map<String, Object> localVarMap = new HashMap<String, Object>();
            
//            String[] ttt = varNames;
            System.out.println(varNames.length);
//            for (int i = 0; i < varNames.length; i++) {
//                localVarMap.put(varNames[i], vars[i]);
//            }
            
            System.out.println("hello");

//            System.out.println("local vars: "+localVarMap);
        }

    }

    public void testMain() throws Exception {
        AgentUtils.install();

        // 获取增强后的字节码
        byte[] bytes = this.enhanceClass(Sample.class, new String[]{"hello"}, SampleInterceptor.class);

        // 查看反编译结果
        System.out.println(Decompiler.decompile(bytes));

        //增强前
        System.out.println("Before reTransform ...");
        new Sample().hello("world", 50, 1.0 );
        
        VerifyUtils.asmVerify(bytes, true);

        // 通过 reTransform 增强类
        AgentUtils.reTransform(Sample.class, bytes);

        //测试结果
        System.out.println("After reTransform ...");
        new Sample().hello("world", 50, 1.0 );
    }

    protected byte[] enhanceClass(Class targetClass, String[] targetMethodNames, Class interceptorClass) throws Exception {
        // 解析定义的 Interceptor类 和相关的注解
        DefaultInterceptorClassParser interceptorClassParser = new DefaultInterceptorClassParser();
        List<InterceptorProcessor> processors = interceptorClassParser.parse(interceptorClass);

        // 加载字节码
        ClassNode classNode = AsmUtils.loadClass(targetClass);

        List<String> methodNameList = Arrays.asList(targetMethodNames);

        // 对加载到的字节码做增强处理
        for (MethodNode methodNode : classNode.methods) {
            if (methodNameList.contains(methodNode.name)) {
                MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
                for (InterceptorProcessor interceptor : processors) {
                    interceptor.process(methodProcessor);
                }
            }
        }

        // 获取增强后的字节码
        byte[] bytes = AsmUtils.toBytes(classNode);
        return bytes;
    }

    public static class PrintExceptionSuppressHandler {

        @ExceptionHandler(inline = true)
        public static void onSuppress(@Binding.Throwable Throwable e, @Binding.Class Object clazz) {
            System.out.println("exception handler: " + clazz);
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws Exception {
        new InlineDemo1().testMain();
    }

}

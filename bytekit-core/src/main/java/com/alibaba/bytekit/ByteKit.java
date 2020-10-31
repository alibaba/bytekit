package com.alibaba.bytekit;

import java.util.List;

import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.parser.InterceptorClassParser;
import com.alibaba.bytekit.asm.matcher.ClassMatcher;
import com.alibaba.bytekit.asm.matcher.MethodMatcher;

public class ByteKit {

    
    private ClassMatcher classMatcher;
    private MethodMatcher methodMatcher;
    
    private Class<?> interceptorClass;
    
    private InterceptorClassParser interceptorClassParser;
    
    private List<InterceptorProcessor>  interceptorProcessors;
    
    
    /**
     * 通过 classloader 扫描到 classloader 下面配置文件，再读取到具体的类。再读取出 .class 文件，再做出
     * @param classLoader
     */
    public static void registerInstrumentClass(ClassLoader classLoader) {
        
    }
    
    /**
     * 通过 classloader 扫描到 classloader 下面配置文件，再读取到具体的类。再读取出 .class 文件，再做出
     * @param classLoader
     */
    public static void registerInstrumentClass(ClassLoader classLoader, String className) {
        
    }
}


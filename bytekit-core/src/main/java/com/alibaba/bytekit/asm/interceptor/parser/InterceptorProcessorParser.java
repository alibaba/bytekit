package com.alibaba.bytekit.asm.interceptor.parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;

public interface InterceptorProcessorParser {

    public InterceptorProcessor parse(Method method, Annotation annotationOnMethod);
}

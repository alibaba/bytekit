package com.alibaba.bytekit.asm.interceptor.parser;

import java.util.List;

import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;

public interface InterceptorClassParser {

    public List<InterceptorProcessor> parse(Class<?> clazz);
}

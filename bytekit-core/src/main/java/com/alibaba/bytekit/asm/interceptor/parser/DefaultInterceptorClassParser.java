package com.alibaba.bytekit.asm.interceptor.parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.InterceptorParserHander;
import com.alibaba.bytekit.utils.InstanceUtils;
import com.alibaba.bytekit.utils.ReflectionUtils;
import com.alibaba.bytekit.utils.ReflectionUtils.MethodCallback;

public class DefaultInterceptorClassParser implements InterceptorClassParser {

    @Override
    public List<InterceptorProcessor> parse(Class<?> clazz) {
        final List<InterceptorProcessor> result = new ArrayList<InterceptorProcessor>();

        MethodCallback methodCallback = new MethodCallback() {

            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                for (Annotation onMethodAnnotation : method.getAnnotations()) {
                    for (Annotation onAnnotation : onMethodAnnotation.annotationType().getAnnotations()) {
                        if (InterceptorParserHander.class.isAssignableFrom(onAnnotation.annotationType())) {

                            if (!Modifier.isStatic(method.getModifiers())) {
                                throw new IllegalArgumentException("method must be static. method: " + method);
                            }

                            InterceptorParserHander handler = (InterceptorParserHander) onAnnotation;
                            InterceptorProcessorParser interceptorProcessorParser = InstanceUtils
                                    .newInstance(handler.parserHander());
                            InterceptorProcessor interceptorProcessor = interceptorProcessorParser.parse(method,
                                    onMethodAnnotation);
                            result.add(interceptorProcessor);
                        }
                    }
                }
            }

        };
        ReflectionUtils.doWithMethods(clazz, methodCallback);

        return result;
    }

}

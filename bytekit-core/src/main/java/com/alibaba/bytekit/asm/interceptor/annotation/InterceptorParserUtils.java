package com.alibaba.bytekit.asm.interceptor.annotation;

import java.lang.reflect.Method;
import java.util.List;

import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.InterceptorMethodConfig;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.location.LocationMatcher;
import com.alibaba.deps.org.objectweb.asm.Type;

public class InterceptorParserUtils {

    public static InterceptorProcessor createInterceptorProcessor(Method method, LocationMatcher locationMatcher) {
        return createInterceptorProcessor(method, locationMatcher, true);
    }

    public static InterceptorProcessor createInterceptorProcessor(Method method, LocationMatcher locationMatcher,
            boolean inline) {
        return createInterceptorProcessor(method, locationMatcher, inline, null, null);
    }

    public static InterceptorProcessor createInterceptorProcessor(Method method, LocationMatcher locationMatcher,
            boolean inline, Class<? extends Throwable> suppress, Class<?> suppressHandler) {

        InterceptorProcessor interceptorProcessor = new InterceptorProcessor(
                method.getDeclaringClass().getClassLoader());

        // locationMatcher
        interceptorProcessor.setLocationMatcher(locationMatcher);

        // interceptorMethodConfig
        InterceptorMethodConfig interceptorMethodConfig = new InterceptorMethodConfig();
        interceptorProcessor.setInterceptorMethodConfig(interceptorMethodConfig);
        interceptorMethodConfig.setOwner(Type.getInternalName(method.getDeclaringClass()));
        interceptorMethodConfig.setMethodName(method.getName());
        interceptorMethodConfig.setMethodDesc(Type.getMethodDescriptor(method));

        // inline
        interceptorMethodConfig.setInline(inline);

        // bindings
        List<Binding> bindings = BindingParserUtils.parseBindings(method);
        interceptorMethodConfig.setBindings(bindings);

        // errorHandlerMethodConfig
        if (suppress != null) {
            InterceptorMethodConfig errorHandlerMethodConfig = ExceptionHandlerUtils.errorHandlerMethodConfig(suppress,
                    suppressHandler);
            if (errorHandlerMethodConfig != null) {
                interceptorProcessor.setExceptionHandlerConfig(errorHandlerMethodConfig);
            }
        }

        return interceptorProcessor;
    }
}

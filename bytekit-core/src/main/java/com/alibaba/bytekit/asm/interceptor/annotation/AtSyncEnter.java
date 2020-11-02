package com.alibaba.bytekit.asm.interceptor.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.AtSyncEnter.SyncEnterInterceptorProcessorParser;
import com.alibaba.bytekit.asm.interceptor.parser.InterceptorProcessorParser;
import com.alibaba.bytekit.asm.location.LocationMatcher;
import com.alibaba.bytekit.asm.location.SyncLocationMatcher;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(ElementType.METHOD)
@InterceptorParserHander(parserHander = SyncEnterInterceptorProcessorParser.class)
public @interface AtSyncEnter {
    boolean inline() default true;

    Class<? extends Throwable> suppress() default None.class;

    Class<?> suppressHandler() default Void.class;

    int count() default -1;
    boolean whenComplete() default false;

    class SyncEnterInterceptorProcessorParser implements InterceptorProcessorParser {

        @Override
        public InterceptorProcessor parse(Method method, Annotation annotationOnMethod) {

            AtSyncEnter atSyncEnter = (AtSyncEnter) annotationOnMethod;

            LocationMatcher locationMatcher = new SyncLocationMatcher(Opcodes.MONITORENTER, atSyncEnter.count(), atSyncEnter.whenComplete());

            return InterceptorParserUtils.createInterceptorProcessor(method,
                    locationMatcher,
                    atSyncEnter.inline(),
                    atSyncEnter.suppress(),
                    atSyncEnter.suppressHandler());
        }

    }
}

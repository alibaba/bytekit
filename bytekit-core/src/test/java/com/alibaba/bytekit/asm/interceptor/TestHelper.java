package com.alibaba.bytekit.asm.interceptor;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.bytekit.utils.AgentUtils;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.MatchUtils;
import com.alibaba.bytekit.utils.VerifyUtils;

/**
 * 
 * @author hengyunabc
 *
 */
public class TestHelper {

    private Class<?> interceptorClass;

    private boolean redefine;

    private boolean reTransform;

    private String methodMatcher = "*";

    private boolean asmVerity = true;

    public static TestHelper builder() {
        return new TestHelper();
    }

    public TestHelper interceptorClass(Class<?> interceptorClass) {
        this.interceptorClass = interceptorClass;
        return this;
    }

    public TestHelper redefine(boolean redefine) {
        this.redefine = redefine;
        return this;
    }

    public TestHelper reTransform(boolean reTransform) {
        this.reTransform = reTransform;
        return this;
    }

    public TestHelper methodMatcher(String methodMatcher) {
        this.methodMatcher = methodMatcher;
        return this;
    }

    public byte[] process(Class<?> transform) throws Exception {
        DefaultInterceptorClassParser defaultInterceptorClassParser = new DefaultInterceptorClassParser();

        List<InterceptorProcessor> interceptorProcessors = defaultInterceptorClassParser.parse(interceptorClass);

        ClassNode classNode = AsmUtils.loadClass(transform);

        List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
        for (MethodNode methodNode : classNode.methods) {
            if (MatchUtils.wildcardMatch(methodNode.name, methodMatcher)) {
                matchedMethods.add(methodNode);
            }
        }

        for (MethodNode methodNode : matchedMethods) {
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
            for (InterceptorProcessor interceptor : interceptorProcessors) {
                interceptor.process(methodProcessor);
            }
        }

        byte[] bytes = AsmUtils.toBytes(classNode);
        if (asmVerity) {
            VerifyUtils.asmVerify(bytes);
        }

        if (redefine) {
            AgentUtils.redefine(transform, bytes);
        }

        if (reTransform) {
            AgentUtils.reTransform(transform, bytes);
        }

        return bytes;
    }
}

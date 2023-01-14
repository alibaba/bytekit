package com.alibaba.bytekit.asm.location;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.InterceptorParserUtils;
import com.alibaba.bytekit.asm.interceptor.annotation.None;
import com.alibaba.bytekit.utils.*;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LineBeforeLocationMatcherTest {

    @Rule
    public OutputCapture capture = new OutputCapture();

    public static class SpyLookInterceptor {

        public static void atLineBefore(@Binding.This Object target, @Binding.Class Class<?> clazz,
                                        @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args,
                                        @Binding.LineBefore int line,
                                        @Binding.LocalVars Object[] vars,
                                        @Binding.LocalVarNames String[] varNames) {
            System.out.println("atLineBefore-args:" + clazz + methodInfo + target + args + line + vars + varNames);
        }

    }

    static class LineBeforeSample {

        public int testLineBefore() {
            int varA = 0;
            int varB = 1;
            int varC = 2;
            int varT = varA + varB * varC;
            System.out.println("varT:"+varT);
            return varB * 2;
        }

    }


    @Test
    public void testMatched() throws Exception {
        beforeLine(46);
        assertThat(capture.toString()).contains("atLineBefore-args:");
    }

    @Test
    public void testUnMatched() throws Exception {
        beforeLine(1000);
        assertThat(capture.toString()).doesNotContain("atLineBefore-args:");
    }

    @Test
    public void testMethodExit() throws Exception {
        beforeLine(-1);
        assertThat(capture.toString()).contains("atLineBefore-args:");
    }

    private List<Location> beforeLine(int beforeLine) throws Exception {
        LineBeforeLocationMatcher locationMatcher = new LineBeforeLocationMatcher(beforeLine);
        Method method = ReflectionUtils.findMethod(SpyLookInterceptor.class, "atLineBefore",null);
        InterceptorProcessor lineBeforeInterceptorProcessor = InterceptorParserUtils.createInterceptorProcessor(method, locationMatcher, true, None.class, Void.class);

        ClassNode classNode = AsmUtils.loadClass(LineBeforeSample.class);

        String methodMatcher = "testLineBefore";
        List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
        for (MethodNode methodNode : classNode.methods) {
            if (MatchUtils.wildcardMatch(methodNode.name, methodMatcher)) {
                matchedMethods.add(methodNode);
            }
        }

        List<Location> locations = new LinkedList<Location>();
        for (MethodNode methodNode : matchedMethods) {
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
            locations.addAll(lineBeforeInterceptorProcessor.process(methodProcessor));
        }

        byte[] bytes = AsmUtils.toBytes(classNode);
        System.out.println(Decompiler.decompile(bytes));
        return locations;
    }

}

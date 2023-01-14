package com.alibaba.bytekit.asm.location;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.InterceptorParserUtils;
import com.alibaba.bytekit.asm.interceptor.annotation.None;
import com.alibaba.bytekit.asm.location.filter.GroupLocationFilter;
import com.alibaba.bytekit.asm.location.filter.InvokeCheckLocationFilter;
import com.alibaba.bytekit.asm.location.filter.LocationFilter;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.Decompiler;
import com.alibaba.bytekit.utils.MatchUtils;
import com.alibaba.bytekit.utils.ReflectionUtils;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LineBeforeLocationMatcherFilterTest {

    @Rule
    public OutputCapture capture = new OutputCapture();

    public static class SpyAPI {

        public static void atLineBefore(Class<?> clazz, String methodInfo, Object target, Object[] args, int line, Object[] vars, String[] varNames) {

        }
    }

    public static class SpyLookInterceptor {

        public static void atLineBefore(@Binding.This Object target, @Binding.Class Class<?> clazz,
                                        @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args,
                                        @Binding.LineBefore int line,
                                        @Binding.LocalVars Object[] vars,
                                        @Binding.LocalVarNames String[] varNames) {
            SpyAPI.atLineBefore(clazz, methodInfo, target, args, line, vars, varNames);
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


    /**
     * 搭配{@link LineBeforeLocationMatcher} 对 {@link Location#filtered} 进行验证
     * 暴露 match 和 filter 的结果 及 重复enhance的场景
     */
    @Test
    public void testRepeatEnhance() throws Exception {
        LineBeforeLocationMatcher locationMatcher = new LineBeforeLocationMatcher(59);
        Method method = ReflectionUtils.findMethod(SpyLookInterceptor.class, "atLineBefore",null);
        InterceptorProcessor lineBeforeInterceptorProcessor = InterceptorParserUtils.createInterceptorProcessor(method, locationMatcher, true, None.class, Void.class);

        GroupLocationFilter groupLocationFilter = new GroupLocationFilter();
        //当lookingLineNum>0,指定行的前面进行filter
        LocationFilter lineBeforeFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                "atLineBefore", LocationType.LINE);
        //lookingLineNum=-1,方法退出前进行filter
        LocationFilter methodExistFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                "atLineBefore", LocationType.EXIT);
        groupLocationFilter.addFilter(lineBeforeFilter);
        groupLocationFilter.addFilter(methodExistFilter);

        ClassNode classNode = AsmUtils.loadClass(LineBeforeSample.class);

        String methodMatcher = "testLineBefore";
        List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
        for (MethodNode methodNode : classNode.methods) {
            if (MatchUtils.wildcardMatch(methodNode.name, methodMatcher)) {
                matchedMethods.add(methodNode);
            }
        }

        new LinkedList<Location>();
        for (MethodNode methodNode : matchedMethods) {
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode, groupLocationFilter);
            //首次增强
            List<Location> locations = lineBeforeInterceptorProcessor.process(methodProcessor);
            //不为空则表示match到了
            Assertions.assertThat(locations).isNotEmpty();

            byte[] bytes = AsmUtils.toBytes(classNode);
            System.out.println(Decompiler.decompile(bytes));

            //只增强一个
            Assertions.assertThat(
                            AsmUtils.findMethodInsnNode(methodNode, Type.getInternalName(LineBeforeLocationMatcherFilterTest.SpyAPI.class), "atLineBefore"))
                    .size().isEqualTo(1);



            //再次增强
            locations = lineBeforeInterceptorProcessor.process(methodProcessor);
            //第二次依旧能match到
            Assertions.assertThat(locations).isNotEmpty();

            byte[] bytes2 = AsmUtils.toBytes(classNode);
            System.out.println(Decompiler.decompile(bytes2));

            //再次增强，因为是重复的，所以最终只有一条
            Assertions.assertThat(
                            AsmUtils.findMethodInsnNode(methodNode, Type.getInternalName(LineBeforeLocationMatcherFilterTest.SpyAPI.class), "atLineBefore"))
                    .size().isEqualTo(1);



        }
    }
}

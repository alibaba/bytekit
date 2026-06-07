package com.alibaba.bytekit.asm.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.binding.BindingContext;
import com.alibaba.bytekit.asm.binding.StackSaver;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExit;
import com.alibaba.bytekit.asm.interceptor.annotation.AtLine;
import com.alibaba.bytekit.asm.interceptor.annotation.ExceptionHandler;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.bytekit.asm.location.LineDuplicatePolicy;
import com.alibaba.bytekit.asm.location.LineLocationMatcher;
import com.alibaba.bytekit.asm.location.LineMode;
import com.alibaba.bytekit.asm.location.Location;
import com.alibaba.bytekit.asm.location.Location.LineLocation;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.VerifyUtils;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.InsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.IntInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.JumpInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.LabelNode;
import com.alibaba.deps.org.objectweb.asm.tree.LineNumberNode;
import com.alibaba.deps.org.objectweb.asm.tree.LocalVariableNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.deps.org.objectweb.asm.tree.VarInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.analysis.BasicValue;
import com.alibaba.deps.org.objectweb.asm.tree.analysis.Frame;

public class AtLineFrameAwareTest {

    private static final AtomicInteger STACK_LINE_HITS = new AtomicInteger();
    private static final AtomicInteger SUPPRESS_HITS = new AtomicInteger();
    private static final AtomicInteger CONCURRENT_HITS = new AtomicInteger();
    public static final AtomicInteger ORDERED_LINE_HITS = new AtomicInteger();
    private static volatile Object[] capturedLocalVars;
    private static volatile String[] capturedLocalVarNames;

    public static class ThrowingLineInterceptor {
        @AtLine(lines = { 100 }, inline = false, suppress = RuntimeException.class, suppressHandler = SuppressHandler.class)
        public static void atLine() {
            STACK_LINE_HITS.incrementAndGet();
            throw new RuntimeException("line callback failure");
        }
    }

    public static class SuppressHandler {
        @ExceptionHandler(inline = false)
        public static void onSuppress(@Binding.Throwable Throwable throwable) {
            SUPPRESS_HITS.incrementAndGet();
        }
    }

    public static class LocalVarsLineInterceptor {
        @AtLine(lines = { 100 }, inline = false)
        public static void atLine(@Binding.LocalVars Object[] localVars,
                @Binding.LocalVarNames String[] localVarNames) {
            capturedLocalVars = localVars;
            capturedLocalVarNames = localVarNames;
        }
    }

    public static class ConcurrentLineInterceptor {
        @AtLine(lines = { 100 }, inline = false)
        public static void atLine() {
            CONCURRENT_HITS.incrementAndGet();
        }
    }

    public static class OrderedEnterInterceptor {
        @AtEnter(inline = true)
        public static void atEnter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args) {
            orderedEnter(target, clazz, methodInfo, args);
        }
    }

    public static class OrderedExitInterceptor {
        @AtExit(inline = true)
        public static void atExit(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args, @Binding.Return Object returnObj) {
            orderedExit(target, clazz, methodInfo, args, returnObj);
        }
    }

    public static class OrderedExceptionExitInterceptor {
        @AtExceptionExit(inline = true)
        public static void atExceptionExit(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args,
                @Binding.Throwable Throwable throwable) {
            orderedExceptionExit(target, clazz, methodInfo, args, throwable);
        }
    }

    public static class OrderedLineInterceptor {
        @AtLine(lines = { 100 }, inline = true)
        public static void atLine() {
            ORDERED_LINE_HITS.incrementAndGet();
        }
    }

    public static class PrefixLineInterceptor {
        @AtLine(lines = { 99 }, inline = false)
        public static void atLine() {
        }
    }

    public static void orderedEnter(Object target, Class<?> clazz, String methodInfo, Object[] args) {
    }

    public static void orderedExit(Object target, Class<?> clazz, String methodInfo, Object[] args, Object returnObj) {
    }

    public static void orderedExceptionExit(Object target, Class<?> clazz, String methodInfo, Object[] args,
            Throwable throwable) {
    }

    @Test
    public void lineCallbackWithSuppressKeepsOriginalOperandStack() throws Exception {
        STACK_LINE_HITS.set(0);
        SUPPRESS_HITS.set(0);
        TransformResult result = transform(stackLineClass("StackLineWithSuppress"), ThrowingLineInterceptor.class);

        Object instance = newInstance(result.classNode.name, result.bytes);
        Object value = instance.getClass().getMethod("stackLine").invoke(instance);

        assertThat(result.locations).isEqualTo(1);
        assertThat(value).isEqualTo(7);
        assertThat(STACK_LINE_HITS.get()).isEqualTo(1);
        assertThat(SUPPRESS_HITS.get()).isEqualTo(1);
    }

    @Test
    public void frameAwareLocalVariablesSkipUnreadableSlots() throws Exception {
        capturedLocalVars = null;
        capturedLocalVarNames = null;
        TransformResult result = transform(localVariableClass("LocalVariableFrameAware"), LocalVarsLineInterceptor.class);

        Object instance = newInstance(result.classNode.name, result.bytes);
        Object value = instance.getClass().getMethod("readLocal").invoke(instance);

        assertThat(result.locations).isEqualTo(1);
        assertThat(value).isEqualTo(7);
        assertThat(capturedLocalVarNames).contains("this", "value");
        assertThat(capturedLocalVarNames).doesNotContain("stale");
        assertThat(capturedLocalVars).hasSameSizeAs(capturedLocalVarNames);
    }

    @Test
    public void frameAwareLineInstrumentationIsSafeUnderConcurrentInvocation() throws Exception {
        CONCURRENT_HITS.set(0);
        TransformResult result = transform(stackLineClass("StackLineConcurrent"), ConcurrentLineInterceptor.class);
        final Object instance = newInstance(result.classNode.name, result.bytes);
        final Method method = instance.getClass().getMethod("stackLine");
        int threadCount = 8;
        final int loops = 200;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threadCount);
        final AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        start.await();
                        for (int j = 0; j < loops; j++) {
                            if (!Integer.valueOf(7).equals(method.invoke(instance))) {
                                failures.incrementAndGet();
                            }
                        }
                    } catch (Throwable e) {
                        failures.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                }
            });
            thread.start();
        }

        start.countDown();
        done.await();

        assertThat(result.locations).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(0);
        assertThat(CONCURRENT_HITS.get()).isEqualTo(threadCount * loops);
    }

    @Test
    public void lineInstrumentationWorksAfterEnterExitInstrumentation() throws Exception {
        ORDERED_LINE_HITS.set(0);
        ClassNode classNode = lineAtMethodEntryClass("LineAfterNormalInterceptors");
        TransformResult result = transformInOrder(classNode, OrderedEnterInterceptor.class, OrderedExitInterceptor.class,
                OrderedExceptionExitInterceptor.class, OrderedLineInterceptor.class);

        Object instance = newInstance(result.classNode.name, result.bytes);
        Object value = instance.getClass().getMethod("calculate", int.class).invoke(instance, 3);

        assertThat(result.locations).isEqualTo(4);
        assertThat(value).isEqualTo(8);
        assertThat(ORDERED_LINE_HITS.get()).isEqualTo(1);
    }

    @Test
    public void lineMatcherIgnoresLineNumbersInsertedBeforeOriginalEnterInsn() throws Exception {
        ClassNode classNode = lineAtMethodEntryClass("LineIgnoresInsertedPrefix");
        MethodNode methodNode = findMethod(classNode, "calculate", "(I)I");
        MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
        insertPrefixLine(methodNode, methodProcessor.getEnterInsnNode(), 99);

        int locations = process(methodProcessor, PrefixLineInterceptor.class);

        assertThat(locations).isEqualTo(0);
    }

    @Test
    public void lineStackSaverInitializesTemporaryLocalsOnceUnderConcurrentAccess() throws Exception {
        ClassNode classNode = stackLineClass("LineStackSaverConcurrentInit");
        MethodNode methodNode = findMethod(classNode, "stackLine", "()I");
        BlockingLineStackMethodProcessor methodProcessor = new BlockingLineStackMethodProcessor(classNode, methodNode);
        List<Location> locations = new LineLocationMatcher(100).match(methodProcessor);
        assertThat(locations).hasSize(1);

        final Location location = locations.get(0);
        final StackSaver stackSaver = location.getStackSaver();
        final BindingContext bindingContext = new BindingContext(location, methodProcessor, stackSaver);
        final CountDownLatch firstCallerInside = new CountDownLatch(1);
        final CountDownLatch releaseFirstCaller = new CountDownLatch(1);
        final CountDownLatch firstDone = new CountDownLatch(1);
        final CountDownLatch secondDone = new CountDownLatch(1);
        final AtomicReference<Throwable> firstError = new AtomicReference<Throwable>();
        final AtomicReference<Throwable> secondError = new AtomicReference<Throwable>();
        methodProcessor.blockFirstLineStackVariable(firstCallerInside, releaseFirstCaller);

        Thread first = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    stackSaver.store(new InsnList(), bindingContext);
                } catch (Throwable e) {
                    firstError.set(e);
                } finally {
                    firstDone.countDown();
                }
            }
        });
        first.start();
        assertThat(firstCallerInside.await(5, TimeUnit.SECONDS)).isTrue();

        Thread second = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    stackSaver.load(new InsnList(), bindingContext);
                } catch (Throwable e) {
                    secondError.set(e);
                } finally {
                    secondDone.countDown();
                }
            }
        });
        second.start();
        secondDone.await(200, TimeUnit.MILLISECONDS);

        releaseFirstCaller.countDown();
        assertThat(firstDone.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(secondDone.await(5, TimeUnit.SECONDS)).isTrue();
        first.join();
        second.join();

        assertThat(firstError.get()).isNull();
        assertThat(secondError.get()).isNull();
        assertThat(methodProcessor.lineStackVariableCalls()).isEqualTo(1);
    }

    @Test
    public void rejectAfterControlFlowKeepsFirstOccurrenceAndRejectsLaterDuplicate() throws Exception {
        ClassNode classNode = duplicateLineAfterControlFlowClass("LineRejectAfterControlFlow");
        MethodNode methodNode = findMethod(classNode, "duplicateLine", "(I)I");
        MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);

        List<Location> locations = new LineLocationMatcher(LineMode.FRAME_AWARE,
                LineDuplicatePolicy.REJECT_AFTER_CONTROL_FLOW, 100).match(methodProcessor);

        assertThat(locations).hasSize(1);
        assertThat(locations.get(0).getInsnNode()).isSameAs(firstLineNumberNode(methodNode, 100));
    }

    @Test
    public void lineLocationTreatsUnknownStackTypeAsNoStackToSave() {
        LabelNode labelNode = new LabelNode();
        LineNumberNode lineNumberNode = new LineNumberNode(100, labelNode);
        Frame<BasicValue> frame = new Frame<BasicValue>(0, 1);
        frame.push(BasicValue.UNINITIALIZED_VALUE);

        LineLocation location = new LineLocation(lineNumberNode, 100, frame, 0);

        assertThat(location.isStackNeedSave()).isFalse();
    }

    @Test
    public void lineMatcherRestoresAnalysisLimitsWhenPreparationFails() {
        ClassNode classNode = stackLineClass("LineAnalysisLimitRestore");
        MethodNode methodNode = findMethod(classNode, "stackLine", "()I");
        MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
        int originMaxStack = methodNode.maxStack;
        int originMaxLocals = methodNode.maxLocals;
        methodNode.instructions = null;

        Throwable thrown = null;
        try {
            new LineLocationMatcher(100).match(methodProcessor);
        } catch (Throwable e) {
            thrown = e;
        }

        assertThat(thrown).isInstanceOf(NullPointerException.class);
        assertThat(methodNode.maxStack).isEqualTo(originMaxStack);
        assertThat(methodNode.maxLocals).isEqualTo(originMaxLocals);
    }

    private TransformResult transform(ClassNode classNode, Class<?> interceptorClass) throws Exception {
        return transformInOrder(classNode, interceptorClass);
    }

    private TransformResult transformInOrder(ClassNode classNode, Class<?>... interceptorClasses) throws Exception {
        DefaultInterceptorClassParser parser = new DefaultInterceptorClassParser();
        int locations = 0;
        for (MethodNode methodNode : classNode.methods) {
            if ("<init>".equals(methodNode.name)) {
                continue;
            }
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
            for (Class<?> interceptorClass : interceptorClasses) {
                locations += process(methodProcessor, interceptorClass, parser);
            }
        }
        byte[] bytes = AsmUtils.toBytes(classNode);
        VerifyUtils.asmVerify(bytes);
        return new TransformResult(classNode, bytes, locations);
    }

    private int process(MethodProcessor methodProcessor, Class<?> interceptorClass) throws Exception {
        return process(methodProcessor, interceptorClass, new DefaultInterceptorClassParser());
    }

    private int process(MethodProcessor methodProcessor, Class<?> interceptorClass,
            DefaultInterceptorClassParser parser) throws Exception {
        int locations = 0;
        List<InterceptorProcessor> processors = parser.parse(interceptorClass);
        for (InterceptorProcessor processor : processors) {
            locations += processor.process(methodProcessor).size();
        }
        return locations;
    }

    private ClassNode stackLineClass(String simpleName) {
        ClassNode classNode = newClassNode(simpleName);
        classNode.methods.add(defaultConstructor());

        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "stackLine", "()I", null, null);
        LabelNode start = new LabelNode();
        LabelNode line = new LabelNode();
        LabelNode end = new LabelNode();
        methodNode.instructions.add(start);
        methodNode.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 7));
        methodNode.instructions.add(line);
        methodNode.instructions.add(new LineNumberNode(100, line));
        methodNode.instructions.add(new VarInsnNode(Opcodes.ISTORE, 1));
        methodNode.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
        methodNode.instructions.add(end);
        methodNode.localVariables = new ArrayList<LocalVariableNode>();
        methodNode.localVariables.add(new LocalVariableNode("this", "L" + classNode.name + ";", null, start, end, 0));
        methodNode.localVariables.add(new LocalVariableNode("value", "I", null, line, end, 1));
        methodNode.maxStack = 1;
        methodNode.maxLocals = 2;
        classNode.methods.add(methodNode);
        return classNode;
    }

    private ClassNode localVariableClass(String simpleName) {
        ClassNode classNode = newClassNode(simpleName);
        classNode.methods.add(defaultConstructor());

        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "readLocal", "()I", null, null);
        LabelNode start = new LabelNode();
        LabelNode line = new LabelNode();
        LabelNode end = new LabelNode();
        methodNode.instructions.add(start);
        methodNode.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 7));
        methodNode.instructions.add(new VarInsnNode(Opcodes.ISTORE, 1));
        methodNode.instructions.add(line);
        methodNode.instructions.add(new LineNumberNode(100, line));
        methodNode.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
        methodNode.instructions.add(end);
        methodNode.localVariables = new ArrayList<LocalVariableNode>();
        methodNode.localVariables.add(new LocalVariableNode("this", "L" + classNode.name + ";", null, start, end, 0));
        methodNode.localVariables.add(new LocalVariableNode("value", "I", null, start, end, 1));
        methodNode.localVariables.add(new LocalVariableNode("stale", "I", null, start, end, 2));
        methodNode.maxStack = 1;
        methodNode.maxLocals = 3;
        classNode.methods.add(methodNode);
        return classNode;
    }

    private ClassNode lineAtMethodEntryClass(String simpleName) {
        ClassNode classNode = newClassNode(simpleName);
        classNode.methods.add(defaultConstructor());

        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "calculate", "(I)I", null, null);
        LabelNode start = new LabelNode();
        LabelNode secondLine = new LabelNode();
        LabelNode end = new LabelNode();
        methodNode.instructions.add(start);
        methodNode.instructions.add(new LineNumberNode(100, start));
        methodNode.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        methodNode.instructions.add(new InsnNode(Opcodes.ICONST_1));
        methodNode.instructions.add(new InsnNode(Opcodes.IADD));
        methodNode.instructions.add(new VarInsnNode(Opcodes.ISTORE, 2));
        methodNode.instructions.add(secondLine);
        methodNode.instructions.add(new LineNumberNode(101, secondLine));
        methodNode.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        methodNode.instructions.add(new InsnNode(Opcodes.ICONST_2));
        methodNode.instructions.add(new InsnNode(Opcodes.IMUL));
        methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
        methodNode.instructions.add(end);
        methodNode.localVariables = new ArrayList<LocalVariableNode>();
        methodNode.localVariables.add(new LocalVariableNode("this", "L" + classNode.name + ";", null, start, end, 0));
        methodNode.localVariables.add(new LocalVariableNode("value", "I", null, start, end, 1));
        methodNode.localVariables.add(new LocalVariableNode("sum", "I", null, secondLine, end, 2));
        methodNode.maxStack = 2;
        methodNode.maxLocals = 3;
        classNode.methods.add(methodNode);
        return classNode;
    }

    private ClassNode duplicateLineAfterControlFlowClass(String simpleName) {
        ClassNode classNode = newClassNode(simpleName);
        classNode.methods.add(defaultConstructor());

        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "duplicateLine", "(I)I", null, null);
        LabelNode start = new LabelNode();
        LabelNode duplicateLine = new LabelNode();
        LabelNode end = new LabelNode();
        methodNode.instructions.add(start);
        methodNode.instructions.add(new LineNumberNode(100, start));
        methodNode.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        methodNode.instructions.add(new JumpInsnNode(Opcodes.IFLT, duplicateLine));
        methodNode.instructions.add(new InsnNode(Opcodes.ICONST_1));
        methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
        methodNode.instructions.add(duplicateLine);
        methodNode.instructions.add(new LineNumberNode(100, duplicateLine));
        methodNode.instructions.add(new InsnNode(Opcodes.ICONST_2));
        methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
        methodNode.instructions.add(end);
        methodNode.localVariables = new ArrayList<LocalVariableNode>();
        methodNode.localVariables.add(new LocalVariableNode("this", "L" + classNode.name + ";", null, start, end, 0));
        methodNode.localVariables.add(new LocalVariableNode("value", "I", null, start, end, 1));
        methodNode.maxStack = 1;
        methodNode.maxLocals = 2;
        classNode.methods.add(methodNode);
        return classNode;
    }

    private ClassNode newClassNode(String simpleName) {
        ClassNode classNode = new ClassNode();
        classNode.version = Opcodes.V1_8;
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.name = "com/alibaba/bytekit/asm/interceptor/generated/" + simpleName;
        classNode.superName = "java/lang/Object";
        classNode.methods = new ArrayList<MethodNode>();
        return classNode;
    }

    private MethodNode defaultConstructor() {
        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V",
                false));
        methodNode.instructions.add(new InsnNode(Opcodes.RETURN));
        methodNode.maxStack = 1;
        methodNode.maxLocals = 1;
        return methodNode;
    }

    private MethodNode findMethod(ClassNode classNode, String name, String desc) {
        for (MethodNode methodNode : classNode.methods) {
            if (name.equals(methodNode.name) && desc.equals(methodNode.desc)) {
                return methodNode;
            }
        }
        throw new IllegalArgumentException("method not found: " + name + desc);
    }

    private void insertPrefixLine(MethodNode methodNode, AbstractInsnNode enterInsnNode, int line) {
        LabelNode prefix = new LabelNode();
        InsnList instructions = new InsnList();
        instructions.add(prefix);
        instructions.add(new LineNumberNode(line, prefix));
        methodNode.instructions.insertBefore(enterInsnNode, instructions);
    }

    private LineNumberNode firstLineNumberNode(MethodNode methodNode, int line) {
        for (AbstractInsnNode insnNode = methodNode.instructions.getFirst(); insnNode != null; insnNode = insnNode
                .getNext()) {
            if (insnNode instanceof LineNumberNode && ((LineNumberNode) insnNode).line == line) {
                return (LineNumberNode) insnNode;
            }
        }
        throw new IllegalArgumentException("line not found: " + line);
    }

    private Object newInstance(String internalName, byte[] bytes) throws Exception {
        Class<?> clazz = new TestClassLoader().define(internalName.replace('/', '.'), bytes);
        return clazz.newInstance();
    }

    private static class TransformResult {
        private final ClassNode classNode;
        private final byte[] bytes;
        private final int locations;

        private TransformResult(ClassNode classNode, byte[] bytes, int locations) {
            this.classNode = classNode;
            this.bytes = bytes;
            this.locations = locations;
        }
    }

    private static class TestClassLoader extends ClassLoader {
        private TestClassLoader() {
            super(AtLineFrameAwareTest.class.getClassLoader());
        }

        private Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    private static class BlockingLineStackMethodProcessor extends MethodProcessor {
        private CountDownLatch firstCallerInside;
        private CountDownLatch releaseFirstCaller;
        private final AtomicInteger lineStackVariableCalls = new AtomicInteger();

        private BlockingLineStackMethodProcessor(ClassNode classNode, MethodNode methodNode) {
            super(classNode, methodNode);
        }

        private void blockFirstLineStackVariable(CountDownLatch firstCallerInside,
                CountDownLatch releaseFirstCaller) {
            this.firstCallerInside = firstCallerInside;
            this.releaseFirstCaller = releaseFirstCaller;
        }

        @Override
        public LocalVariableNode initLineStackVariableNode(String name, Type type) {
            if (lineStackVariableCalls.incrementAndGet() == 1) {
                firstCallerInside.countDown();
                try {
                    releaseFirstCaller.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
            return super.initLineStackVariableNode(name, type);
        }

        private int lineStackVariableCalls() {
            return lineStackVariableCalls.get();
        }
    }
}

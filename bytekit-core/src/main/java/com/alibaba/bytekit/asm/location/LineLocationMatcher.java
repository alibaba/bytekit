package com.alibaba.bytekit.asm.location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.location.Location.LineLocation;
import com.alibaba.bytekit.asm.location.filter.LocationFilter;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.IincInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.JumpInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.LineNumberNode;
import com.alibaba.deps.org.objectweb.asm.tree.LookupSwitchInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.deps.org.objectweb.asm.tree.TableSwitchInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.VarInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.analysis.Analyzer;
import com.alibaba.deps.org.objectweb.asm.tree.analysis.AnalyzerException;
import com.alibaba.deps.org.objectweb.asm.tree.analysis.BasicInterpreter;
import com.alibaba.deps.org.objectweb.asm.tree.analysis.BasicValue;
import com.alibaba.deps.org.objectweb.asm.tree.analysis.BasicVerifier;
import com.alibaba.deps.org.objectweb.asm.tree.analysis.Frame;

public class LineLocationMatcher implements LocationMatcher {

    private static final int ANALYSIS_MAX_STACK_HEADROOM = 64;

    private List<Integer> targetLines = Collections.emptyList();
    private LineMode mode;
    private LineDuplicatePolicy duplicatePolicy;

    public LineLocationMatcher(int... targetLines) {
        this(LineMode.FRAME_AWARE, LineDuplicatePolicy.DEFAULT, targetLines);
    }

    public LineLocationMatcher(List<Integer> targetLines) {
        this(LineMode.FRAME_AWARE, LineDuplicatePolicy.DEFAULT, targetLines);
    }

    public LineLocationMatcher(LineMode mode, LineDuplicatePolicy duplicatePolicy, int... targetLines) {
        this.mode = mode == null ? LineMode.FRAME_AWARE : mode;
        this.duplicatePolicy = duplicatePolicy == null ? LineDuplicatePolicy.DEFAULT : duplicatePolicy;
        if (targetLines != null) {
            ArrayList<Integer> result = new ArrayList<Integer>(targetLines.length);
            for (int targetLine : targetLines) {
                result.add(targetLine);
            }
            this.targetLines = result;
        }
    }

    public LineLocationMatcher(LineMode mode, LineDuplicatePolicy duplicatePolicy, List<Integer> targetLines) {
        this.mode = mode == null ? LineMode.FRAME_AWARE : mode;
        this.duplicatePolicy = duplicatePolicy == null ? LineDuplicatePolicy.DEFAULT : duplicatePolicy;
        if (targetLines != null) {
            this.targetLines = targetLines;
        }
    }

    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        List<Location> locations = new ArrayList<Location>();
        LocationFilter locationFilter = methodProcessor.getLocationFilter();
        LineDuplicatePolicy effectivePolicy = effectiveDuplicatePolicy();

        LineFrames lineFrames = null;
        if (mode == LineMode.FRAME_AWARE) {
            lineFrames = analyze(methodProcessor);
            if (lineFrames == null) {
                return locations;
            }
        }

        AbstractInsnNode insnNode = startInsnNode(methodProcessor);
        int lastLine = Integer.MIN_VALUE;
        boolean controlFlowSinceLastLine = true;
        int locationIndex = 0;
        Set<Integer> matchedLines = new HashSet<Integer>();
        Set<Integer> seenTargetLines = new HashSet<Integer>();
        Map<Integer, Boolean> controlFlowAfterLine = new HashMap<Integer, Boolean>();
        while (insnNode != null) {
            if (insnNode instanceof LineNumberNode) {
                LineNumberNode lineNumberNode = (LineNumberNode) insnNode;
                boolean lineBlockBoundary = lineNumberNode.line != lastLine || controlFlowSinceLastLine;
                if (match(lineNumberNode.line)) {
                    boolean rejectAfterControlFlow = effectivePolicy == LineDuplicatePolicy.REJECT_AFTER_CONTROL_FLOW
                            && seenTargetLines.contains(lineNumberNode.line)
                            && Boolean.TRUE.equals(controlFlowAfterLine.get(lineNumberNode.line));
                    if (!rejectAfterControlFlow
                            && allowByDuplicatePolicy(effectivePolicy, matchedLines, lineNumberNode.line,
                                    lineBlockBoundary)
                            && locationFilter.allow(lineNumberNode, LocationType.LINE, false)) {
                        if (mode == LineMode.FRAME_AWARE) {
                            Frame<BasicValue> frame = frameOf(methodProcessor.getMethodNode(), lineFrames.frames,
                                    lineNumberNode);
                            if (isFrameUsable(frame, lineFrames.precise)) {
                                locations.add(new LineLocation(lineNumberNode, lineNumberNode.line, frame,
                                        locationIndex++));
                            }
                        } else {
                            locations.add(new LineLocation(lineNumberNode, lineNumberNode.line));
                        }
                    }
                    seenTargetLines.add(lineNumberNode.line);
                    controlFlowAfterLine.put(lineNumberNode.line, Boolean.FALSE);
                }
                lastLine = lineNumberNode.line;
                controlFlowSinceLastLine = false;
            } else if (isControlFlow(insnNode)) {
                controlFlowSinceLastLine = true;
                for (Integer line : controlFlowAfterLine.keySet()) {
                    controlFlowAfterLine.put(line, Boolean.TRUE);
                }
            }
            insnNode = insnNode.getNext();
        }

        return locations;
    }

    private boolean match(int line) {
        for (int targetLine : targetLines) {
            if (targetLine == -1) {
                return true;
            } else if (line == targetLine) {
                return true;
            }

        }
        return false;
    }

    private LineDuplicatePolicy effectiveDuplicatePolicy() {
        if (duplicatePolicy != LineDuplicatePolicy.DEFAULT) {
            return duplicatePolicy;
        }
        return mode == LineMode.LEGACY ? LineDuplicatePolicy.ALL : LineDuplicatePolicy.FIRST_PER_LINE_BLOCK;
    }

    private AbstractInsnNode startInsnNode(MethodProcessor methodProcessor) {
        return methodProcessor.getEnterInsnNode();
    }

    private boolean allowByDuplicatePolicy(LineDuplicatePolicy policy, Set<Integer> matchedLines, int line,
            boolean lineBlockBoundary) {
        if (policy == LineDuplicatePolicy.ALL) {
            return true;
        }
        if (policy == LineDuplicatePolicy.FIRST) {
            return matchedLines.add(line);
        }
        if (policy == LineDuplicatePolicy.REJECT_AFTER_CONTROL_FLOW) {
            return true;
        }
        if (policy == LineDuplicatePolicy.FIRST_PER_LINE_BLOCK) {
            return lineBlockBoundary;
        }
        return true;
    }

    private LineFrames analyze(MethodProcessor methodProcessor) {
        MethodNode methodNode = methodProcessor.getMethodNode();
        int originMaxStack = methodNode.maxStack;
        int originMaxLocals = methodNode.maxLocals;
        try {
            prepareAnalysisLimits(methodNode);
            Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(new BasicVerifier());
            return new LineFrames(analyzer.analyze(methodProcessor.getOwner(), methodNode), true);
        } catch (AnalyzerException e) {
            try {
                Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(new BasicInterpreter());
                return new LineFrames(analyzer.analyze(methodProcessor.getOwner(), methodNode), false);
            } catch (AnalyzerException ignored) {
                return null;
            }
        } finally {
            methodNode.maxStack = originMaxStack;
            methodNode.maxLocals = originMaxLocals;
        }
    }

    private void prepareAnalysisLimits(MethodNode methodNode) {
        // transform 期 ClassWriter 还没重新计算 maxStack，前序 inline advice 可能让内存中的
        // MethodNode 临时超过原 maxStack。这里仅给 ASM Analyzer 预留保守余量，finally 会恢复原值。
        methodNode.maxStack = methodNode.maxStack + ANALYSIS_MAX_STACK_HEADROOM;
        methodNode.maxLocals = Math.max(methodNode.maxLocals, requiredMaxLocals(methodNode));
    }

    private int requiredMaxLocals(MethodNode methodNode) {
        int maxLocals = (methodNode.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
        Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
        for (Type argumentType : argumentTypes) {
            maxLocals += argumentType.getSize();
        }
        for (AbstractInsnNode insnNode = methodNode.instructions.getFirst(); insnNode != null; insnNode = insnNode
                .getNext()) {
            if (insnNode instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) insnNode;
                maxLocals = Math.max(maxLocals, varInsnNode.var + localSize(varInsnNode.getOpcode()));
            } else if (insnNode instanceof IincInsnNode) {
                IincInsnNode iincInsnNode = (IincInsnNode) insnNode;
                maxLocals = Math.max(maxLocals, iincInsnNode.var + 1);
            }
        }
        return maxLocals;
    }

    private int localSize(int opcode) {
        return opcode == Opcodes.LLOAD || opcode == Opcodes.LSTORE || opcode == Opcodes.DLOAD
                || opcode == Opcodes.DSTORE ? 2 : 1;
    }

    private Frame<BasicValue> frameOf(MethodNode methodNode, Frame<BasicValue>[] frames, AbstractInsnNode insnNode) {
        int index = methodNode.instructions.indexOf(insnNode);
        if (index < 0 || index >= frames.length) {
            return null;
        }
        return frames[index];
    }

    private boolean isFrameUsable(Frame<BasicValue> frame, boolean precise) {
        if (frame == null) {
            return false;
        }
        if (!precise && frame.getStackSize() > 0) {
            return false;
        }
        for (int i = 0; i < frame.getStackSize(); i++) {
            BasicValue value = frame.getStack(i);
            if (value == null || value == BasicValue.UNINITIALIZED_VALUE
                    || value == BasicValue.RETURNADDRESS_VALUE) {
                return false;
            }
            Type type = value.getType();
            if (type == null || type == Type.VOID_TYPE || type.getSort() == Type.METHOD) {
                return false;
            }
        }
        return true;
    }

    private boolean isControlFlow(AbstractInsnNode insnNode) {
        int opcode = insnNode.getOpcode();
        return insnNode instanceof JumpInsnNode || insnNode instanceof TableSwitchInsnNode
                || insnNode instanceof LookupSwitchInsnNode || opcode == Opcodes.ATHROW
                || (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN);
    }

    private static class LineFrames {
        private Frame<BasicValue>[] frames;
        private boolean precise;

        private LineFrames(Frame<BasicValue>[] frames, boolean precise) {
            this.frames = frames;
            this.precise = precise;
        }
    }

}

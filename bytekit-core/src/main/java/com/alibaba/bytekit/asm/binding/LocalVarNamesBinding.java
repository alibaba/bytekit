package com.alibaba.bytekit.asm.binding;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.bytekit.utils.MatchUtils;
import com.alibaba.deps.org.objectweb.asm.tree.analysis.BasicValue;
import com.alibaba.deps.org.objectweb.asm.tree.analysis.Frame;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.deps.org.objectweb.asm.tree.LocalVariableNode;
import com.alibaba.bytekit.utils.AsmOpUtils;
import com.alibaba.bytekit.asm.location.Location.LineLocation;

public class LocalVarNamesBinding extends Binding {

    private String excludePattern;
    /**
     * 是否在变量中忽略掉 this
     */
    private boolean ignoreThis;

    public LocalVarNamesBinding(String excludePattern, boolean ignoreThis) {
        this.excludePattern = excludePattern;
        this.ignoreThis = ignoreThis;
    }

    public LocalVarNamesBinding() {
    }

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        AbstractInsnNode currentInsnNode = bindingContext.getLocation().getInsnNode();

        List<LocalVariableNode> rawLocalVariables = bindingContext.getMethodProcessor().getMethodNode().localVariables;
        List<LocalVariableNode> localVariables = rawLocalVariables == null ? new LinkedList<LocalVariableNode>()
                : new LinkedList<LocalVariableNode>(rawLocalVariables);
        if (excludePattern != null && !excludePattern.isEmpty()){
            Iterator<LocalVariableNode> it = localVariables.iterator();
            while(it.hasNext()){
                LocalVariableNode localVariableNode = it.next();
                if (MatchUtils.wildcardMatch(localVariableNode.name,excludePattern)) {
                    it.remove();
                }
                if (ignoreThis && localVariableNode.name.equals("this")) {
                    it.remove();
                }
            }
        }

        List<LocalVariableNode> results = AsmOpUtils.validVariables(localVariables, currentInsnNode,
                currentFrame(bindingContext));

        AsmOpUtils.push(instructions, results.size());
        AsmOpUtils.newArray(instructions, AsmOpUtils.STRING_TYPE);

        for (int i = 0; i < results.size(); ++i) {
            AsmOpUtils.dup(instructions);

            AsmOpUtils.push(instructions, i);
            AsmOpUtils.push(instructions, results.get(i).name);

            AsmOpUtils.arrayStore(instructions, AsmOpUtils.STRING_TYPE);
        }
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return AsmOpUtils.STRING_ARRAY_TYPE;
    }

    public String getExcludePattern() {
        return excludePattern;
    }

    public void setExcludePattern(String excludePattern) {
        this.excludePattern = excludePattern;
    }

    private Frame<BasicValue> currentFrame(BindingContext bindingContext) {
        if (bindingContext.getLocation() instanceof LineLocation) {
            return ((LineLocation) bindingContext.getLocation()).getFrame();
        }
        return null;
    }
}

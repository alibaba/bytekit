package com.alibaba.bytekit.asm.binding;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.bytekit.utils.MatchUtils;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.deps.org.objectweb.asm.tree.LocalVariableNode;
import com.alibaba.bytekit.utils.AsmOpUtils;

public class LocalVarNamesBinding extends Binding {

    private String excludePattern;

    public LocalVarNamesBinding(String excludePattern) {
        this.excludePattern = excludePattern;
    }

    public LocalVarNamesBinding() {
    }

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        AbstractInsnNode currentInsnNode = bindingContext.getLocation().getInsnNode();

        List<LocalVariableNode> localVariables = new LinkedList<LocalVariableNode>(bindingContext.getMethodProcessor().getMethodNode().localVariables);
        if (excludePattern != null && !excludePattern.isEmpty()){
            Iterator<LocalVariableNode> it = localVariables.iterator();
            while(it.hasNext()){
                LocalVariableNode localVariableNode = it.next();
                if (MatchUtils.wildcardMatch(localVariableNode.name,excludePattern)) it.remove();
            }
        }

        List<LocalVariableNode> results = AsmOpUtils.validVariables(localVariables, currentInsnNode);

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
}

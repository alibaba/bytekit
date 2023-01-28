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

/**
 * TODO 增加一个配置，是否包含 method args
 * @author hengyunabc
 *
 */
public class LocalVarsBinding extends Binding{

    private String excludePattern;

    public LocalVarsBinding(String excludePattern) {
        this.excludePattern = excludePattern;
    }

    public LocalVarsBinding() {
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
        AsmOpUtils.newArray(instructions, AsmOpUtils.OBJECT_TYPE);

        for (int i = 0; i < results.size(); ++i) {
            AsmOpUtils.dup(instructions);

            AsmOpUtils.push(instructions, i);

            LocalVariableNode variableNode = results.get(i);
            AsmOpUtils.loadVar(instructions, Type.getType(variableNode.desc), variableNode.index);
            AsmOpUtils.box(instructions, Type.getType(variableNode.desc));

            AsmOpUtils.arrayStore(instructions, AsmOpUtils.OBJECT_TYPE);
        }

    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return AsmOpUtils.OBJECT_ARRAY_TYPE;
    }

    public String getExcludePattern() {
        return excludePattern;
    }

    public void setExcludePattern(String excludePattern) {
        this.excludePattern = excludePattern;
    }
}

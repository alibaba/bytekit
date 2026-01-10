package com.alibaba.bytekit.asm.binding;

import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.deps.org.objectweb.asm.tree.LineNumberNode;
import com.alibaba.bytekit.utils.AsmOpUtils;

/**
 * Binding for method last line number
 * @author lbs
 *
 */
public class MethodLastLineBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        int line = -1;
        LineNumberNode lastLineNumberNode = null;
        
        // Start from the first instruction of the method
        AbstractInsnNode insnNode = bindingContext.getMethodProcessor().getEnterInsnNode();
        
        // Find the last LineNumberNode
        while (insnNode != null) {
            if (insnNode instanceof LineNumberNode) {
                lastLineNumberNode = (LineNumberNode) insnNode;
            }
            insnNode = insnNode.getNext();
        }
        
        if (lastLineNumberNode != null) {
            line = lastLineNumberNode.line;
        }
        
        AsmOpUtils.push(instructions, line);
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(int.class);
    }

}

package com.alibaba.bytekit.asm.binding;

import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.deps.org.objectweb.asm.tree.LineNumberNode;
import com.alibaba.bytekit.utils.AsmOpUtils;

/**
 * Binding for method first line number
 * @author lbs
 *
 */
public class MethodFirstLineBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        int line = -1;
        
        // Start from the very first instruction of the method
        AbstractInsnNode insnNode = bindingContext.getMethodProcessor().getMethodNode().instructions.getFirst();
        
        // Find the first LineNumberNode
        while (insnNode != null) {
            if (insnNode instanceof LineNumberNode) {
                line = ((LineNumberNode) insnNode).line;
                break;
            }
            insnNode = insnNode.getNext();
        }
        
        AsmOpUtils.push(instructions, line);
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(int.class);
    }

}

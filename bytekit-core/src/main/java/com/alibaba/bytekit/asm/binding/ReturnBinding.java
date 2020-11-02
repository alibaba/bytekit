package com.alibaba.bytekit.asm.binding;

import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.deps.org.objectweb.asm.tree.LocalVariableNode;
import com.alibaba.bytekit.asm.location.Location;
import com.alibaba.bytekit.utils.AsmOpUtils;

public class ReturnBinding extends Binding {


    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        //check location
        
        Location location = bindingContext.getLocation();

        if (!AsmOpUtils.isReturnCode(location.getInsnNode().getOpcode())) {
            throw new IllegalArgumentException("current location is not return location. location: " + location);
        }
        
        Type returnType = bindingContext.getMethodProcessor().getReturnType();
        if(returnType.equals(Type.VOID_TYPE)) {
            AsmOpUtils.push(instructions, null);
        }else {
            LocalVariableNode returnVariableNode = bindingContext.getMethodProcessor().initReturnVariableNode();
            AsmOpUtils.loadVar(instructions, returnType, returnVariableNode.index);
        }
        
    }

    @Override
    public boolean fromStack() {
        return true;
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return bindingContext.getMethodProcessor().getReturnType();
    }
    
}

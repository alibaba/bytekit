package com.alibaba.bytekit.asm.binding;

import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.bytekit.utils.AsmOpUtils;

public class ClassBinding extends Binding{

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        String owner = bindingContext.getMethodProcessor().getOwner();
        AsmOpUtils.ldc(instructions, Type.getObjectType(owner));
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(Class.class);
    }

}

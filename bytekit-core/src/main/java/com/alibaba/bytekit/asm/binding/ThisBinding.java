package com.alibaba.bytekit.asm.binding;

import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;

public class ThisBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        bindingContext.getMethodProcessor().loadThis(instructions);
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(Object.class);
    }

}

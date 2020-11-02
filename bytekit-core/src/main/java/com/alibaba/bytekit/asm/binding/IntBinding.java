package com.alibaba.bytekit.asm.binding;

import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.bytekit.utils.AsmOpUtils;

public class IntBinding extends Binding {

    private int value;

    private boolean box = true;

    public IntBinding(int value) {
        this(value, true);
    }

    public IntBinding(int value, boolean box) {
        this.value = value;
        this.box = box;
    }

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        AsmOpUtils.push(instructions, value);
        if (box) {
            AsmOpUtils.box(instructions, Type.INT_TYPE);
        }
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.INT_TYPE;
    }

}

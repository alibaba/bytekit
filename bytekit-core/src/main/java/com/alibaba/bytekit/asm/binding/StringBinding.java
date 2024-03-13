package com.alibaba.bytekit.asm.binding;

import com.alibaba.bytekit.utils.AsmOpUtils;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;

/**
 * 字符串的Binding
 * 参照了 {@link IntBinding}
 */
public class StringBinding extends Binding {

    private String value;


    public StringBinding(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 提供value覆盖入口
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        AsmOpUtils.push(instructions, value);
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(String.class);
    }

}

package com.alibaba.bytekit.asm.binding;

import com.alibaba.bytekit.asm.location.Location;
import com.alibaba.bytekit.utils.AsmOpUtils;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;

public class LineBeforeBinding extends Binding {

    public LineBeforeBinding() {

    }

    /**
     * 匹配指定的{@link LineBeforeLocation}使用
     * support Location.LineBeforeLocation only
     */
    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        Location location = bindingContext.getLocation();
        if (location instanceof Location.LineBeforeLocation){
            int targetLine = ((Location.LineBeforeLocation) location).getBeforeLine();
            AsmOpUtils.push(instructions, targetLine);
        }else {
            throw new IllegalArgumentException("support Location.LineBeforeLocation only!");
        }
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(int.class);
    }

}

package com.alibaba.bytekit.asm.binding;

import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.utils.AsmOpUtils;

/**
 * @author hengyunabc
 *
 */
public class MethodNameBinding extends Binding {

	@Override
	public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
		MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
		AsmOpUtils.ldc(instructions, methodProcessor.getMethodNode().name);
	}

	@Override
	public Type getType(BindingContext bindingContext) {
		return Type.getType(String.class);
	}

}

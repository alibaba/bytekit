package com.alibaba.bytekit.asm.binding;

import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.FieldInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.utils.AsmOpUtils;

/**
 * @author hengyunabc
 *
 */
public class MethodBinding  extends Binding{

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        // 先获取类本身的 class ，再调用 getDeclaredMethod ，它需要一个变长参数，实际上要传一个数组
        /**
         * @see java.lang.Class.getDeclaredMethod(String, Class<?>...)
         */
        MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
        AsmOpUtils.ldc(instructions, Type.getObjectType(methodProcessor.getOwner()));
        
        AsmOpUtils.push(instructions, methodProcessor.getMethodNode().name);
        
        Type[] argumentTypes = Type.getMethodType(methodProcessor.getMethodNode().desc).getArgumentTypes();
        
        AsmOpUtils.push(instructions, argumentTypes.length);
        AsmOpUtils.newArray(instructions, AsmOpUtils.CLASS_TYPE);

        for(int i = 0; i < argumentTypes.length; ++i) {
            AsmOpUtils.dup(instructions);

            AsmOpUtils.push(instructions, i);

            if (AsmOpUtils.needBox(argumentTypes[i])) {
                // 相当于 Boolean.TYPE;
                AsmOpUtils.getStatic(instructions, AsmOpUtils.getBoxedType(argumentTypes[i]), "TYPE",
                        AsmOpUtils.CLASS_TYPE);
            } else {
                AsmOpUtils.ldc(instructions, argumentTypes[i]);
            }

            AsmOpUtils.arrayStore(instructions, AsmOpUtils.CLASS_TYPE);
        }
        
        MethodInsnNode declaredMethodInsnNode = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, AsmOpUtils.CLASS_TYPE.getInternalName(),
                "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
        instructions.add(declaredMethodInsnNode);
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(java.lang.reflect.Method.class);
    }

}

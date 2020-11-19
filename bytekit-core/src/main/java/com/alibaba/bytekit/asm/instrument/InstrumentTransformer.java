package com.alibaba.bytekit.asm.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

import com.alibaba.bytekit.asm.inst.impl.InstrumentImpl;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;

/**
 * 
 * @author hengyunabc 2020-11-13
 *
 */
public class InstrumentTransformer implements ClassFileTransformer {

    private InstrumentParseResult instrumentParseResult;

    public InstrumentTransformer(InstrumentParseResult instrumentParseResult) {
        this.instrumentParseResult = instrumentParseResult;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        List<InstrumentConfig> instrumentConfigs = instrumentParseResult.getInstrumentConfigs();

        ClassNode originClassNode = null;
        ClassNode targetClassNode = null;
        for (InstrumentConfig config : instrumentConfigs) {
            if (config.getClassMatcher().match(loader, className, classBeingRedefined, protectionDomain,
                    classfileBuffer)) {
                if (originClassNode == null) {
                    originClassNode = AsmUtils.toClassNode(classfileBuffer);
                    targetClassNode = AsmUtils.copy(originClassNode);
                }

                // 匹配上，则进行字节码替换处理
                ClassNode instrumentClassNode = config.getInstrumentClassNode();

                // TODO 如果名字一样，则不用修改？
                byte[] renameClass = AsmUtils.renameClass(AsmUtils.toBytes(instrumentClassNode),
                        Type.getObjectType(originClassNode.name).getClassName());

                instrumentClassNode = AsmUtils.toClassNode(renameClass);

                // 查找 @Instrument 字节码里的 method，如果在原来的有同样的，则处理替换；如果没有，则复制过去
                for (MethodNode methodNode : instrumentClassNode.methods) {

                    // 只处理非 abstract函数
                    if (AsmUtils.isAbstract(methodNode)) {
                        continue;
                    }

                    // 不处理构造函数
                    if (AsmUtils.isConstructor(methodNode)) {
                        continue;
                    }

                    // 从原来的类里查找对应的函数
                    MethodNode findMethod = AsmUtils.findMethod(originClassNode.methods, methodNode);
                    if (findMethod != null) {
                        MethodNode updatedMethodNode = InstrumentImpl.replaceInvokeOrigin(originClassNode.name,
                                findMethod, methodNode);

                        AsmUtils.replaceMethod(targetClassNode, updatedMethodNode);
                    } else {
                        // 没找到对应的，则复制函数过去
                        AsmUtils.addMethod(targetClassNode, methodNode);
                    }
                }
                // 处理@NewField

            }
        }

        if (targetClassNode != null) {
            byte[] resutlBytes = AsmUtils.toBytes(targetClassNode);
            return resutlBytes;
        }

        return null;
    }

}

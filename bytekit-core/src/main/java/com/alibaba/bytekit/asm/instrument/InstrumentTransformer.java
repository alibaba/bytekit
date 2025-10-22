package com.alibaba.bytekit.asm.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.bytekit.asm.inst.impl.InstrumentImpl;
import com.alibaba.bytekit.log.Logger;
import com.alibaba.bytekit.log.Loggers;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.ReflectUtils;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;

/**
 * 
 * @author hengyunabc 2020-11-13
 *
 */
public class InstrumentTransformer implements ClassFileTransformer {
    private final Logger logger = Loggers.getLogger(getClass());
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
        ClassReader classReader = null;
        for (InstrumentConfig config : instrumentConfigs) {
            if (config.getClassMatcher().match(loader, className, classBeingRedefined, protectionDomain,
                    classfileBuffer)) {

                if (originClassNode == null) {
                    originClassNode = new ClassNode(Opcodes.ASM9);
                    classReader = AsmUtils.toClassNode(classfileBuffer, originClassNode);
                    // fix https://github.com/alibaba/one-java-agent/issues/51
                    originClassNode = AsmUtils.removeJSRInstructions(originClassNode);
                    targetClassNode = AsmUtils.copy(originClassNode);
                }

                // 匹配上，则进行字节码替换处理
                ClassNode instrumentClassNode = AsmUtils.copy(config.getInstrumentClassNode());

                if(config.isUpdateMajorVersion()) {
                    AsmUtils.updateMajorVersion(instrumentClassNode, targetClassNode);
                }

                // 如果 @Instrument 的字节码的类名 和 目标字节码的类名不一样，则修改为一致
                if (!originClassNode.name.equals(instrumentClassNode.name)) {
                    instrumentClassNode = AsmUtils.renameClass(instrumentClassNode, originClassNode.name);
                }

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
                        updatedMethodNode.access = findMethod.access;
                        AsmUtils.replaceMethod(targetClassNode, updatedMethodNode);
                    } else {
                        // TODO 有一些特别注解标记的函数，并且在原来类里没找到对应的，则复制函数过去
                        // 不能全部复制，比如匹配到一个接口，但已经父类里实现了接口
                        // AsmUtils.addMethod(targetClassNode, methodNode);
                    }
                }
                // 处理@NewField

            }
        }

        if (targetClassNode != null) {
            // TODO 支持 bootstrap classloader?
            if (loader != null) {
                // 收集所有匹配的 instrument config 关联的 define 类
                List<DefineConfig> allDefineConfigs = new ArrayList<DefineConfig>();
                for (InstrumentConfig config : instrumentConfigs) {
                    if (config.getClassMatcher().match(loader, className, classBeingRedefined, protectionDomain,
                            classfileBuffer)) {
                        allDefineConfigs.addAll(config.getDefineConfigs());
                    }
                }
                
                // 兼容旧格式：添加全局的 define 配置（已废弃）
                List<DefineConfig> globalDefineConfigs = instrumentParseResult.getDefineConfigs();
                if (globalDefineConfigs != null && !globalDefineConfigs.isEmpty()) {
                    allDefineConfigs.addAll(globalDefineConfigs);
                }
                
                // 定义所有关联的类
                for (DefineConfig defineConfig : allDefineConfigs) {
                    try {
                        ReflectUtils.defineClass(defineConfig.getClassName(), defineConfig.getClassBytes(), loader);
                    } catch (Throwable e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof LinkageError) {
                            String errorMessage = cause.getMessage();
                            if (errorMessage != null && errorMessage.contains("duplicate class definition")) {
                                // ignore
                                continue;
                            }
                        }
                        if (logger.isInfoEnabled()) {
                            logger.info("transform class: " + className + " error! can not define class: "
                                    + defineConfig.getClassName(), e);
                        }
                        return null;
                    }
                }
            }

            AsmUtils.fixMajorVersion(targetClassNode);
            byte[] resutlBytes = AsmUtils.toBytes(targetClassNode, loader, classReader);
            logger.info("transform class: " + className + " success!");
            return resutlBytes;
        }

        return null;
    }

}

package com.alibaba.bytekit.asm.meta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.deps.org.objectweb.asm.ClassReader;

/**
 * 从字节码 byte[] 中提取类的元信息
 * 
 * @author hengyunabc 2020-11-20
 *
 */
public class ClassMeta {
    /**
     * internal name 是 java/lang/String 的形式
     */
    private String internalClassName;
    /**
     * internal name 是 java/lang/String 的形式
     */
    private String internalSuperName;
    private Set<String> interfaces = new HashSet<String>();

    public ClassMeta() {

    }

    public ClassMeta(String internalClassName, String internalSuperName, String[] interfaces) {
        this.internalClassName = internalClassName;
        this.internalSuperName = internalSuperName;
        if (interfaces != null) {
            for (String i : interfaces) {
                this.interfaces.add(i);
            }
        }
    }

    public ClassMeta(String internalClassName, String internalSuperName, Set<String> interfaces) {
        this.internalClassName = internalClassName;
        this.internalSuperName = internalSuperName;
        this.interfaces = interfaces;
    }

    public static ClassMeta fromByteCode(byte[] classfileBuffer) {
        if (classfileBuffer == null) {
            return null;
        }
        ClassReader reader = new ClassReader(classfileBuffer);
        String clazzName = reader.getClassName();
        String superName = reader.getSuperName();
        String[] interfacesArray = reader.getInterfaces();

        return new ClassMeta(clazzName, superName, interfacesArray);
    }

    /**
     * 查找所有的 interface ，包含多重继承的
     * 
     * @param classMetaCache
     * @return
     */
    public Set<String> allInterfaces(ClassMetaCache classMetaCache) {
        // TODO 每次都要向上多次查询，需要缓存一次结果不？

        Set<String> result = new HashSet<String>(interfaces);
        // 从 super 类里查找
        if (internalSuperName != null) {
            ClassMeta classMeta = classMetaCache.findClassMeta(internalSuperName);
            if (classMeta != null) {
                result.addAll(classMeta.allInterfaces(classMetaCache));
            }
        }

        // 从实现的 interface里查找
        for (String interfaceName : interfaces) {
            ClassMeta classMeta = classMetaCache.findClassMeta(interfaceName);
            if (classMeta != null) {
                result.addAll(classMeta.allInterfaces(classMetaCache));
            }
        }
        return result;
    }

    public List<String> allSuperNames(ClassMetaCache classMetaCache) {
        List<String> result = new ArrayList<String>();

        String superName = internalSuperName;
        for (; superName != null;) {
            result.add(superName);
            ClassMeta classMeta = classMetaCache.findClassMeta(superName);
            if (classMeta == null) {
                break;
            } else {
                superName = classMeta.getInternalSuperName();
            }
        }

        return result;
    }

    public String getInternalClassName() {
        return internalClassName;
    }

    public void setInternalClassName(String internalClassName) {
        this.internalClassName = internalClassName;
    }

    public String getInternalSuperName() {
        return internalSuperName;
    }

    public void setInternalSuperName(String internalSuperName) {
        this.internalSuperName = internalSuperName;
    }

    public Set<String> getInterfaces() {
        return interfaces;
    }

}

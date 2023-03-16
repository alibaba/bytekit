package com.alibaba.bytekit.asm;

import com.alibaba.bytekit.asm.meta.ClassMeta;
import com.alibaba.bytekit.utils.ClassLoaderUtils;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.ClassWriter;

/**
 * 基于 ClassMeta 实现的 ClassWriter，避免加载类
 * 
 * @author hengyunabc 2023-03-16
 *
 */
public class ClassMetaClassWriter extends ClassWriter {
    private ClassLoader classLoader;

    public ClassMetaClassWriter(int flags, ClassLoader loader) {
        this(null, flags, loader);
    }

    public ClassMetaClassWriter(ClassReader classReader, int flags, ClassLoader loader) {
        super(classReader, flags);
        // TODO 对 null loader的处理是否准确
        this.classLoader = ClassLoaderUtils.wrap(loader);
    }

    /*
     * 注意，为了自动计算帧的大小，有时必须计算两个类共同的父类。
     * 缺省情况下，ClassWriter将会在getCommonSuperClass方法中计算这些，通过在加载这两个类进入虚拟机时，使用反射API来计算。
     * 但是，如果你将要生成的几个类相互之间引用，这将会带来问题，因为引用的类可能还不存在。
     * 在这种情况下，你可以重写getCommonSuperClass方法来解决这个问题。
     *
     * 采用尝试用 ClassLoader#getResource 的方式来读取 .class 文件，再计算 CommonSuperClass
     */
    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        byte[] type1Bytes = ClassLoaderUtils.readBytecodeByName(classLoader, type1);
        if (type1Bytes == null) {
            return super.getCommonSuperClass(type1, type2);
        }
        byte[] type2Bytes = ClassLoaderUtils.readBytecodeByName(classLoader, type2);
        if (type2Bytes == null) {
            return super.getCommonSuperClass(type1, type2);
        }
        ClassMeta classMeta1 = ClassMeta.fromByteCode(type1Bytes, classLoader);
        ClassMeta classMeta2 = ClassMeta.fromByteCode(type2Bytes, classLoader);

        if (classMeta1.isAssignableFrom(classMeta2)) {
            return type1;
        }
        if (classMeta2.isAssignableFrom(classMeta1)) {
            return type2;
        }
        if (classMeta1.isInterface() || classMeta2.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                classMeta1 = classMeta1.getSuperclass();
            } while (!classMeta1.isAssignableFrom(classMeta2));
            return classMeta1.getInternalClassName();
        }
    }
}

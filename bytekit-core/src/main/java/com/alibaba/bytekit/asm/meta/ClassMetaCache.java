package com.alibaba.bytekit.asm.meta;

import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.bytekit.utils.ClassLoaderUtils;

/**
 * 每个ClassLoader对应一个 ClassMetaCache
 * 
 * @author hengyunabc 2020-11-24
 *
 */
public class ClassMetaCache {
    /**
     * 标记已查找过的类，避免重复查找
     */
    private static final ClassMeta NO_EXIST_CLASSMETA = new ClassMeta();

    private final ConcurrentHashMap<String, ClassMeta> classMetaMap = new ConcurrentHashMap<String, ClassMeta>();

    private ClassLoader classLoader;

    public ClassMetaCache(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void addClassMeta(String internalClassName, ClassMeta meta) {
        classMetaMap.put(internalClassName, meta);
    }

    public ClassMeta findClassMeta(String internalClassName) {
        ClassMeta classMeta = classMetaMap.get(internalClassName);
        if (classMeta == NO_EXIST_CLASSMETA) {
            return null;
        }
        if (classMeta == null) {
            byte[] bytes = ClassLoaderUtils.readBytecodeByName(classLoader, internalClassName);
            if (bytes != null) {
                classMeta = ClassMeta.fromByteCode(bytes);
                ClassMeta existed = classMetaMap.putIfAbsent(internalClassName, classMeta);
                if (existed != null) {
                    classMeta = existed;
                }
            } else {
                classMetaMap.put(internalClassName, NO_EXIST_CLASSMETA);
                return null;
            }
        }

        return classMeta;
    }

    public ClassMeta findAndTryLoadClassMeta(String internalClassName, byte[] classfileBuffer) {
        ClassMeta classMeta = null;
        if (internalClassName != null) {
            classMeta = classMetaMap.get(internalClassName);
        }

        /**
         * 可能之前查找失败被标记为 NO_EXIST_CLASSMETA ，如果显式传入字节码，则从字节码里提取
         */
        if (classMeta == null || classMeta == NO_EXIST_CLASSMETA) {
            classMeta = ClassMeta.fromByteCode(classfileBuffer);
            ClassMeta existed = classMetaMap.putIfAbsent(classMeta.getInternalClassName(), classMeta);
            if (existed != null) {
                classMeta = existed;
            }
        }
        return classMeta;
    }
}

package com.alibaba.bytekit.asm.meta;

import java.util.List;
import java.util.Set;

import com.alibaba.bytekit.utils.ClassLoaderUtils;
import com.alibaba.bytekit.utils.concurrent.ConcurrentWeakKeyHashMap;

/**
 * 记录所有类的元信息，用于判断是否实现了接口，是否子类
 * 
 * <pre>
 * 1. 尝试找到某个类的，如果是 Object，或者基本类型，则忽略
 * 2. 如果是一个新的类，向上找到它的父类，父的 interface ，那么要把对应的信息补完到服务里
 * </pre>
 * 
 * @author hengyunabc 2020-11-20
 *
 */
public class ClassMetaService {

    private static final ConcurrentWeakKeyHashMap<ClassLoader, ClassMetaCache> cacheMap = new ConcurrentWeakKeyHashMap<ClassLoader, ClassMetaCache>();

    public static ClassMetaCache findClassMetaCache(ClassLoader classLoader) {
        classLoader = ClassLoaderUtils.wrap(classLoader);
        ClassMetaCache classMetaCache = cacheMap.get(classLoader);
        if (classMetaCache == null) {
            cacheMap.putIfAbsent(classLoader, new ClassMetaCache(classLoader));
            classMetaCache = cacheMap.get(classLoader);
        }
        return classMetaCache;
    }

    /**
     * 从指定 classloader查找类所有实现的 interface。基于从ClassLoader里查找 .class 文件机制
     * 
     * @param loader
     * @param internalClassName
     * @param classfileBuffer
     * @return
     */
    public static Set<String> allInterfaces(ClassLoader loader, String internalClassName, byte[] classfileBuffer) {
        ClassMetaCache classMetaCache = ClassMetaService.findClassMetaCache(loader);
        ClassMeta classMeta = classMetaCache.findAndTryLoadClassMeta(internalClassName, classfileBuffer, loader);
        return classMeta.allInterfaces(classMetaCache);
    }

    public static List<String> allSuperNames(ClassLoader loader, String internalClassName, byte[] classfileBuffer) {
        ClassMetaCache classMetaCache = ClassMetaService.findClassMetaCache(loader);
        ClassMeta classMeta = classMetaCache.findAndTryLoadClassMeta(internalClassName, classfileBuffer, loader);
        return classMeta.allSuperNames(classMetaCache);
    }
}
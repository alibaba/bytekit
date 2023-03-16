package com.alibaba.bytekit.asm.meta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.bytekit.utils.ClassLoaderUtils;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.Opcodes;

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

    private ClassMeta superClassMeta;
    private List<ClassMeta> interfaceClassMetas = null;

    private int access;

    private ClassLoader classLoader;

    public ClassMeta() {

    }

    public ClassMeta(byte[] classfileBuffer, ClassLoader classLoader) {
        ClassReader reader = new ClassReader(classfileBuffer);
        this.internalClassName = reader.getClassName();
        this.internalSuperName = reader.getSuperName();
        String[] interfacesArray = reader.getInterfaces();

        if (interfacesArray != null) {
            for (String i : interfacesArray) {
                this.interfaces.add(i);
            }
        }
        this.access = reader.getAccess();
        this.classLoader = ClassLoaderUtils.wrap(classLoader);
    }

    public static ClassMeta fromByteCode(byte[] classfileBuffer, ClassLoader classLoader) {
        if (classfileBuffer == null) {
            return null;
        }
        return new ClassMeta(classfileBuffer, classLoader);
    }

    public boolean isInterface() {
        return (access & Opcodes.ACC_INTERFACE) > 0;
    }

    public ClassMeta getSuperclass() {
        if (this.internalSuperName == null) {
            return null;
        }
        if (superClassMeta == null) {
            byte[] classBytes = ClassLoaderUtils.readBytecodeByName(classLoader, internalSuperName);
            if (classBytes == null) {
                // TODO 打印error?
                return null;
            }
            superClassMeta = new ClassMeta(classBytes, classLoader);
        }
        return superClassMeta;
    }

    private boolean isSubclassOf(ClassMeta other) {
        for (ClassMeta c = this; c != null; c = c.getSuperclass()) {
            ClassMeta sc = c.getSuperclass();
            if (sc != null && sc.internalClassName.equals(other.internalClassName)) {
                return true;
            }
        }
        return false;
    }

    List<ClassMeta> interfaceMetas() {
        if (this.interfaceClassMetas == null) {
            interfaceClassMetas = new ArrayList<ClassMeta>();
            for (String interfaceName : interfaces) {
                byte[] classBytes = ClassLoaderUtils.readBytecodeByName(classLoader, interfaceName);
                if (classBytes == null) {
                    continue;
                }
                ClassMeta meta = new ClassMeta(classBytes, classLoader);
                interfaceClassMetas.add(meta);
            }
        }
        return interfaceClassMetas;
    }

    private boolean implementsInterface(ClassMeta other) {
        if (this.equals(other)) {
            return true;
        }

        for (ClassMeta c = this; c != null; c = c.getSuperclass()) {
            for (ClassMeta in : c.interfaceMetas()) {
                if (in.internalClassName.equals(other.internalClassName) || in.implementsInterface(other)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAssignableFrom(ClassMeta other) {
        return this.equals(other) || other.implementsInterface(this) || other.isSubclassOf(this)
                || (other.isInterface() && this.internalClassName.equals("Ljava/lang/Object;"));
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((internalClassName == null) ? 0 : internalClassName.hashCode());
        result = prime * result + ((internalSuperName == null) ? 0 : internalSuperName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO 是否要比较 classLoader?
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClassMeta other = (ClassMeta) obj;
        if (internalClassName == null) {
            if (other.internalClassName != null)
                return false;
        } else if (!internalClassName.equals(other.internalClassName))
            return false;
        if (internalSuperName == null) {
            if (other.internalSuperName != null)
                return false;
        } else if (!internalSuperName.equals(other.internalSuperName))
            return false;
        return true;
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

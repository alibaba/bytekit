package com.alibaba.bytekit.asm.matcher;

import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.bytekit.asm.meta.ClassMetaService;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.ClassLoaderUtils;

/**
 * 
 * @author hengyunabc 2020-11-24
 *
 */
public class SimpleSubclassMatcher implements ClassMatcher {

    private Set<String> classNames = new HashSet<String>();

    /**
     * 保存另一份转换为 internal 的数据，避免每次match转换
     */
    private Set<String> internalClassNames = new HashSet<String>();

    public SimpleSubclassMatcher(String... className) {
        for (String name : className) {
            add(name);
        }
    }

    public SimpleSubclassMatcher(Collection<String> names) {
        for (String name : names) {
            add(name);
        }
    }

    private void add(String name) {
        classNames.add(name);
        internalClassNames.add(AsmUtils.internalClassName(name));
    }

    @Override
    public boolean match(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        loader = ClassLoaderUtils.wrap(loader);

        if (classBeingRedefined != null) { // 在retransform 时，可以直接判断
            return match(classBeingRedefined);
        } else {

            List<String> allSuperNames = ClassMetaService.allSuperNames(loader, className, classfileBuffer);
            for (String superName : allSuperNames) {
                if (internalClassNames.contains(superName)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean match(Class<?> clazz) {
        for (Class<?> superclass = clazz;;) {
            if (superclass == null || Object.class.equals(superclass)) {
                return false;
            }
            if (classNames != null && classNames.contains(superclass.getName().replace('/', '.'))) {
                return true;
            }
            superclass = superclass.getSuperclass();
        }
    }

}

package com.alibaba.bytekit.asm.meta;

import java.util.List;
import java.util.Set;
import java.util.logging.LogRecord;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.ClassLoaderUtils;

public class ClassMetaServiceTest {

    interface A {

    }

    interface B extends A, java.util.logging.Filter {

    }

    interface C extends B, A {

    }

    class TestClass implements C {

        @Override
        public boolean isLoggable(LogRecord record) {
            return false;
        }

    }

    @Test
    public void testExistClass() {

        Set<String> allInterfaces = ClassMetaService.allInterfaces(this.getClass().getClassLoader(),
                AsmUtils.internalClassName(TestClass.class), ClassLoaderUtils.readBytecode(TestClass.class));

        Assertions.assertThat(allInterfaces)
                .containsOnlyOnce(AsmUtils.internalClassName(java.util.logging.Filter.class))
                .containsOnlyOnce(AsmUtils.internalClassName(A.class))
                .containsOnlyOnce(AsmUtils.internalClassName(B.class))
                .containsOnlyOnce(AsmUtils.internalClassName(C.class));

        Set<String> allInterfaces2 = ClassMetaService.allInterfaces(this.getClass().getClassLoader(),
                AsmUtils.internalClassName(SSS.class), ClassLoaderUtils.readBytecode(SSS.class));

        Assertions.assertThat(allInterfaces2).isEmpty();
    }

    @Test
    public void testNoExistClass() {
        ClassMetaCache classMetaCache = ClassMetaService.findClassMetaCache(this.getClass().getClassLoader());
        ClassMeta classMeta = classMetaCache.findClassMeta("com/no/exist");
        Assertions.assertThat(classMeta).isNull();
    }

    abstract class S {

    }

    class SS extends S {

    }

    class SSS extends SS {

    }

    @Test
    public void testSuperClass() {
        List<String> allSuperNames = ClassMetaService.allSuperNames(this.getClass().getClassLoader(),
                AsmUtils.internalClassName(SSS.class), ClassLoaderUtils.readBytecode(SSS.class));

        Assertions.assertThat(allSuperNames).containsOnlyOnce(AsmUtils.internalClassName(SS.class))
                .containsOnlyOnce(AsmUtils.internalClassName(S.class))
                .containsOnlyOnce(AsmUtils.internalClassName(Object.class));

        List<String> allSuperNames2 = ClassMetaService.allSuperNames(this.getClass().getClassLoader(),
                AsmUtils.internalClassName(TestClass.class), ClassLoaderUtils.readBytecode(TestClass.class));

        Assertions.assertThat(allSuperNames2).containsExactly(AsmUtils.internalClassName(Object.class));
    }
}

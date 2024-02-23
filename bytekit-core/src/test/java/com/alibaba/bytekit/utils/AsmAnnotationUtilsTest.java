package com.alibaba.bytekit.utils;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.stereotype.Service;

import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.bytekit.utils.AsmAnnotationUtils;
import com.alibaba.bytekit.utils.AsmUtils;

/**
 * 
 * @author hengyunabc 2020-05-04
 *
 */
public class AsmAnnotationUtilsTest {

    @Target(value = { ElementType.TYPE, ElementType.METHOD })
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface AdviceInfo {

        public String[] adviceInfos();

        public String name() default "";
    }

    @Service
    @AdviceInfo(adviceInfos = { "xxxx", "yyy" }, name = "testAdvice")
    static class AAA {

        @AdviceInfo(adviceInfos = { "mmm", "yyy" })
        public void test() {

        }

    }

    @Service
    static class BBB {
        public void test() {
        }
    }

    @Test
    public void test() throws IOException {
        ClassNode classNodeA = AsmUtils.loadClass(AAA.class);

        ClassNode classNodeB = AsmUtils.loadClass(BBB.class);

        String name = AsmAnnotationUtils.queryAnnotationValue(classNodeA.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "name");
        Assertions.assertThat(name).isEqualTo("testAdvice");

        List<String> names = AsmAnnotationUtils.queryAnnotationValues(classNodeA.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "name");
        Assertions.assertThat(names).containsExactly("testAdvice");

        List<String> adviceInfos = AsmAnnotationUtils.queryAnnotationValue(classNodeA.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "adviceInfos");
        Assertions.assertThat(adviceInfos).containsExactly("xxxx", "yyy");

        Assertions.assertThat((BigDecimal)AsmAnnotationUtils.queryAnnotationValue(classNodeA.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "notExist")).isNull();

        List<List<String>> allAdviceInfos = AsmAnnotationUtils.queryAnnotationValues(classNodeA.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "adviceInfos");

        Assertions.assertThat(allAdviceInfos).size().isEqualTo(1);
        Assertions.assertThat(allAdviceInfos.get(0)).containsExactly("xxxx", "yyy");

        Assertions.assertThat(AsmAnnotationUtils.queryAnnotationArrayValue(classNodeA.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "notExist")).isNotNull().isEmpty();

        Assertions.assertThat(AsmAnnotationUtils.queryAnnotationArrayValue(classNodeA.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "adviceInfos")).isEqualTo(Arrays.asList("xxxx", "yyy"));

        AsmAnnotationUtils.addAnnotationInfo(classNodeA.visibleAnnotations, Type.getDescriptor(AdviceInfo.class),
                "adviceInfos", "fff");

        Assertions
                .assertThat(AsmAnnotationUtils.queryAnnotationArrayValue(classNodeA.visibleAnnotations,
                        Type.getDescriptor(AdviceInfo.class), "adviceInfos"))
                .isEqualTo(Arrays.asList("xxxx", "yyy", "fff"));

        Assertions.assertThat(AsmAnnotationUtils.queryAnnotationArrayValue(classNodeB.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "adviceInfos")).isEmpty();

        AsmAnnotationUtils.addAnnotationInfo(classNodeB.visibleAnnotations, Type.getDescriptor(AdviceInfo.class),
                "adviceInfos", "fff");

        Assertions.assertThat(AsmAnnotationUtils.queryAnnotationArrayValue(classNodeB.visibleAnnotations,
                Type.getDescriptor(AdviceInfo.class), "adviceInfos")).isEqualTo(Arrays.asList("fff"));

    }

}

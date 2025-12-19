package com.alibaba.bytekit.asm.instrument;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;

import com.alibaba.bytekit.asm.inst.AbstractAutowireCapableBeanFactory_Instrument;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.ClassLoaderUtils;
import com.alibaba.bytekit.utils.VerifyUtils;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;

/**
 * 覆盖 @Instrument(updateMajorVersion) 的默认值解析逻辑。
 */
public class UpdateMajorVersionDefaultTest {

    @Test
    public void testUpdateMajorVersionDefaultValue() throws Exception {
        byte[] instrumentBytes = ClassLoaderUtils.readBytecode(AbstractAutowireCapableBeanFactory_Instrument.class);
        InstrumentTemplate instrumentTemplate = new InstrumentTemplate();
        instrumentTemplate.addInstrumentClass(instrumentBytes);

        InstrumentParseResult parseResult = instrumentTemplate.build();
        assertThat(parseResult.getInstrumentConfigs()).hasSize(1);

        InstrumentConfig config = parseResult.getInstrumentConfigs().get(0);
        assertThat(config.isUpdateMajorVersion()).isTrue();

        Class<?> targetClass = AbstractAutowireCapableBeanFactory.class;
        byte[] originBytes = ClassLoaderUtils.readBytecode(targetClass);

        ClassNode originNode = AsmUtils.toClassNode(originBytes);
        int originMajorVersion = AsmUtils.getMajorVersion(originNode.version);
        assertThat(originMajorVersion).isLessThan(52);

        InstrumentTransformer transformer = new InstrumentTransformer(parseResult);
        byte[] transformedBytes = transformer.transform(targetClass.getClassLoader(),
                targetClass.getName().replace('.', '/'), targetClass, targetClass.getProtectionDomain(), originBytes);
        assertThat(transformedBytes).isNotNull();

        ClassNode transformedNode = AsmUtils.toClassNode(transformedBytes);
        int transformedMajorVersion = AsmUtils.getMajorVersion(transformedNode.version);
        assertThat(transformedMajorVersion).isGreaterThanOrEqualTo(52);

        // 触发 JVM 校验，避免出现 VerifyError: Illegal type at constant pool entry
        java.net.URL[] urls = ClassLoaderUtils.getUrls(ClassLoader.getSystemClassLoader());
        if (urls != null) {
            VerifyUtils.ClassbyteClassLoader cl = new VerifyUtils.ClassbyteClassLoader(urls,
                    ClassLoader.getSystemClassLoader().getParent());
            cl.addClass(targetClass.getName(), transformedBytes);
        }
    }
}

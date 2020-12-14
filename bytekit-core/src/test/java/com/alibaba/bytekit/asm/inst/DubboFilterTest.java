package com.alibaba.bytekit.asm.inst;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.dubbo.rpc.filter.ConsumerContextFilter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.rule.OutputCapture;
import org.zeroturnaround.zip.ZipUtil;

import com.alibaba.bytekit.asm.instrument.InstrumentParseResult;
import com.alibaba.bytekit.asm.instrument.InstrumentTemplate;
import com.alibaba.bytekit.asm.instrument.InstrumentTransformer;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.JavaVersionUtils;
import com.alibaba.bytekit.utils.VerifyUtils;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;

/**
 * 
 * @author hengyunabc 2020-11-27
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DubboFilterTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public OutputCapture capture = new OutputCapture();

    private Object object;

    @Before
    public void beforeMethod() {
        // dubbo need jdk8
        org.junit.Assume.assumeTrue(JavaVersionUtils.isGreaterThanJava7());
    }

    @Before
    public void before() throws Exception {
        String file = this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();

        File testJarFile = folder.newFile("test.jar");
        ZipUtil.pack(new File(file), testJarFile);

        InstrumentTemplate instrumentTemplate = new InstrumentTemplate(testJarFile);

        InstrumentParseResult instrumentParseResult = instrumentTemplate.build();

        InstrumentTransformer instrumentTransformer = new InstrumentTransformer(instrumentParseResult);

        ClassNode originClassNode = AsmUtils.loadClass(ConsumerContextFilter.class);
        byte[] bytes = AsmUtils.toBytes(originClassNode);

        byte[] transformedBytes = instrumentTransformer.transform(null, ConsumerContextFilter.class.getName(),
                ConsumerContextFilter.class, null, bytes);

        VerifyUtils.asmVerify(transformedBytes);
        object = VerifyUtils.instanceVerity(transformedBytes);
    }

    @Test
    public void test_invoke() throws Exception {
        try {
            VerifyUtils.invoke(object, "invoke", null, null);
        } catch (Throwable e) {
            // ignore
        }

        assertThat(capture.toString()).contains("invoker class: org.apache.dubbo.rpc.filter.ConsumerContextFilter");

    }
}

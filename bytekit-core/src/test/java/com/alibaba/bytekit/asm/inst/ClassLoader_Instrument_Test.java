package com.alibaba.bytekit.asm.inst;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

import org.junit.Test;

import com.alibaba.bytekit.asm.instrument.InstrumentParseResult;
import com.alibaba.bytekit.asm.instrument.InstrumentTemplate;
import com.alibaba.bytekit.asm.instrument.InstrumentTransformer;
import com.alibaba.bytekit.utils.IOUtils;

import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * 
 * @author hengyunabc 2020-11-30
 *
 */
public class ClassLoader_Instrument_Test {

    @Test
    public void test() throws Exception {
        String file = this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();

        TestClassLoader cl = new TestClassLoader(new URL[] { new File(file).toURI().toURL() });

        Class<?> loadClass = cl.loadClass(this.getClass().getName());

        assertThat(loadClass).isNotEqualTo(this.getClass());

        boolean flag = false;
        try {
            cl.loadClass("java.arthas.SpyAPI");
        } catch (Exception e) {
            flag = true;
        }
        // load SpyAPI error
        assertThat(flag).isTrue();

        // 构建一个jar
        // append 到 bootstrap
        File spyJar = new File(file, "arthas-spy.jar");
        JarFile jarFile = new JarFile(spyJar);
        Instrumentation instrumentation = ByteBuddyAgent.install();
        instrumentation.appendToBootstrapClassLoaderSearch(jarFile);

        InstrumentTemplate template = new InstrumentTemplate();

        byte[] classBytes = IOUtils.getBytes(this.getClass().getClassLoader()
                .getResourceAsStream(ClassLoader_Instrument.class.getName().replace('.', '/') + ".class"));
        template.addInstrumentClass(classBytes);

        InstrumentParseResult instrumentParseResult = template.build();
        InstrumentTransformer instrumentTransformer = null;
        try {
            instrumentTransformer = new InstrumentTransformer(instrumentParseResult);
            instrumentation.addTransformer(instrumentTransformer, true);
            instrumentation.retransformClasses(ClassLoader.class);
            // load success
            cl.loadClass("java.arthas.SpyAPI");
        } finally {
            instrumentation.removeTransformer(instrumentTransformer);
        }
    }

    static class TestClassLoader extends URLClassLoader {

        public TestClassLoader(URL[] urls) {
            super(urls, ClassLoader.getSystemClassLoader().getParent());
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("com.") || name.startsWith("java.lang.")) {
                return super.loadClass(name, resolve);
            }
            throw new ClassNotFoundException("class not found: " + name);
        }
    }
}

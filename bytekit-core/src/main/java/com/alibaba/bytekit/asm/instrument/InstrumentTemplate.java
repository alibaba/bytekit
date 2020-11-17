package com.alibaba.bytekit.asm.instrument;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.asm.matcher.SimpleClassMatcher;
import com.alibaba.bytekit.utils.AsmAnnotationUtils;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.IOUtils;
import com.alibaba.bytekit.utils.PropertiesUtils;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;

/**
 * 
 * @author hengyunabc 2020-11-12
 *
 */
public class InstrumentTemplate {

    public static final String INSTRUMENT_PROPERTIES = "instrument.properties";
    public static final String INSTRUMENT = "instrument";

    private ClassLoader targetClassLoader;

    public InstrumentTemplate() {

    }

    public InstrumentParseResult build() {
        InstrumentParseResult result = new InstrumentParseResult();
        try {

            List<String> classes = new ArrayList<String>();

            // 读配置文件
            Enumeration<URL> iter = targetClassLoader.getResources(INSTRUMENT_PROPERTIES);
            while (iter.hasMoreElements()) {
                URL url = iter.nextElement();

                Properties properties = PropertiesUtils.loadNotNull(url);
                String value = properties.getProperty(INSTRUMENT);

                if (value != null) {
                    String[] strings = value.split(",");
                    for (String s : strings) {
                        s = s.trim();
                        if (!s.isEmpty()) {
                            classes.add(s);
                        }
                    }
                }
            }

            // 读取出具体的 .class，再解析
            for (String clazz : classes) {
                URL classUrl = targetClassLoader.getResource(clazz.replace('.', '/') + ".class");
                if (classUrl != null) {
                    byte[] classBytes = IOUtils.getBytes(classUrl.openStream());
                    ClassNode classNode = AsmUtils.toClassNode(classBytes);
                    List<String> matchClassList = AsmAnnotationUtils.queryAnnotationInfo(classNode.visibleAnnotations,
                            Type.getDescriptor(Instrument.class), "Class");

                    if (matchClassList != null && !matchClassList.isEmpty()) {
                        SimpleClassMatcher classMatcher = new SimpleClassMatcher(matchClassList);

                        result.addInstrumentConfig(new InstrumentConfig(classNode, classMatcher));
                    }

                }
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return result;
    }

    public ClassLoader getTargetClassLoader() {
        return targetClassLoader;
    }

    public void setTargetClassLoader(ClassLoader targetClassLoader) {
        this.targetClassLoader = targetClassLoader;
    }

}

package com.alibaba.bytekit.asm.instrument;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.asm.matcher.SimpleClassMatcher;
import com.alibaba.bytekit.asm.matcher.SimpleInterfaceMatcher;
import com.alibaba.bytekit.asm.matcher.SimpleSubclassMatcher;
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

    private List<File> jarFiles = new ArrayList<File>();

    public InstrumentTemplate(File... jarFiles) {
        for (File file : jarFiles) {
            this.jarFiles.add(file);
        }
    }

    public void addJarFiles(Collection<File> jarFiles) {
        this.jarFiles.addAll(jarFiles);
    }

    public void addJarFile(File jarFile) {
        this.jarFiles.add(jarFile);
    }

    public InstrumentParseResult build() throws IOException {
        // 读取jar文件，解析出
        InstrumentParseResult result = new InstrumentParseResult();

        for (File file : jarFiles) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(file);

                JarEntry propertiesEntry = jarFile.getJarEntry(INSTRUMENT_PROPERTIES);

                // 读配置文件
                if (propertiesEntry != null) {
                    InputStream inputStream = jarFile.getInputStream(propertiesEntry);
                    Properties properties = PropertiesUtils.loadNotNull(inputStream);
                    String value = properties.getProperty(INSTRUMENT);

                    List<String> classes = new ArrayList<String>();

                    if (value != null) {
                        String[] strings = value.split(",");
                        for (String s : strings) {
                            s = s.trim();
                            if (!s.isEmpty()) {
                                classes.add(s);
                            }
                        }
                    }

                    // 读取出具体的 .class，再解析
                    for (String clazz : classes) {
                        JarEntry classEntry = jarFile.getJarEntry(clazz.replace('.', '/') + ".class");

                        if (classEntry != null) {
                            byte[] classBytes = IOUtils.getBytes(jarFile.getInputStream(classEntry));
                            ClassNode classNode = AsmUtils.toClassNode(classBytes);
                            List<String> matchClassList = AsmAnnotationUtils.queryAnnotationInfo(
                                    classNode.visibleAnnotations, Type.getDescriptor(Instrument.class), "Class");

                            if (matchClassList != null && !matchClassList.isEmpty()) {
                                SimpleClassMatcher classMatcher = new SimpleClassMatcher(matchClassList);
                                result.addInstrumentConfig(new InstrumentConfig(classNode, classMatcher));
                            }

                            List<String> matchSuperclassList = AsmAnnotationUtils.queryAnnotationInfo(
                                    classNode.visibleAnnotations, Type.getDescriptor(Instrument.class), "Superclass");

                            if (!matchSuperclassList.isEmpty()) {
                                SimpleSubclassMatcher matcher = new SimpleSubclassMatcher(matchSuperclassList);
                                result.addInstrumentConfig(new InstrumentConfig(classNode, matcher));
                            }

                            List<String> matchInterfaceList = AsmAnnotationUtils.queryAnnotationInfo(
                                    classNode.visibleAnnotations, Type.getDescriptor(Instrument.class), "Interface");

                            if (!matchInterfaceList.isEmpty()) {
                                SimpleInterfaceMatcher matcher = new SimpleInterfaceMatcher(matchInterfaceList);
                                result.addInstrumentConfig(new InstrumentConfig(classNode, matcher));
                            }

                        }
                    }
                }

            } finally {
                IOUtils.close(jarFile);
            }
        }

        return result;
    }

}

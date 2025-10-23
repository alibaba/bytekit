package com.alibaba.bytekit.asm.instrument;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.asm.matcher.ClassMatcher;
import com.alibaba.bytekit.asm.matcher.SimpleClassMatcher;
import com.alibaba.bytekit.asm.matcher.SimpleInterfaceMatcher;
import com.alibaba.bytekit.asm.matcher.SimpleSubclassMatcher;
import com.alibaba.bytekit.log.Logger;
import com.alibaba.bytekit.log.Loggers;
import com.alibaba.bytekit.utils.AsmAnnotationUtils;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.IOUtils;
import com.alibaba.bytekit.utils.Pair;
import com.alibaba.bytekit.utils.PropertiesUtils;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;

/**
 * 
 * @author hengyunabc 2020-11-12
 *
 */
public class InstrumentTemplate {
    private final Logger logger = Loggers.getLogger(getClass());
    public static final String INSTRUMENT_PROPERTIES = "instrument.properties";
    public static final String INSTRUMENT = "instrument";
    public static final String TRIGGER_RETRANSFORM = "triggerRetransform";
    /**
     * 通常是工具类，需要在运行时define到用户的ClassLoader里
     */
    public static final String DEFINE = "define";

    private List<File> jarFiles = new ArrayList<File>();

    private List<byte[]> instrumentClassList = new ArrayList<byte[]>();

    private List<byte[]> defineClassList = new ArrayList<byte[]>();

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

    public void addInstrumentClass(byte[] classBytes) {
        this.instrumentClassList.add(classBytes);
    }

    public void addDefineClass(byte[] classBytes) {
        this.defineClassList.add(classBytes);
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

                    String triggerRetransformValue = properties.getProperty("triggerRetransform", "false");
                    boolean triggerRetransform = Boolean.parseBoolean(triggerRetransformValue);
                    
                    // 解析新格式：instrument.{key}=... 和 define.{key}=...
                    Map<String, List<Pair<String, byte[]>>> instrumentMap = new HashMap<String, List<Pair<String, byte[]>>>();
                    Map<String, List<Pair<String, byte[]>>> defineMap = new HashMap<String, List<Pair<String, byte[]>>>();
                    
                    for (String key : properties.stringPropertyNames()) {
                        if (key.startsWith("instrument.")) {
                            String configKey = key.substring("instrument.".length());
                            List<Pair<String, byte[]>> classBytesList = readClassBytes(properties, key, jarFile);
                            if (!classBytesList.isEmpty()) {
                                instrumentMap.put(configKey, classBytesList);
                            }
                        } else if (key.startsWith("define.")) {
                            String configKey = key.substring("define.".length());
                            List<Pair<String, byte[]>> classBytesList = readClassBytes(properties, key, jarFile);
                            if (!classBytesList.isEmpty()) {
                                defineMap.put(configKey, classBytesList);
                            }
                        } else if (key.equals(INSTRUMENT)) {
                            // 兼容旧格式：instrument=...
                            for (Pair<String, byte[]> pair : readClassBytes(properties, INSTRUMENT, jarFile)) {
                                parse(result, pair.second, triggerRetransform, null);
                            }
                        } else if (key.equals(DEFINE)) {
                            // 兼容旧格式：define=...（全局 define，会被添加到所有 instrument）
                            for (Pair<String, byte[]> pair : readClassBytes(properties, DEFINE, jarFile)) {
                                result.addDefineClass(pair.first, pair.second);
                            }
                        }
                    }
                    
                    // 处理新格式的 instrument 和 define 关联
                    for (Map.Entry<String, List<Pair<String, byte[]>>> entry : instrumentMap.entrySet()) {
                        String configKey = entry.getKey();
                        List<Pair<String, byte[]>> defineClassList = defineMap.get(configKey);
                        
                        for (Pair<String, byte[]> instrumentPair : entry.getValue()) {
                            parse(result, instrumentPair.second, triggerRetransform, defineClassList);
                        }
                    }
                }

            } finally {
                IOUtils.close(jarFile);
            }
        }

        // 处理单独设置 byte[]
        for (byte[] classBytes : instrumentClassList) {
            parse(result, classBytes, false, null);
        }

        return result;
    }

    private List<Pair<String, byte[]>> readClassBytes(Properties properties, String key, JarFile jarFile)
            throws IOException {
        List<Pair<String, byte[]>> result = new ArrayList<Pair<String, byte[]>>();
        String value = properties.getProperty(key);
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
                result.add(Pair.of(clazz, classBytes));
            }
        }
        return result;
    }

    private void parse(InstrumentParseResult result, byte[] classBytes, boolean triggerRetransform, List<Pair<String, byte[]>> defineClassList) {
        ClassNode classNode = AsmUtils.toClassNode(classBytes);

        if (!AsmUtils.fitCurrentJvmMajorVersion(classNode)) {
            logger.error(
                    "The current jvm major version is {}, less than the Instrument class major version: {}, ignore this class: {}",
                    AsmUtils.currentJvmMajorVersion(), AsmUtils.getMajorVersion(classNode.version), classNode.name);
            return;
        }

        // 清除apm类的行号
        AsmUtils.removeLineNumbers(classNode);

        boolean updateMajorVersion = Boolean.parseBoolean((String) AsmAnnotationUtils.queryAnnotationValue(classNode.visibleAnnotations,
                Type.getDescriptor(Instrument.class), "updateMajorVersion"));

        List<String> matchClassList = AsmAnnotationUtils.queryAnnotationArrayValue(classNode.visibleAnnotations,
                Type.getDescriptor(Instrument.class), "Class");

        if (matchClassList != null && !matchClassList.isEmpty()) {
            SimpleClassMatcher classMatcher = new SimpleClassMatcher(matchClassList);
            InstrumentConfig config = new InstrumentConfig(classNode, classMatcher, updateMajorVersion, triggerRetransform);
            addDefineConfigs(config, defineClassList);
            result.addInstrumentConfig(config);
        }

        List<String> matchSuperclassList = AsmAnnotationUtils.queryAnnotationArrayValue(classNode.visibleAnnotations,
                Type.getDescriptor(Instrument.class), "Superclass");

        if (!matchSuperclassList.isEmpty()) {
            SimpleSubclassMatcher matcher = new SimpleSubclassMatcher(matchSuperclassList);
            InstrumentConfig config = new InstrumentConfig(classNode, matcher, updateMajorVersion, triggerRetransform);
            addDefineConfigs(config, defineClassList);
            result.addInstrumentConfig(config);
        }

        List<String> matchInterfaceList = AsmAnnotationUtils.queryAnnotationArrayValue(classNode.visibleAnnotations,
                Type.getDescriptor(Instrument.class), "Interface");

        if (!matchInterfaceList.isEmpty()) {
            SimpleInterfaceMatcher matcher = new SimpleInterfaceMatcher(matchInterfaceList);
            InstrumentConfig config = new InstrumentConfig(classNode, matcher, updateMajorVersion, triggerRetransform);
            addDefineConfigs(config, defineClassList);
            result.addInstrumentConfig(config);
        }

        // TODO 处理 @NewField
    }

    /**
     * 将 define 类列表添加到 InstrumentConfig
     */
    private void addDefineConfigs(InstrumentConfig config, List<Pair<String, byte[]>> defineClassList) {
        if (defineClassList != null && !defineClassList.isEmpty()) {
            for (Pair<String, byte[]> pair : defineClassList) {
                config.addDefineConfig(new DefineConfig(pair.second, pair.first));
            }
        }
    }

    public static List<Class<?>> matchedClass(Instrumentation instrumentation, InstrumentConfig instrumentConfig) {
        List<Class<?>> result = new ArrayList<Class<?>>();

        ClassMatcher classMatcher = instrumentConfig.getClassMatcher();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (classMatcher.match(null, clazz.getName(), clazz, null, null)) {
                result.add(clazz);
            }
        }

        return result;
    }
}

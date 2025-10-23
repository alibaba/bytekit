package com.alibaba.bytekit.asm.instrument;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.bytekit.asm.matcher.ClassMatcher;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;

/**
 * <pre>
 * 1. 读properties文件，得到有哪些要增强的类 
 * 2. 解析到具体的配置，还有读取 byte[] 
 * 3. 增加一个Transformer，如果有触发，则用 运行时的 byte[] 替换。 这里是否有不同版本的问题？怎么处理？ 
 * 4. 是否支持retransform
 * 
 * 一个 Inst ，对应一个 matcher，然后一个ClassLoader，可能读取出来有多个 Properties。 一个
 * Properties里可能有多个 Inst。然后它们是同一个 Transformer ？ 有类进来，就先用 matcher来匹配，有匹上的，就处理
 * </pre>
 * 
 * @author hengyunabc 2020-11-12
 *
 */
public class InstrumentConfig {
    private boolean updateMajorVersion;

    private ClassNode instrumentClassNode;

    private ClassMatcher classMatcher;

    /**
     * 有的类在之前已经被加载到 JVM 里了，需要显式触发 Retransform ，增强的字节码才能生效
     */
    private boolean triggerRetransform = false;

    /**
     * 与这个 instrument 配置关联的 define 类列表，只有当这个 instrument 匹配时才会 define 这些类
     */
    private List<DefineConfig> defineConfigs = new ArrayList<DefineConfig>();

    public InstrumentConfig(ClassNode instrumentClassNode, ClassMatcher classMatcher) {
        this(instrumentClassNode, classMatcher, false, false);
    }

    public InstrumentConfig(ClassNode instrumentClassNode, ClassMatcher classMatcher, boolean updateMajorVersion, boolean triggerRetransform) {
        super();
        this.instrumentClassNode = instrumentClassNode;
        this.classMatcher = classMatcher;
        this.updateMajorVersion = updateMajorVersion;
        this.triggerRetransform = triggerRetransform;
    }

    public ClassNode getInstrumentClassNode() {
        return instrumentClassNode;
    }

    public void setInstrumentClassNode(ClassNode instrumentClassNode) {
        this.instrumentClassNode = instrumentClassNode;
    }

    public ClassMatcher getClassMatcher() {
        return classMatcher;
    }

    public void setClassMatcher(ClassMatcher classMatcher) {
        this.classMatcher = classMatcher;
    }

    public boolean isUpdateMajorVersion() {
        return updateMajorVersion;
    }

    public void setUpdateMajorVersion(boolean updateMajorVersion) {
        this.updateMajorVersion = updateMajorVersion;
    }

    public boolean isTriggerRetransform() {
        return triggerRetransform;
    }

    public void setTriggerRetransform(boolean triggerRetransform) {
        this.triggerRetransform = triggerRetransform;
    }

    public List<DefineConfig> getDefineConfigs() {
        return defineConfigs;
    }

    public void setDefineConfigs(List<DefineConfig> defineConfigs) {
        this.defineConfigs = defineConfigs;
    }

    public void addDefineConfig(DefineConfig defineConfig) {
        this.defineConfigs.add(defineConfig);
    }

}

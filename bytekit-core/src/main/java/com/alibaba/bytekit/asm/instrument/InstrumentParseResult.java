package com.alibaba.bytekit.asm.instrument;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author hengyunabc 2020-11-12
 *
 */
public class InstrumentParseResult {

    private List<InstrumentConfig> instrumentConfigs = new ArrayList<InstrumentConfig>();

    /**
     * @deprecated 已废弃，define 配置现在关联到具体的 InstrumentConfig 上。为了保持向后兼容暂时保留。
     */
    @Deprecated
    private List<DefineConfig> defineConfigs = new ArrayList<DefineConfig>();

    /**
     * @deprecated 已废弃，建议使用关联到 InstrumentConfig 的 define 配置
     */
    @Deprecated
    public void addDefineClass(String className, byte[] classBytes) {
        this.defineConfigs.add(new DefineConfig(classBytes, className));
    }

    public void addInstrumentConfig(InstrumentConfig config) {
        this.instrumentConfigs.add(config);
    }

    public List<InstrumentConfig> getInstrumentConfigs() {
        return instrumentConfigs;
    }

    public void setInstrumentConfigs(List<InstrumentConfig> instrumentConfigs) {
        this.instrumentConfigs = instrumentConfigs;
    }

    /**
     * @deprecated 已废弃，define 配置现在关联到具体的 InstrumentConfig 上
     */
    @Deprecated
    public List<DefineConfig> getDefineConfigs() {
        return defineConfigs;
    }

    /**
     * @deprecated 已废弃，define 配置现在关联到具体的 InstrumentConfig 上
     */
    @Deprecated
    public void setDefineConfigs(List<DefineConfig> defineConfigs) {
        this.defineConfigs = defineConfigs;
    }

}

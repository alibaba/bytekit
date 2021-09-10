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

    private List<DefineConfig> defineConfigs = new ArrayList<DefineConfig>();

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

    public List<DefineConfig> getDefineConfigs() {
        return defineConfigs;
    }

    public void setDefineConfigs(List<DefineConfig> defineConfigs) {
        this.defineConfigs = defineConfigs;
    }

}

package com.alibaba.bytekit.asm.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author hengyunabc 2020-11-12
 *
 */
public class InstrumentParseResult {

    List<InstrumentConfig> instrumentConfigs = new ArrayList<InstrumentConfig>();

    ClassFileTransformer transformer;

    public void addInstrumentConfig(InstrumentConfig config) {
        this.instrumentConfigs.add(config);
    }

    public List<InstrumentConfig> getInstrumentConfigs() {
        return instrumentConfigs;
    }

    public void setInstrumentConfigs(List<InstrumentConfig> instrumentConfigs) {
        this.instrumentConfigs = instrumentConfigs;
    }

}

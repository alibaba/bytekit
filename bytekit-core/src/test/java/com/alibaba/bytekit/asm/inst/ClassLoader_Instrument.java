package com.alibaba.bytekit.asm.inst;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

/**
 * 
 * @author hengyunabc 2020-11-30
 *
 */
@Instrument(Class = "java.lang.ClassLoader")
public abstract class ClassLoader_Instrument {
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name.startsWith("java.arthas.")) {
            return ClassLoader.getSystemClassLoader().loadClass(name);
        }

        Class clazz = InstrumentApi.invokeOrigin();
        return clazz;
    }
}

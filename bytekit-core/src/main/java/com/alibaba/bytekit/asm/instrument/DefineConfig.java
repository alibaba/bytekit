package com.alibaba.bytekit.asm.instrument;

/**
 * 
 * @author hengyunabc 2021-08-27
 *
 */
public class DefineConfig {
    private byte[] classBytes;
    private String className;

    public DefineConfig(byte[] classBytes, String className) {
        super();
        this.classBytes = classBytes;
        this.className = className;
    }

    public byte[] getClassBytes() {
        return classBytes;
    }

    public void setClassBytes(byte[] classBytes) {
        this.classBytes = classBytes;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

}

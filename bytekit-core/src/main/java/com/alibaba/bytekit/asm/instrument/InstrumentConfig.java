package com.alibaba.bytekit.asm.instrument;

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
 * @author hengyunabc 2020-11-12
 *
 */
public class InstrumentConfig {

    private ClassNode instrumentClassNode;

    private ClassMatcher classMatcher;

    public InstrumentConfig(ClassNode instrumentClassNode, ClassMatcher classMatcher) {
        super();
        this.instrumentClassNode = instrumentClassNode;
        this.classMatcher = classMatcher;
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

}

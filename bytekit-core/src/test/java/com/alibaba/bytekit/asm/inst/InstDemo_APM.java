package com.alibaba.bytekit.asm.inst;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.NewField;

@Instrument
public abstract class InstDemo_APM {

    @NewField
    private String newField;

    public int newMethod(String s) {
        return s.length() + 998;
    }

    // 这种方式来写怎么样？有点丑，但是不需要写那些转换的代码。 在插件的编绎出结果后，可以检查下 名字，static，参数等是否匹配的。
    // 这种处理有点丑，但inline应该没问题
    public int __origin_returnInt(int i) {
        return 0;
    }

    public int returnInt(int i) {

        int re = __origin_returnInt(i);

        return 9998 + re;
    }

    public static int returnIntStatic(int i) {
        return 9998;
    }
}

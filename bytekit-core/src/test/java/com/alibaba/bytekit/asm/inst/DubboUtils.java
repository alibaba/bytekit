package com.alibaba.bytekit.asm.inst;

import org.apache.dubbo.rpc.Invoker;

public class DubboUtils {
    public static void print(Invoker<?> invoker) {
        System.out.println(invoker);
    }
}

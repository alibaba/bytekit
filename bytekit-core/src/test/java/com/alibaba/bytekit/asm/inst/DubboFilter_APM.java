package com.alibaba.bytekit.asm.inst;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

/**
 * @see org.apache.dubbo.rpc.Filter
 * @author hengyunabc 2020-11-26
 *
 */
@Instrument(Interface = "org.apache.dubbo.rpc.Filter")
public abstract class DubboFilter_APM {

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.err.println("invoker class: " + this.getClass().getName());
        Result result = InstrumentApi.invokeOrigin();

        return result;
    }
}

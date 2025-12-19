package com.alibaba.bytekit.asm.inst;

import java.util.Comparator;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.RootBeanDefinition;

import com.alibaba.bytekit.agent.inst.Instrument;
import com.alibaba.bytekit.agent.inst.InstrumentApi;

@Instrument(Class = "org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory")
public abstract class AbstractAutowireCapableBeanFactory_Instrument {

    public Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {
        // 触发 Java8 接口静态方法调用（invokestatic + InterfaceMethodref），用于覆盖 updateMajorVersion 的默认行为
        Comparator.naturalOrder();
        return InstrumentApi.invokeOrigin();
    }
}


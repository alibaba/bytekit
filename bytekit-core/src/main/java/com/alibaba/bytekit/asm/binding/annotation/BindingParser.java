package com.alibaba.bytekit.asm.binding.annotation;

import java.lang.annotation.Annotation;

import com.alibaba.bytekit.asm.binding.Binding;

public interface BindingParser {
    
    public Binding parse(Annotation annotation);

}

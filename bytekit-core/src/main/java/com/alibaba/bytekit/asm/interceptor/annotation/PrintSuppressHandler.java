package com.alibaba.bytekit.asm.interceptor.annotation;

import com.alibaba.bytekit.asm.binding.Binding;

public class PrintSuppressHandler {
    
    @ExceptionHandler(inline = true)
    public static void onSuppress(@Binding.Throwable Throwable e) {
        e.printStackTrace();
    }
}

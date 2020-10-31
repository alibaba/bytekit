package com.alibaba.bytekit.asm.location;

import java.util.List;

import com.alibaba.bytekit.asm.MethodProcessor;

public interface LocationMatcher {

    public List<Location> match(MethodProcessor methodProcessor);

}

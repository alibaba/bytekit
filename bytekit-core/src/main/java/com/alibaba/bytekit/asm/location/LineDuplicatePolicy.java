package com.alibaba.bytekit.asm.location;

public enum LineDuplicatePolicy {
    DEFAULT,
    FIRST,
    ALL,
    REJECT_AFTER_CONTROL_FLOW,
    FIRST_PER_LINE_BLOCK
}

package com.alibaba.bytekit.asm.location;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.location.filter.LocationFilter;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.InsnNode;
import com.alibaba.deps.org.objectweb.asm.tree.LineNumberNode;

import java.util.ArrayList;
import java.util.List;

/**
 * find the location before line.
 * 1.will only return instance of type {@link  com.alibaba.bytekit.asm.location.Location.LineBeforeLocation}.
 * 2.will only return first method exit location when beforeLine equals -1 .
 * 3.will only return first matched {@link LineNumberNode}'s previous node even there are multi matched {@link LineNumberNode} in method.
 * 4.you should add {@link LocationType#EXIT} and {@link LocationType#LINE} in {@link LocationFilter} if you want to avoid the repeat intercept.
 */
public class LineBeforeLocationMatcher implements LocationMatcher {

    private Integer beforeLine = 0;

    public LineBeforeLocationMatcher(int beforeLine) {
        if (beforeLine < -1) {
            throw new IllegalArgumentException("beforeLine must grater than -1");
        }
        this.beforeLine = beforeLine;
    }

    @Override
    public List<Location> match(MethodProcessor methodProcessor) {
        List<Location> locations = new ArrayList<Location>();
        AbstractInsnNode insnNode = methodProcessor.getEnterInsnNode();
        LocationFilter locationFilter = methodProcessor.getLocationFilter();

        while (insnNode != null) {

            if (beforeLine == -1) {
                if (insnNode instanceof InsnNode) {
                    InsnNode node = (InsnNode) insnNode;
                    if (matchExit(node)) {
                        boolean filtered = !locationFilter.allow(node, LocationType.EXIT, false);
                        Location location = new Location.LineBeforeLocation(node, beforeLine, false, filtered);
                        locations.add(location);
                        //只取第一个
                        break;
                    }
                }
            } else {
                if (insnNode instanceof LineNumberNode) {
                    LineNumberNode lineNumberNode = (LineNumberNode) insnNode;
                    if (matchLine(lineNumberNode.line)) {
                        boolean filtered = !locationFilter.allow(lineNumberNode, LocationType.LINE, false);
                        //目前因为如果直接返回lineNumberNode，按当前逻辑增强完之后会导致行号丢失，暂时没找到原因，因此向上取一个节点
                        Location location = new Location.LineBeforeLocation(lineNumberNode.getPrevious(), beforeLine, false, filtered);
                        locations.add(location);
                        //由于会存在多个相同行号的情况，这里只取第一个行号
                        break;
                    }
                }
            }

            insnNode = insnNode.getNext();
        }
        return locations;
    }

    private boolean matchLine(int line) {
        return line == beforeLine;
    }

    /**
     * same with {@link com.alibaba.bytekit.asm.location.ExitLocationMatcher}
     */
    public boolean matchExit(InsnNode node) {
        switch (node.getOpcode()) {
            case Opcodes.RETURN: // empty stack
            case Opcodes.IRETURN: // 1 before n/a after
            case Opcodes.FRETURN: // 1 before n/a after
            case Opcodes.ARETURN: // 1 before n/a after
            case Opcodes.LRETURN: // 2 before n/a after
            case Opcodes.DRETURN: // 2 before n/a after
                return true;
        }
        return false;
    }

}

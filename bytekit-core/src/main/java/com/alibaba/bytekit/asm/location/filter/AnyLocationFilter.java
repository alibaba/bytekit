package com.alibaba.bytekit.asm.location.filter;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.bytekit.asm.location.LocationType;
import com.alibaba.deps.org.objectweb.asm.tree.AbstractInsnNode;

public class AnyLocationFilter implements LocationFilter {

    protected List<LocationFilter> filters = new ArrayList<LocationFilter>();

    public AnyLocationFilter(LocationFilter... filters) {
        for (LocationFilter filter : filters) {
            this.filters.add(filter);
        }
    }

    public void addFilter(LocationFilter filter) {
        this.filters.add(filter);
    }

    @Override
    public boolean allow(AbstractInsnNode insnNode, LocationType locationType, boolean complete) {
        for (LocationFilter filter : filters) {
            if (filter.allow(insnNode, locationType, complete)) {
                return true;
            }
        }
        return false;
    }
}

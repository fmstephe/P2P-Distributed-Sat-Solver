package org.francis.sat.solver;

import java.util.List;

public interface WorkSharer {

    public abstract List<?> giveWork();

    public abstract void receiveWork(List<?> stack);

    public abstract int sharableWork();
    
    public abstract boolean needsWork();
    
    public abstract boolean isComplete();
    
    public abstract boolean isTriviallyUnsat();
}

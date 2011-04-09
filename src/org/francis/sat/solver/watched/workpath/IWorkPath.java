package org.francis.sat.solver.watched.workpath;

import java.util.List;

import org.francis.sat.solver.WorkSharer;

public interface IWorkPath {

    public abstract void addToPath(int literal, boolean branchable, boolean unit);

    public abstract int backtrack();

    public abstract List<?> giveWork();

    public abstract void receiveWork(List<?> receivedWork);

    public abstract int workSize();

    public abstract int assignCount();
}
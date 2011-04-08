package org.francis.sat.solver.watched;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.francis.sat.solver.Clause;

public class WorkPath {

    private static final int BRANCHABLE = 1;
    private static final int UNIT = 2;
    private static final int SHARED = 4;
    
    private final WatchedFormula formula;
    private final int[] path;
    private int idx;
    private int workSize;
    
    public WorkPath(WatchedFormula formula, int varNum) {
        this.formula = formula;
        this.path = new int[varNum*2];
        this.idx = 0;
        this.workSize = 0;
    }
    
    public void addToPath(int literal, boolean branchable, boolean unit) {
        assert workSize == countWorkSize();
        int workState = 0;
        workState |= branchable ? BRANCHABLE : 0;
        workState |= unit ? UNIT : 0;
        int var = Clause.getVariable(literal);
        path[idx] = literal;
        path[idx+1] = workState;
        idx += 2;
        assert !containsLiteral(literal);
        assert !formula.freeVars.contains(var);
        if (branchable && !unit) workSize++;
        assert workSize == countWorkSize();
    }

    private boolean containsLiteral(int literal) {
        for (int i = 0; i < idx; i += 2) {
            if (path[i] == literal) return true;
        }
        return false;
    }

    public int backtrack() {
        assert idx != 0;
        while (idx != 0) {
            assert idx%2 == 0;
            assert workSize == countWorkSize();
            assert formula.checkState();
            idx -= 2;
            int literal = path[idx];
            int workState = path[idx+1];
            formula.resetLiteral(path[idx]);
            if (workState == BRANCHABLE) workSize--;
            assert workSize == countWorkSize();
            if (workState == (BRANCHABLE | SHARED)) {
                return literal;
            }
        }
        assert workSize == 0;
        assert workSize == countWorkSize();
        return -1;
    }

    public List<?> giveWork() {
        assert workSize == countWorkSize();
        List<PathElement> localPath = new ArrayList<PathElement>(path.size());
        List<PathElement> sharedPath = new ArrayList<PathElement>(path.size());
        List<PathElement> oldPath = path;
        boolean share = workSize%2 == 0;
        int shareTarget = workSize-1;
        int shareCount = 0;
        int i = 0;
        for (;i < oldPath.size(); i++) {
            PathElement p = oldPath.get(i);
            if (p.branchable && !p.shared) {
                if (shareCount == shareTarget) {
                    assert !share;
                    break; // This is done to prevent the last unshared branchable element from being shared
                }
                if (share) {
                    sharedPath.add(p.copyPathElement(false));
                    localPath.add(p.copyPathElement(true));
                    workSize--;
                }
                else {
                    sharedPath.add(p.copyPathElement(true));
                    localPath.add(p.copyPathElement(false));
                }
                share = !share;
                shareCount++;
            }
            else {
                sharedPath.add(p.copyPathElement());
                localPath.add(p.copyPathElement());
            }
        }
        for (;i < oldPath.size();i++) {
            PathElement p = oldPath.get(i);
            localPath.add(p);
        }
        assert verifySharedPath(localPath,sharedPath,oldPath);
        this.path = localPath;
        assert workSize == countWorkSize();
        return sharedPath;
    }

    private boolean verifySharedPath(List<PathElement> localPath,List<PathElement> sharedPath,List<PathElement> oldPath) {
        assert sharedPath.size() < path.size();
        assert localPath.size() == oldPath.size();
        int i = 0;
        for (; i < sharedPath.size(); i++) {
            PathElement l = localPath.get(i);
            PathElement s = sharedPath.get(i);
            PathElement o = oldPath.get(i);
            assert l.branchable == s.branchable;
            assert l.branchable == o.branchable;
            // If an element was already shared then it must be shared in both new paths
            assert !(o.branchable && o.shared && (!l.shared || !s.shared));
            // If an element was branchable and not shared then it must be shared in one new path but not in the other
            assert !(o.branchable && !o.shared && (l.shared == s.shared));
            assert l.unit == s.unit;
            assert l.unit == o.unit;
        }
        for (; i < localPath.size(); i++) {
            PathElement l = localPath.get(i);
            PathElement o = oldPath.get(i);
            assert l.branchable == o.branchable;
            assert l.shared == o.shared;
            assert l.unit == o.unit;
            if (i == sharedPath.size())
                assert l.branchable && !l.shared;
            else
                assert !(l.branchable && !l.shared);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public void receiveWork(List<?> receivedWork) {
        assert workSize == 0;
        this.path = (List<PathElement>)receivedWork;
        for (PathElement e : path) {
            formula.setLiteralForNewPath(e.literal, e.branchable);
            if (e.branchable && !e.unit && !e.shared) workSize++;
        }
        assert formula.checkState();
        assert workSize == countWorkSize();
    }

    public int workSize() {
        assert workSize == countWorkSize();
        return workSize;
    }
    
    public int assignCount() {
        return path.size();
    }
    
    private int countWorkSize() {
        int count = 0;
        for (PathElement e : path) {
            if (e.branchable && !e.unit && !e.shared) count++;
        }
        return count;
    }
    
    @Override
    public String toString() {
        return path.toString();
    }
}

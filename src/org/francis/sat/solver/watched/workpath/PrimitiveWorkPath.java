package org.francis.sat.solver.watched.workpath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.francis.sat.solver.watched.WatchedFormula;

public class PrimitiveWorkPath implements IWorkPath {

    private List<PathElement> path;
    private final WatchedFormula formula;
    private int workSize;
    
    public PrimitiveWorkPath(WatchedFormula formula, int varNum) {
        this.formula = formula;
        this.path = new ArrayList<PathElement>();
        this.workSize = 0;
    }
    
    public void addToPath(int literal, boolean branchable, boolean unit) {
        assert workSize == countWorkSize();
        PathElement e = new PathElement(literal,branchable,unit,false);
        assert !path.contains(e);
        assert !formula.isIndetermined(literal);
        path.add(e);
        if (branchable && !unit) workSize++;
        assert workSize == countWorkSize();
    }

    public int backtrack() {
        assert workSize == countWorkSize();
        while (!path.isEmpty()) {
            assert workSize == countWorkSize();
            assert formula.checkState();
            PathElement e = path.remove(path.size()-1);
            formula.resetLiteral(e.literal);
            if (e.branchable && !e.unit && !e.shared) workSize--;
            assert workSize == countWorkSize();
            if (e.branchable && !e.shared) {
                return e.literal;
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

    private static class PathElement implements Serializable {
        private static final long serialVersionUID = -7816551849274761489L;
        public final int literal;
        public final boolean branchable;
        public final boolean unit;
        public final boolean shared;
        
        public PathElement(int literal, boolean branchable, boolean unit, boolean shared) {
            assert !(branchable && unit);
            this.literal = literal;
            this.branchable = branchable;
            this.shared = shared;
            this.unit = unit;
        }
        
        public PathElement copyPathElement() {
            PathElement newE = new PathElement(this.literal,this.branchable,this.unit,this.shared);
            return newE;
        }
        
        public PathElement copyPathElement(boolean shared) {
            PathElement newE = new PathElement(this.literal,this.branchable,this.unit,shared);
            return newE;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Math.abs(literal);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PathElement other = (PathElement) obj;
            if (Math.abs(literal) != Math.abs(other.literal))
                return false;
            return true;
        }
        
        public String toString() {
            return literal + "," + (branchable ? "br" : "nb") +","+ (shared ? "sh" : "ns") +","+ (unit ? "un" : "nu");
        }
    }
}

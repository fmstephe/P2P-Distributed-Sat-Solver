package org.francis.sat.solver.watched.workpath;

import java.util.ArrayList;
import java.util.List;

import org.francis.sat.solver.watched.WatchedFormula;

public class ObjectWorkPath implements IWorkPath {

    private static final int BRANCHABLE = 1;
    private static final int UNIT = 2;
    private static final int SHARED = 4;
    
    private final WatchedFormula formula;
    private final int[] path;
    private int idx;
    private int workSize;
    
    public ObjectWorkPath(WatchedFormula formula, int varNum) {
        this.formula = formula;
        this.path = new int[varNum*2];
        this.idx = 0;
        this.workSize = 0;
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.workpath.IWork#addToPath(int, boolean, boolean)
     */
    @Override
    public void addToPath(int literal, boolean branchable, boolean unit) {
        assert workSize == countWorkSize();
        assert !containsLiteral(literal);
        assert !formula.isIndetermined(literal);
        int workState = 0;
        workState |= branchable ? BRANCHABLE : 0;
        workState |= unit ? UNIT : 0;
        path[idx] = literal;
        path[idx+1] = workState;
        idx += 2;
        if (branchable && !unit) workSize++;
        assert idx%2 == 0;
        assert containsLiteral(literal);
        assert workSize == countWorkSize();
    }

    private boolean containsLiteral(int literal) {
        for (int i = 0; i < idx; i += 2) {
            if (path[i] == literal) return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.workpath.IWork#backtrack()
     */
    @Override
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
            if (workState == BRANCHABLE) {
                return literal;
            }
        }
        assert workSize == 0;
        assert workSize == countWorkSize();
        return -1;
    }

    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.workpath.IWork#giveWork()
     */
    @Override
    public List<?> giveWork() {
        assert workSize == countWorkSize();
        int[] oldPath = copyPathForAsserts();
        List<Integer> sharedPath = new ArrayList<Integer>(idx);
        boolean share = workSize%2 == 0;
        int shareTarget = workSize-1;
        int shareCount = 0;
        int i = 0;
        for (;i <= idx; i += 2) {
            assert i == sharedPath.size();
            int literal = path[i];
            int workState = path[i+1];
            if (workState == BRANCHABLE) { // !SHARED & !UNIT
                if (shareCount == shareTarget) {
                    assert !share;
                    break; // This is done to prevent the last unshared branchable element from being shared
                }
                if (share) {
                    sharedPath.add(literal);
                    sharedPath.add(workState);
                    path[i+1] |= SHARED; // Set the local work-state to shared
                    workSize--;
                }
                else {
                    sharedPath.add(literal);
                    sharedPath.add(workState | SHARED); // Set the shared work-state to shared
                }
                share = !share;
                shareCount++;
            }
            else {
                sharedPath.add(literal);
                sharedPath.add(workState);
            }
        }
        assert verifySharedPath(path,sharedPath,oldPath);
        assert workSize == countWorkSize();
        return sharedPath;
    }
    
    private int[] copyPathForAsserts() {
        // Assertion hackery allows us to conditionally copy the work-path is assertions are enabled
        boolean assertionsEnabled = false;
        assert (assertionsEnabled = true);
        if (assertionsEnabled) {
            int[] copiedPath = new int[idx];
            System.arraycopy(path, 0, copiedPath, 0, idx);
            return copiedPath;
        }
        return null;
    }

    private boolean verifySharedPath(int[] localPath, List<Integer> sharedPath, int[] oldPath) {
        assert sharedPath.size() < idx;
        int i = 0;
        for (; i < idx; i += 2) {
            int lLiteral = localPath[i];
            int sLiteral = i < sharedPath.size() ? sharedPath.get(i) : -1;
            int oLiteral = oldPath[i];
            int lWorkState = localPath[i+1];
            int sWorkState = i < sharedPath.size() ? sharedPath.get(i+1) : -1;
            int oWorkState = oldPath[i+1];
            boolean lBranchable = (lWorkState & BRANCHABLE) == BRANCHABLE;
            boolean sBranchable = (sWorkState & BRANCHABLE) == BRANCHABLE;
            boolean oBranchable = (oWorkState & BRANCHABLE) == BRANCHABLE;
            boolean lShared = (lWorkState & SHARED) == SHARED;
            boolean sShared = (sWorkState & SHARED) == SHARED;
            boolean oShared = (oWorkState & SHARED) == SHARED;
            boolean lUnit = (lWorkState & UNIT) == UNIT;
            boolean sUnit = (sWorkState & UNIT) == UNIT;
            boolean oUnit = (oWorkState & UNIT) == UNIT;
            if (i < sharedPath.size()) {
                assert lLiteral == sLiteral;
                assert sLiteral == oLiteral;
                assert lBranchable == sBranchable;
                assert lBranchable == oBranchable;
                // If an element was already shared then it must be shared in both new paths
                assert !(oBranchable && oShared && (!lShared || !sShared));
                // If an element was branchable and not shared then it must be shared in one new path but not in the other
                assert !(oBranchable && !oShared && (lShared == sShared));
                assert lUnit == sUnit;
                assert lUnit == oUnit;
            }
            else {
                assert lLiteral == oLiteral;
                assert lBranchable == oBranchable;
                assert lShared == oShared;
                assert lUnit == oUnit;
                if (i == sharedPath.size())
                    assert lBranchable && !lShared;
                else
                    assert !(lBranchable && !lShared);
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.workpath.IWork#receiveWork(java.util.List)
     */
    @Override
    public void receiveWork(List<?> receivedWork) {
        assert workSize == 0;
        for (int i = 0; i < receivedWork.size(); i += 2) {
            int literal = (Integer)receivedWork.get(i);
            int workState = (Integer)receivedWork.get(i+1);
            path[i] = literal;
            path[i+1] = workState;
            formula.setLiteralForNewPath(literal, (workState & BRANCHABLE) == BRANCHABLE);
            if (workState == BRANCHABLE) workSize++;
        }
        idx = receivedWork.size();
        assert formula.checkState();
        assert workSize == countWorkSize();
    }

    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.workpath.IWork#workSize()
     */
    @Override
    public int workSize() {
        assert workSize == countWorkSize();
        return workSize;
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.workpath.IWork#assignCount()
     */
    @Override
    public int assignCount() {
        return idx/2;
    }
    
    private int countWorkSize() {
        int count = 0;
        for (int i = 1; i < idx; i += 2) {
            if (path[i] == BRANCHABLE) count++;
        }
        return count;
    }
    
    @Override
    public String toString() {
        return path.toString();
    }
}
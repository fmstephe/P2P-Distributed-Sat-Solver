package org.francis.sat.solver.watched;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.francis.sat.solver.BooleanFormula;
import org.francis.sat.solver.Clause;
import org.francis.sat.solver.WorkSharer;

public class WatchedFormula implements BooleanFormula, WorkSharer, Serializable {
    
    private static final long serialVersionUID = -2045142152043525717L;
    
    private final int varNum, clauseNum;
    private final byte[] varVals;
    private final List<WatchedClause>[] watchedClauseArray;
    private final List<WatchedClause> initUnitClauses;
    private WorkPath path;
    protected final TIntSet freeVars;
    private boolean isTriviallyUnsat = false;
    
    @SuppressWarnings("unchecked")
    WatchedFormula(int varNum, int clauseNum) {
        this.varNum = varNum;
        this.clauseNum = clauseNum;
        this.initUnitClauses = new ArrayList<WatchedClause>();
        this.watchedClauseArray = new List[varNum+1];
        for (int i = 1; i < watchedClauseArray.length; i++) {
            watchedClauseArray[i] = new ArrayList<WatchedClause>();
        }
        this.varVals = new byte[varNum+1];
        this.freeVars = new TIntHashSet(varNum);
    }
    
    public void init() {
        this.path = new WorkPath(this,varNum);
        for (int i = 1; i < varVals.length; i++) {
            freeVars.add(i);
        }
        for (int i = 1; i < varVals.length; i++) {
            varVals[i] = 0;
        }
        boolean noConflicts = unitPropogation(initUnitClauses);
        if (!noConflicts) {
            backtrack();
            isTriviallyUnsat = true;
        }
        assert checkState();
    }
    
    public void addClause(List<Integer> literals) {
        WatchedClause newClause = new WatchedClause(literals,this);
        int check = newClause.preCheck();
        if (check == WatchedClause.MADE_SAT) {
            return;
        }
        else if (check == WatchedClause.MADE_UNIT) {
            initUnitClauses.add(newClause);
        }
        else {
            int[] watches = newClause.getWatchedLiterals();
            int var1Idx = Clause.getVariable(watches[0]);
            int var2Idx = Clause.getVariable(watches[1]);
            watchedClauseArray[var1Idx].add(newClause);
            watchedClauseArray[var2Idx].add(newClause);
        }
    }
    
    public int chooseVariable() {
        TIntIterator itr = freeVars.iterator();
        return itr.next();
    }
    
    private boolean unitPropogation(List<WatchedClause> unitClauses) {
        for (int i = 0; i < unitClauses.size(); i++) {
            WatchedClause clause = unitClauses.get(i);
            int unitLiteral = clause.getUnitLiteral();
            if (!isAssigned(unitLiteral)) {
                setLiteral(unitLiteral,false,true,unitClauses);
            }
            else if (isConflicted(unitLiteral)) {
                return false;
            }
        }
        return true;
    }
    
    protected boolean isConflicted(int literal) {
        int val = varVals[Clause.getVariable(literal)];
        if (val == 0) return false;
        return (literal%2 == 0 && val == -1) || (literal%2 == 1 && val == 1);
    }
    
    protected boolean isSatisfied(int literal) {
        int val = varVals[Clause.getVariable(literal)];
        if (val == 0) return false;
        return (literal%2 == 0 && val == 1) || (literal%2 == 1 && val == -1);
    }
    
    public boolean isIndetermined(int literal) {
        return varVals[Clause.getVariable(literal)] == 0;
    }
    
    protected int getVal(int literal) {
        return varVals[Clause.getVariable(literal)];
    }
    
    protected boolean isAssigned(int literal) {
        return getVal(literal) != 0;
    }

    public void tryLiteral(int literal) {
        assert checkState();
        setWithBacktracking(literal,true);
        assert checkState();
    }
    
    private void setWithBacktracking(int literal, boolean branchable) {
        List<WatchedClause> unitClauses = new ArrayList<WatchedClause>();
        setLiteral(literal,branchable,false,unitClauses);
        boolean noConflict = unitPropogation(unitClauses);
        if (!noConflict) {
            backtrack();
        }
    }
    
    protected void setLiteral(int literal, boolean branchable, boolean unit, List<WatchedClause> unitClauses) {
        assert checkState();
        int var = Clause.getVariable(literal);
        freeVars.remove(var);
        varVals[var] = literal%2 == 0 ? (byte)1 : (byte)-1;
        path.addToPath(literal, branchable, unit);
        List<WatchedClause> watchedClauses = watchedClauseArray[var];
        Iterator<WatchedClause> itr = watchedClauses.iterator();
        while (itr.hasNext()) {
            WatchedClause clause = itr.next();
            int newWatchedLiteral = clause.satisfying(literal);
            if (newWatchedLiteral >= 0) {
                watchedClauseArray[Clause.getVariable(newWatchedLiteral)].add(clause);
                itr.remove();
            }
            else if (newWatchedLiteral == WatchedClause.MADE_UNIT) {
                unitClauses.add(clause);
            }
        }
        assert checkState();
    }
    
    protected void setLiteralForNewPath(int literal, boolean branchable) {
        int var = Clause.getVariable(literal);
        freeVars.remove(var);
        varVals[var] = literal%2 == 0 ? (byte)1 : (byte)-1;
        List<WatchedClause> watchedClauses = watchedClauseArray[var];
        Iterator<WatchedClause> itr = watchedClauses.iterator();
        while (itr.hasNext()) {
            WatchedClause clause = itr.next();
            int newWatchedLiteral = clause.satisfying(literal);
            if (newWatchedLiteral >= 0) {
                watchedClauseArray[Clause.getVariable(newWatchedLiteral)].add(clause);
                itr.remove();
            }
        }
    }
    
    protected void backtrack() {
        assert checkState();
        int branchingLiteral = path.backtrack();
        if (branchingLiteral > 0) {
            assert checkState();
            setWithBacktracking(Clause.compliment(branchingLiteral),false);
            assert checkState();
        }
        assert checkState();
    }
    
    public String toString() {
        return Arrays.toString(varVals);
    }

    public void resetLiteral(int literal) {
        int var = Clause.getVariable(literal);
        freeVars.add(var);
        varVals[var] = 0;
    }

    public List<?> giveWork() {
        assert checkState();
        return path.giveWork();
    }

    public void receiveWork(List<?> receivedWork) {
        assert checkState();
        assert needsWork();
        path.receiveWork(receivedWork);
        assert checkState();
        backtrack();
        assert checkState();
    }
    
    public int sharableWork() {
        return path.workSize();
    }
    
    @Override
    public boolean needsWork() {
        return path.assignCount() == 0;
    }

    @Override
    public boolean isComplete() {
        return path.assignCount() == varNum;
    }

    @Override
    public boolean isTriviallyUnsat() {
        return isTriviallyUnsat;
    }
    
    protected boolean checkState() {
        assert varNum - freeVars.size() == path.assignCount();
        return true;
    }

    @Override
    public int getVarNum() {
        return this.varNum;
    }

    @Override
    public int getClauseNum() {
        return this.clauseNum;
    }

    @Override
    public List<List<Integer>> getDimacsClauses() {
        List<List<Integer>> clauses = new ArrayList<List<Integer>>();
        for (List<WatchedClause> watchedClauses : watchedClauseArray) {
            if (watchedClauses == null) continue;
            for (WatchedClause watchedClause : watchedClauses) {
                clauses.add(watchedClause.getDimacsClause());
            }
        }
        for (WatchedClause watchedClause : initUnitClauses) {
            clauses.add(watchedClause.getDimacsClause());
        }
        return clauses;
    }
}

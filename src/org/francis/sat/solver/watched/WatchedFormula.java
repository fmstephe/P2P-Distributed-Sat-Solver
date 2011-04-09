package org.francis.sat.solver.watched;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.francis.sat.solver.BooleanFormula;
import org.francis.sat.solver.Clause;
import org.francis.sat.solver.PriorityIntHeap;
import org.francis.sat.solver.WorkSharer;
import org.francis.sat.solver.watched.clauserepo.FourWayWatchedClauseRepo;
import org.francis.sat.solver.watched.clauserepo.IWatchedClauseRepo;

public class WatchedFormula implements BooleanFormula, WorkSharer, Serializable {
    
    private static final long serialVersionUID = -2045142152043525717L;
    
    private final int varNum, clauseNum;
    private final byte[] varVals;
    private final IWatchedClauseRepo watchedClauseRepo;
    private final List<WatchedClause> initUnitClauses;
    private final List<WatchedClause> initSatClauses;
    private WorkPath path;
    protected final PriorityIntHeap freeVars;
    private boolean isTriviallyUnsat = false;
    
    WatchedFormula(int varNum, int clauseNum) {
        this.varNum = varNum;
        this.clauseNum = clauseNum;
        this.initUnitClauses = new ArrayList<WatchedClause>();
        this.initSatClauses = new ArrayList<WatchedClause>();
        this.watchedClauseRepo = new FourWayWatchedClauseRepo(varNum); // Unsafe release of object reference in constructor
        this.varVals = new byte[varNum+1];
        this.freeVars = new PriorityIntHeap(varNum);
    }
    
    public void init() {
        this.path = new WorkPath(this,varNum);
        for (int i = 1; i <= varNum; i++) {
            freeVars.insert(i);
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
            initSatClauses.add(newClause);
        }
        else if (check == WatchedClause.MADE_UNIT) {
            initUnitClauses.add(newClause);
        }
        else {
            for (int literal : newClause.getLiterals()) {
                int var = Clause.getVariable(literal);
                freeVars.incPriority(var, 1d);
            }
            watchedClauseRepo.addClause(newClause);
        }
    }
    
    public int chooseVariable() {
        return freeVars.peek();
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
    
    public boolean isConflicted(int literal) {
        int val = varVals[Clause.getVariable(literal)];
        if (val == 0) return false;
        return (literal%2 == 0 && val == -1) || (literal%2 == 1 && val == 1);
    }
    
    public boolean isSatisfied(int literal) {
        int val = varVals[Clause.getVariable(literal)];
        if (val == 0) return false;
        return (literal%2 == 0 && val == 1) || (literal%2 == 1 && val == -1);
    }
    
    public boolean isIndetermined(int literal) {
        assert !(varVals[Clause.getVariable(literal)] == 0 && freeVars.contains(Clause.getVariable(literal)));
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
        setLiteral0(literal,unitClauses);
        path.addToPath(literal, branchable, unit);
        assert checkState();
    }
    
    public void setLiteralForNewPath(int literal, boolean branchable) {
        setLiteral0(literal,new ArrayList<WatchedClause>());
    }
    
    private void setLiteral0(int literal, List<WatchedClause> unitClauses) {
        int var = Clause.getVariable(literal);
        freeVars.delete(var);
        varVals[var] = literal%2 == 0 ? (byte)1 : (byte)-1;
        watchedClauseRepo.setLiteral(literal, unitClauses);
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
        freeVars.insert(var);
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
    
    public boolean checkState() {
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
        Set<WatchedClause> clauseSet = watchedClauseRepo.getClauses();
        for (WatchedClause watchedClause : clauseSet) {
            clauses.add(watchedClause.getDimacsClause());
        }
        for (WatchedClause watchedClause : initUnitClauses) {
            clauses.add(watchedClause.getDimacsClause());
        }
        for (WatchedClause watchedClause : initSatClauses) {
            clauses.add(watchedClause.getDimacsClause());
        }
        assert clauses.size() == clauseNum;
        return clauses;
    }
}

package org.francis.sat.solver.watched;

import java.util.List;

import org.francis.sat.collections.HopScotchList;
import org.francis.sat.solver.Clause;

import static org.francis.sat.solver.watched.WatchedClause.watchOne;
import static org.francis.sat.solver.watched.WatchedClause.watchTwo;

public class WatchedClauseRepo {
    
    private final HopScotchList<WatchedClause>[] watchedClauseOne;
    private final HopScotchList<WatchedClause>[] watchedClauseTwo;
    private final static int originIdx = 1;
    
    @SuppressWarnings("unchecked")
    public WatchedClauseRepo(final int varNum) {
        this.watchedClauseOne = new HopScotchList[varNum+1];
        this.watchedClauseTwo = new HopScotchList[varNum+1];
        initArray(watchedClauseOne);
        initArray(watchedClauseTwo);
    }
    
    private void initArray(HopScotchList<WatchedClause>[] watchedArray ) {
        for (int i = originIdx; i < watchedArray.length; i++) {
            watchedArray[i] = new HopScotchList<WatchedClause>();
        }
    }
    
    public void addClause(WatchedClause clause) {
        int[] watches = clause.getWatchedLiterals();
        int var1 = Clause.getVariable(watches[watchOne]);
        int var2 = Clause.getVariable(watches[watchTwo]);
        watchedClauseOne[var1].add(clause);
        watchedClauseTwo[var2].add(clause);
        assert consistent();
    }
    
    public void setLiteral(int literal, List<WatchedClause> unitClauses) {
        setLiteral(literal,unitClauses,watchedClauseOne,watchOne);
        setLiteral(literal,unitClauses,watchedClauseTwo,watchTwo);
    }
    
    private void setLiteral(int literal, List<WatchedClause> unitClauses, HopScotchList<WatchedClause>[] watchArray, int watchIdx) {
        int var = Clause.getVariable(literal);
        HopScotchList<WatchedClause> watchedClauses = watchArray[var];
        for (int i = watchedClauses.size()-1; i >= 0; i--) {
            WatchedClause clause = watchedClauses.get(i);
            int newWatchedLiteral = clause.satisfying(literal,watchIdx);
            if (newWatchedLiteral >= 0) {
                watchArray[Clause.getVariable(newWatchedLiteral)].add(clause);
                watchedClauses.remove(i);
            }
            else if (newWatchedLiteral == WatchedClause.MADE_UNIT) {
                unitClauses.add(clause);
            }
        }
    }
    
    public int size() {
        return watchedClauseOne.length;
    }
    
    public HopScotchList<WatchedClause> getClauseListOne(int i) {
        return watchedClauseOne[i];
    }
    
    public HopScotchList<WatchedClause> getClauseListTwo(int i) {
        return watchedClauseTwo[i];
    }
    
    private boolean consistent() {
        assert consistent(watchedClauseOne, watchOne);
        assert consistent(watchedClauseTwo, watchTwo);
        return true;
    }
    
    private boolean consistent(HopScotchList<WatchedClause>[] watchArray, int watchIdx) {
        for (int i = originIdx; i < watchArray.length; i++) {
            HopScotchList<WatchedClause> clauses = watchArray[i];
            for (int j = 0; j < clauses.size(); j++) {
                WatchedClause clause = clauses.get(j);
                if (Clause.getVariable(clause.getWatchedLiterals()[watchIdx]) != i) 
                    return false;
            }
        }
        return true;
    }
}
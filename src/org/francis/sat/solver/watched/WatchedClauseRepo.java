package org.francis.sat.solver.watched;

import java.util.List;

import org.francis.sat.collections.HopScotchList;
import org.francis.sat.solver.Clause;

public class WatchedClauseRepo {
    
    private final HopScotchList<WatchedClause>[] watchedClauseArray;
    
    @SuppressWarnings("unchecked")
    public WatchedClauseRepo(int varNum) {
        this.watchedClauseArray = new HopScotchList[varNum+1];
        for (int i = 0; i <= varNum; i++) {
            watchedClauseArray[i] = new HopScotchList<WatchedClause>();
        }
    }
    
    public void addClause(WatchedClause clause) {
        int[] watches = clause.getWatchedLiterals();
        int var1Idx = Clause.getVariable(watches[0]);
        int var2Idx = Clause.getVariable(watches[1]);
        watchedClauseArray[var1Idx].add(clause);
        watchedClauseArray[var2Idx].add(clause);
    }
    
    public void watchClause(int var, WatchedClause clause) {
        watchedClauseArray[var].add(clause);
    }
    
    public void setLiteral(int literal, List<WatchedClause> unitClauses) {
        int var = Clause.getVariable(literal);
        HopScotchList<WatchedClause> watchedClauses = watchedClauseArray[var];
        for (int i = watchedClauses.size()-1; i >= 0; i--) {
            WatchedClause clause = watchedClauses.get(i);
            int newWatchedLiteral = clause.satisfying(literal);
            if (newWatchedLiteral >= 0) {
                watchedClauseArray[Clause.getVariable(newWatchedLiteral)].add(clause);
                watchedClauses.remove(i);
            }
            else if (newWatchedLiteral == WatchedClause.MADE_UNIT) {
                unitClauses.add(clause);
            }
        }
    }
    
    public int size() {
        return watchedClauseArray.length;
    }
    
    public HopScotchList<WatchedClause> getClauseList(int i) {
        return watchedClauseArray[i];
    }
}

package org.francis.sat.solver.watched.clauserepo;

import static org.francis.sat.solver.watched.WatchedClause.watchOne;
import static org.francis.sat.solver.watched.WatchedClause.watchTwo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.francis.sat.collections.HopScotchList;
import org.francis.sat.solver.Clause;
import org.francis.sat.solver.watched.WatchedClause;
import org.francis.sat.solver.watched.WatchedFormula;

public class SimpleWatchedClauseRepo implements IWatchedClauseRepo {
    
    private final HopScotchList<WatchedClause>[] watchedClausesByVar;
    private final WatchedFormula formula;
    private final static int ORIGIN_IDX = 1;
    
    @SuppressWarnings("unchecked")
    public SimpleWatchedClauseRepo(final int varNum, WatchedFormula formula) {
        this.formula = formula;
        this.watchedClausesByVar = new HopScotchList[varNum+1];
        for (int i = ORIGIN_IDX; i < watchedClausesByVar.length; i++) {
            watchedClausesByVar[i] = new HopScotchList<WatchedClause>();
        }
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.clauserepo.IWatchedClauseRepo#addClause(org.francis.sat.solver.watched.WatchedClause)
     */
    @Override
    public void addClause(WatchedClause clause) {
        int[] watches = clause.getWatchedLiterals();
        int lit1 = watches[watchOne];
        int var1 = Clause.getVariable(lit1);
        int lit2 = watches[watchTwo];
        int var2 = Clause.getVariable(lit2);
        watchedClausesByVar[var1].add(clause);
        watchedClausesByVar[var2].add(clause);
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.clauserepo.IWatchedClauseRepo#setLiteral(int, java.util.List)
     */
    public void setLiteral(int literal, List<WatchedClause> unitClauses) {
        assert consistent();
        int var = Clause.getVariable(literal);
        HopScotchList<WatchedClause> watchedClauses = watchedClausesByVar[var];
        for (int i = watchedClauses.size()-1; i >= 0; i--) {
            WatchedClause clause = watchedClauses.get(i);
            int watchIdx = determineWatch(clause,var);
            if (watchIdx < 0)
                continue;
            int newWatchedLiteral = clause.satisfying(literal,watchIdx);
            if (newWatchedLiteral >= 0) {
                int newVar = Clause.getVariable(newWatchedLiteral);
                watchedClausesByVar[newVar].add(clause);
                watchedClauses.remove(i);
            }
            else if (newWatchedLiteral == WatchedClause.MADE_UNIT) {
                unitClauses.add(clause);
            }
        }
        assert consistent();
    }
    
    private int determineWatch(WatchedClause clause, int var) {
        int[] literals = clause.getLiterals();
        int watchIdx;
        if (Clause.getVariable(literals[0]) == var )
            watchIdx = 0;
        else
            watchIdx = 1;
        if (formula.isSatisfied(literals[watchIdx]))
            return -1;
        else
            return watchIdx;
    }

    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.clauserepo.IWatchedClauseRepo#size()
     */
    @Override
    public int size() {
        return watchedClausesByVar.length;
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.clauserepo.IWatchedClauseRepo#getClauses()
     */
    @Override
    public Set<WatchedClause> getClauses() {
        Set<WatchedClause> clauseSet = new HashSet<WatchedClause>();
        getClauses(watchedClausesByVar, clauseSet);
        return clauseSet;
    }
    
    private void getClauses(HopScotchList<WatchedClause>[] watchArray, Set<WatchedClause> clauses) {
        for (int i = 0; i < watchArray.length; i++) {
            HopScotchList<WatchedClause> clauseList = watchArray[i];
            for (int j = 0; j < clauseList.size(); j++) {
                WatchedClause watchedClause = clauseList.get(j);
                clauses.add(watchedClause);
            }
        }
    }
    
    private boolean consistent() {
        assert consistent(watchedClausesByVar);
        return true;
    }
    
    private boolean consistent(HopScotchList<WatchedClause>[] watchArray) {
        for (int i = ORIGIN_IDX; i < watchArray.length; i++) {
            HopScotchList<WatchedClause> clauses = watchArray[i];
            for (int j = 0; j < clauses.size(); j++) {
                WatchedClause clause = clauses.get(j);
                if (Clause.getVariable(clause.getWatchedLiterals()[0]) == i)
                    continue;
                if (Clause.getVariable(clause.getWatchedLiterals()[1]) == i)
                    continue;
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append(getClauses().toString());
        return out.toString();
    }
}
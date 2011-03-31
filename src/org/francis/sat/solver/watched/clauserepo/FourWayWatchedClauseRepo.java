package org.francis.sat.solver.watched.clauserepo;

import static org.francis.sat.solver.watched.WatchedClause.watchOne;
import static org.francis.sat.solver.watched.WatchedClause.watchTwo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.francis.sat.collections.HopScotchList;
import org.francis.sat.solver.Clause;
import org.francis.sat.solver.watched.WatchedClause;

public class FourWayWatchedClauseRepo implements IWatchedClauseRepo {
    
    private final HopScotchList<WatchedClause>[] watchedClauseOneP;
    private final HopScotchList<WatchedClause>[] watchedClauseOneN;
    private final HopScotchList<WatchedClause>[] watchedClauseTwoP;
    private final HopScotchList<WatchedClause>[] watchedClauseTwoN;
    private final static int originIdx = 1;
    
    @SuppressWarnings("unchecked")
    public FourWayWatchedClauseRepo(final int varNum) {
        this.watchedClauseOneP = new HopScotchList[varNum+1];
        this.watchedClauseOneN = new HopScotchList[varNum+1];
        this.watchedClauseTwoP = new HopScotchList[varNum+1];
        this.watchedClauseTwoN = new HopScotchList[varNum+1];
        initArray(watchedClauseOneP);
        initArray(watchedClauseOneN);
        initArray(watchedClauseTwoP);
        initArray(watchedClauseTwoN);
    }
    
    private void initArray(HopScotchList<WatchedClause>[] watchedArray ) {
        for (int i = originIdx; i < watchedArray.length; i++) {
            watchedArray[i] = new HopScotchList<WatchedClause>();
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
        if (Clause.isPosLiteral(lit1))
            watchedClauseOneP[var1].add(clause);
        else
            watchedClauseOneN[var1].add(clause);
        if (Clause.isPosLiteral(lit2))
            watchedClauseTwoP[var2].add(clause);
        else
            watchedClauseTwoN[var2].add(clause);
        assert consistent();
    }
    
    private void watchClause(int literal, WatchedClause clause, int watchIdx) {
        int var = Clause.getVariable(literal);
        if (watchIdx == watchOne) {
            if (Clause.isPosLiteral(literal))
                watchedClauseOneP[var].add(clause);
            else
                watchedClauseOneN[var].add(clause);
        }
        else {
            if (Clause.isPosLiteral(literal))
                watchedClauseTwoP[var].add(clause);
            else
                watchedClauseTwoN[var].add(clause);
        }
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.clauserepo.IWatchedClauseRepo#setLiteral(int, java.util.List)
     */
    @Override
    public void setLiteral(int literal, List<WatchedClause> unitClauses) {
        if (Clause.isPosLiteral(literal)) {
            setLiteral(literal,unitClauses,watchedClauseOneN,watchOne);
            setLiteral(literal,unitClauses,watchedClauseTwoN,watchTwo);
        }
        else {
            setLiteral(literal,unitClauses,watchedClauseOneP,watchOne);
            setLiteral(literal,unitClauses,watchedClauseTwoP,watchTwo);
        }
    }
    
    private void setLiteral(int literal, List<WatchedClause> unitClauses, HopScotchList<WatchedClause>[] watchArray, int watchIdx) {
        int var = Clause.getVariable(literal);
        HopScotchList<WatchedClause> watchedClauses = watchArray[var];
        for (int i = watchedClauses.size()-1; i >= 0; i--) {
            WatchedClause clause = watchedClauses.get(i);
            int newWatchedLiteral = clause.satisfying(literal,watchIdx);
            if (newWatchedLiteral >= 0) {
                watchClause(newWatchedLiteral,clause,watchIdx);
                watchedClauses.remove(i);
            }
            else if (newWatchedLiteral == WatchedClause.MADE_UNIT) {
                unitClauses.add(clause);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.clauserepo.IWatchedClauseRepo#size()
     */
    @Override
    public int size() {
        return watchedClauseOneP.length;
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.clauserepo.IWatchedClauseRepo#getClauses()
     */
    @Override
    public Set<WatchedClause> getClauses() {
        Set<WatchedClause> clauseSet = new HashSet<WatchedClause>();
        getClauses(watchedClauseOneN, clauseSet);
        getClauses(watchedClauseOneP, clauseSet);
        getClauses(watchedClauseTwoN, clauseSet);
        getClauses(watchedClauseTwoP, clauseSet);
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
        assert consistent(watchedClauseOneP, watchOne, true);
        assert consistent(watchedClauseOneN, watchOne, false);
        assert consistent(watchedClauseTwoP, watchTwo, true);
        assert consistent(watchedClauseTwoN, watchTwo, false);
        return true;
    }
    
    private boolean consistent(HopScotchList<WatchedClause>[] watchArray, int watchIdx, boolean positive) {
        for (int i = originIdx; i < watchArray.length; i++) {
            HopScotchList<WatchedClause> clauses = watchArray[i];
            for (int j = 0; j < clauses.size(); j++) {
                WatchedClause clause = clauses.get(j);
                if (Clause.getVariable(clause.getWatchedLiterals()[watchIdx]) != i)
                    return false;
                if (Clause.isPosLiteral(clause.getWatchedLiterals()[watchIdx]) != positive)
                    return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append(Arrays.toString(watchedClauseOneP));
        out.append(Arrays.toString(watchedClauseOneN));
        out.append(Arrays.toString(watchedClauseTwoP));
        out.append(Arrays.toString(watchedClauseTwoN));
        return out.toString();
    }
}
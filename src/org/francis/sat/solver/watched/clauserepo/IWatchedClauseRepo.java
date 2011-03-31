package org.francis.sat.solver.watched.clauserepo;

import java.util.List;
import java.util.Set;

import org.francis.sat.solver.watched.WatchedClause;

public interface IWatchedClauseRepo {

    public abstract void addClause(WatchedClause clause);

    public abstract void setLiteral(int literal, List<WatchedClause> unitClauses);

    public abstract int size();

    public abstract Set<WatchedClause> getClauses();

}
package org.francis.sat.solver.immutable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.francis.sat.solver.BooleanFormula;
import org.francis.sat.solver.immutable.ImmutableSolver.FormulaState;

public class ImmutableFormula implements BooleanFormula, Serializable {
    
    private static final long serialVersionUID = 327202774606481321L;
    
    private List<ImmutableClause> clauses;
    
    protected ImmutableFormula(List<ImmutableClause> extClauses) {
        this.clauses = new ArrayList<ImmutableClause>(extClauses);
    }
    
    public static ImmutableFormula getInstance(List<ImmutableClause> extClauses) {
        List<ImmutableClause> intClauses = new ArrayList<ImmutableClause>(extClauses);
        return new ImmutableFormula(intClauses);
    }
    
    public FormulaState determineState() {
        if (clauses == null) return FormulaState.UNSAT;
        if (clauses.isEmpty()) return FormulaState.SAT;
        return FormulaState.IND;
    }
    
    public int chooseVariable() {
        return ImmutableClause.getVariable(clauses.get(0).getFirst());
    }
    
    public void unitPropogation() {
        List<Integer> unitLiterals = new ArrayList<Integer>();
        while (true) {
            boolean newUnitFound = false;
            CLAUSE_LOOP:
            for (int i = clauses.size()-1; i >= 0; i--) {
                ImmutableClause clause = clauses.get(i);
                Iterator<Integer> itr = unitLiterals.iterator();
                while (itr.hasNext()) {
                    int unitLit = itr.next();
                    ImmutableClause newClause = clause.satisfying(unitLit);
                    if (newClause == null) {
                        clauses.remove(i); // Here we have satisfied this clause
                        continue CLAUSE_LOOP;
                    }
                    if (newClause != clause) {
                        clause = newClause;
                        clauses.set(i, newClause);
                    }
                    if (clause.unsat()) {
                        clauses = null;
                        return;
                    }
                }
                if (clause.isUnit()) {
                    newUnitFound = true;
                    unitLiterals.add(clause.getFirst());
                    clauses.remove(i); // This clause is unit
                }
            }
            if (!newUnitFound) return;
        }
    }
    
    public ImmutableFormula setLiteral(int literal) {
        List<ImmutableClause> newState = new ArrayList<ImmutableClause>(clauses.size());
        for (ImmutableClause clause : clauses) {
            ImmutableClause newClause = clause.satisfying(literal);
            if (newClause != null)
                newState.add(newClause);
            if (newClause != null && newClause.unsat())
                return null;
        }
        return new ImmutableFormula(newState);
    }
}
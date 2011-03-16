package org.francis.sat.solver.immutable;

import java.util.ArrayList;
import java.util.List;

import org.francis.sat.solver.FormulaFactory;

public class ImmutableFormulaFactory implements FormulaFactory {

    int varNum;
    int clauseNum;
    List<ImmutableClause> clauses;
    
    @Override
    public void addClause(List<Integer> literals) {
        if (clauses == null) throw new IllegalStateException("You must call init before you call makeClause(...)"); 
        clauses.add(new ImmutableClause(literals));
    }

    @Override
    public void init(int varNum, int clauseNum) {
        this.varNum = varNum;
        this.clauseNum = clauseNum;
        this.clauses = new ArrayList<ImmutableClause>();
    }

    @Override
    public ImmutableFormula retrieveFormula() {
        return new ImmutableFormula(clauses);
    }
}

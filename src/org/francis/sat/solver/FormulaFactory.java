package org.francis.sat.solver;

import java.util.List;

public interface FormulaFactory {

    public void init(int varNum, int clauseNum);
    
    public void addClause(List<Integer> literals);
    
    public BooleanFormula retrieveFormula();
}

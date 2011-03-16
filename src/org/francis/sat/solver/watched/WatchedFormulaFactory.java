package org.francis.sat.solver.watched;

import java.util.ArrayList;
import java.util.List;

import org.francis.sat.solver.FormulaFactory;

public class WatchedFormulaFactory implements FormulaFactory {

    int varNum;
    int clauseNum;
    WatchedFormula formula;
    List<List<Integer>> rawClauses;
    
    @Override
    public void init(int varNum, int clauseNum) {
        this.varNum = varNum;
        this.clauseNum = clauseNum;
        rawClauses = new ArrayList<List<Integer>>(clauseNum);
    }
    
    private void checkState() {
        if (rawClauses == null) throw new IllegalStateException("You must call init() before you call cany other methods on this class.");
    }
    @Override
    public void addClause(List<Integer> literals) {
        checkState();
        rawClauses.add(literals);
    }

    @Override
    public WatchedFormula retrieveFormula() {
        checkState();
        WatchedFormula formula = new WatchedFormula(varNum,clauseNum);
        for (List<Integer> rawClause : rawClauses) {
            formula.addClause(rawClause);
        }
        formula.init();
        return formula;
    }
}

package org.francis.sat.solver;

import java.util.List;

public interface BooleanFormula {

    public int getVarNum();
    
    public int getClauseNum();
    
    public List<List<Integer>> getDimacsClauses();
}
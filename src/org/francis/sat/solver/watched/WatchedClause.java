package org.francis.sat.solver.watched;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.francis.sat.solver.Clause;

public class WatchedClause extends Clause implements Serializable{
    
    private static final long serialVersionUID = 8744962775672145582L;
    
    public static final int MADE_UNIT = -1;
    public static final int WATCH_UNCHANGED = -2;
    public static final int MADE_SAT = -3;
    public static final int MADE_UNSAT = -4;
    
    int watchOne;
    int watchTwo;
    final private int[] literals;
    private final WatchedFormula formula;

    public WatchedClause(List<Integer> literalsList, WatchedFormula formula) {
        this.formula = formula;
        Set<Integer> literalsSet = new HashSet<Integer>(literalsList);
        this.literals = new int[literalsSet.size()];
        int index = 0;
        for (Integer literal : literalsSet) {
            this.literals[index] = literal;
            index++;
        }
        watchOne = 0;
        watchTwo = 1;
    }

    public int[] getWatchedLiterals() {
        return new int[] {literals[watchOne],literals[watchTwo]};
    }
    
    // NB: Caller should be certain that this clause is (or recently was) unit
    public int getUnitLiteral() {
        if (literals.length == 1) return literals[0];
        boolean watchOneCon = formula.isConflicted(literals[watchOne]);
        if (watchOneCon) 
            return literals[watchTwo];
        else
            return literals[watchOne];
    }
    
    public boolean isUnit() {
        boolean watchOneCon = formula.isConflicted(literals[watchOne]);
        boolean watchTwoInd = formula.isIndetermined(literals[watchTwo]);
        if (watchOneCon && watchTwoInd) return true;
        boolean watchTwoCon = formula.isConflicted(literals[watchTwo]);
        boolean watchOneInd = formula.isIndetermined(literals[watchOne]);
        return (watchTwoCon && watchOneInd);
    }

    public int satisfying(int literal) {
        assert Clause.getVariable(literals[watchOne]) == Clause.getVariable(literal) || Clause.getVariable(literals[watchTwo]) == Clause.getVariable(literal);
        int var = Clause.getVariable(literal);
        if (literal == literals[watchOne] || literal == literals[watchTwo]) return WATCH_UNCHANGED;
        if (Clause.getVariable(literals[watchOne]) == var) {
            int newWatch = watchOne;
            while(true) {
                newWatch = (newWatch+1)%literals.length;
                if (newWatch == watchTwo) continue;
                if (newWatch == watchOne) {
                    if (isUnit()) return MADE_UNIT;
                    return WATCH_UNCHANGED;
                }
                if (formula.isIndetermined(literals[newWatch]) || formula.isSatisfied(literals[newWatch])) {
                    watchOne = newWatch;
                    return literals[watchOne];
                }
            }
        }
        else { // Clause.getVariable(literals[watchTwo]) == var
            int newWatch = watchTwo;
            while(true) {
                newWatch = (newWatch+1)%literals.length;
                if (newWatch == watchOne) continue;
                if (newWatch == watchTwo) {
                    if (isUnit()) return MADE_UNIT;
                    return WATCH_UNCHANGED;
                }
                if (formula.isIndetermined(literals[newWatch]) || formula.isSatisfied(literals[newWatch])) {
                    watchTwo = newWatch;
                    return literals[watchTwo];
                }
            }
        }
    }
    
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<");
        for (int i = 0; i < literals.length; i++) {
            int varName = Clause.getDimacs(literals[i]);
            builder.append(varName);
            if (i != literals.length-1) {
                builder.append(" ");
            }
        }
        builder.append(">");
        return builder.toString();
    }

    public int preCheck() {
        if (literals.length == 1) {
            return MADE_UNIT;
        }
        for (int i = 0; i < literals.length; i++) {
            int literal = literals[i];
            for (int j = 0; j < literals.length; j++) {
                int compare = literals[j];
                if (Clause.compliment(literal) == compare) {
                    return MADE_SAT;
                }
            }
        }
        return 0;
    }

    @Override
    public List<Integer> getDimacsClause() {
        List<Integer> dimacsClause = new ArrayList<Integer>(literals.length);
        for (int literal : this.literals) {
            dimacsClause.add(this.getDimacs(literal));
        }
        return dimacsClause;
    }
}
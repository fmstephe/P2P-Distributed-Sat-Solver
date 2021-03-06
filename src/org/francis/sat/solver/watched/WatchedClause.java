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
    
    public static final int watchOne = 0;
    public static final int watchTwo = 1;
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
    }

    public int[] getWatchedLiterals() {
        return new int[] {literals[watchOne],literals[watchTwo]};
    }
    
    // NB: Caller must be certain that this clause is (or recently was) unit
    public int getUnitLiteral() {
        if (literals.length == 1) return literals[0];
        boolean watchOneCon = formula.isConflicted(literals[watchOne]);
        if (watchOneCon) 
            return literals[watchTwo];
        else
            return literals[watchOne];
    }
    
    public int satisfying(int literal, int watchIdx) {
        assert Clause.getVariable(literals[watchIdx]) == Clause.getVariable(literal);
        for (int i = 2; i < literals.length; i++) {
            int newLiteral = literals[i];
            if (!formula.isConflicted(newLiteral)) {
                int oldLiteral = literals[watchIdx];
                literals[watchIdx] = newLiteral;
                literals[i] = oldLiteral;
                return newLiteral;
            }
        }
        if (formula.isIndetermined(literals[watchIdx^1])) return MADE_UNIT;
        return WATCH_UNCHANGED;
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
            dimacsClause.add(Clause.getDimacs(literal));
        }
        return dimacsClause;
    }
    
    // NB: Unsafe sharing of literals - don't be careless with this array, caller
    public int[] getLiterals() {
        return literals;
    }
}
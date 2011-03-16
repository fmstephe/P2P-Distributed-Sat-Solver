package org.francis.sat.solver.immutable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.francis.sat.solver.Clause;

public class ImmutableClause extends Clause implements Serializable {
    
    private static final long serialVersionUID = 8744962775672145582L;
    
    final private int[] literals;

    public ImmutableClause(List<Integer> literalsList) {
        Set<Integer> copiedLiterals = new HashSet<Integer>(literalsList);
        this.literals = new int[copiedLiterals.size()];
        int index = 0;
        for (Integer literal : copiedLiterals) {
            this.literals[index] = literal;
            index++;
        }
    }

    private ImmutableClause(int[] literals) {
        this.literals = literals;
    }

    public ImmutableClause satisfying(int literal) {
        if (contains(literal)) {
            return null;
        }
        if (!contains(literal ^ 1)) {
            return this;
        }
        // This bit assumes that there is exactly one occurrance of a given
        // literal in a clause
        int[] newLiterals = new int[literals.length-1];
        int invalidFound = 0;
        for (int i = 0; i < literals.length; i++) {
            if (literals[i] == (literal ^ 1)) {
                invalidFound = 1;
                continue;
            }
            newLiterals[i-invalidFound] = literals[i];
        }
        return new ImmutableClause(newLiterals);
    }

    public boolean contains(int literal) {
        for (int i : literals)
            if (i == literal)
                return true;
        return false;
    }

    public int getFirst() {
        if (literals.length > 0)
            return literals[0];
        else
            return -1;
    }

    public boolean unsat() {
        return literals.length == 0;
    }

    public boolean isUnit() {
        return literals.length == 1;
    }
    
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int literal : literals) {
            int varName = literal%2 == 0 ? getVariable(literal) : -getVariable(literal);
            builder.append(varName);
            builder.append(" ");
        }
        return builder.toString();
    }
}
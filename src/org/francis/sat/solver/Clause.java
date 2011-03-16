package org.francis.sat.solver;

import java.util.List;


public abstract class Clause {
    
    public static int getVariable(int literal) {
        return literal >> 1;
    }
    
    public static int getDimacs(int literal) {
        return literal%2 == 0 ? getVariable(literal) : -getVariable(literal);
    }
    
    public static int literalFromDimacs(int dimacs) {
        return dimacs < 0 ? (dimacs*-2)+1 : dimacs*2;
    }
    
    public static int posLiteral(int variable) {
        return variable << 1;
    }
    
    public static int negLiteral(int variable) {
        return (variable << 1)+1;
    }

    public static int compliment(int literal) {
        return literal ^ 1;
    }
    
    public abstract List<Integer> getDimacsClause();
}
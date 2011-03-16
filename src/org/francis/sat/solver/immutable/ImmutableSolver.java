package org.francis.sat.solver.immutable;

import java.util.ArrayList;
import java.util.List;

import org.francis.sat.network.NetworkManager;
import org.francis.sat.solver.SatSolver;
import org.francis.sat.solver.WorkSharer;

public class ImmutableSolver implements SatSolver, WorkSharer {
    
    public enum FormulaState {SAT,UNSAT,IND};
    
    private List<ImmutableFormula> stack;
    private final NetworkManager networkManager;
    
    public ImmutableSolver(NetworkManager networkManager, ImmutableFormula formula) {
        this.networkManager = networkManager;
        this.stack = new ArrayList<ImmutableFormula>();
        stack.add(formula);
    }
    
    public ImmutableSolver(NetworkManager networkManager) {
        this.networkManager = networkManager;
        this.stack = new ArrayList<ImmutableFormula>();
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.ISatSolver#solve()
     */
    public boolean solve() {
        while(true) {
            if (networkManager != null) networkManager.manageNetwork(this);
            if (stack.isEmpty()) return sendAndReturn(false); // This should only ever be true if the networkManager is null
            ImmutableFormula branchState = stack.remove(stack.size()-1);
            FormulaState s = checkState(branchState);
            if (s == FormulaState.SAT) return true;
            if (s == FormulaState.UNSAT) continue;
            int p = branchState.chooseVariable();
            ImmutableFormula positive = branchState.setLiteral(ImmutableClause.posLiteral(p));
            positive.unitPropogation();
            ImmutableFormula negative = branchState.setLiteral(ImmutableClause.negLiteral(p));
            negative.unitPropogation();
            stack.add(negative);
            stack.add(positive);
        }
    }
    
    private boolean sendAndReturn(boolean result) {
        if (networkManager != null) networkManager.sendResult(result);
        return result;
    }
    
    private FormulaState checkState (ImmutableFormula formula) {
        if (formula == null) return FormulaState.UNSAT;
        return formula.determineState();
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.ISatSolver#giveWork()
     */
    public List<ImmutableFormula> giveWork() {
        System.out.println("Giving Work");
        List<ImmutableFormula> keptClauses = new ArrayList<ImmutableFormula>(stack.size()/2+1);
        List<ImmutableFormula> sharedClauses = new ArrayList<ImmutableFormula>(stack.size()/2);
        for (int i = 0; i < stack.size(); i++) {
            if (i%2 == 0)
                keptClauses.add(stack.get(i));
            else
                sharedClauses.add(stack.get(i));
        }
        this.stack = keptClauses;
        return sharedClauses;
    }

    /* (non-Javadoc)
     * @see org.francis.sat.solver.ISatSolver#receiveWork(java.util.List)
     */
    @SuppressWarnings("unchecked")
    public void receiveWork(List<?> stack) {
        System.out.println("Work Received");
        this.stack = (List<ImmutableFormula>)stack;
    }

    /* (non-Javadoc)
     * @see org.francis.sat.solver.ISatSolver#workSize()
     */
    public int sharableWork() {
        return stack.size();
    }

    @Override
    public boolean isComplete() {
        return stack.get(stack.size()-1).determineState() == FormulaState.SAT;
    }
}

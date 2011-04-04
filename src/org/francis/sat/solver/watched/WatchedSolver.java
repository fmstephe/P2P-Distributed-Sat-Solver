package org.francis.sat.solver.watched;

import org.francis.sat.network.NetworkManager;
import org.francis.sat.solver.Clause;
import org.francis.sat.solver.SatSolver;

public class WatchedSolver implements SatSolver {
    
    private WatchedFormula formula;
    private final NetworkManager networkManager;
    private final boolean firstWorkingSolver;
    
    public WatchedSolver(NetworkManager networkManager, WatchedFormula formula, boolean firstWorkingSolver) {
        this.networkManager = networkManager;
        this.formula = formula;
        this.firstWorkingSolver = firstWorkingSolver;
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.ISatSolver#solve()
     */
    @Override
    public boolean solve() {
        if (firstWorkingSolver) {
            int var = formula.chooseVariable();
            int literal = Clause.posLiteral(var);
            formula.tryLiteral(literal);
        }
        while(true) {
            boolean stillRunning = networkManager.manageNetwork(formula);
            if (!stillRunning) {
                return false;
            }
            int var = formula.chooseVariable();
            int literal = Clause.posLiteral(var);
            formula.tryLiteral(literal);
        }
    }
}

package org.francis.sat.solver.watched;

import org.francis.sat.network.NetworkManager;
import org.francis.sat.solver.Clause;
import org.francis.sat.solver.SatSolver;

public class WatchedSolver implements SatSolver {
    
    private WatchedFormula formula;
    private final NetworkManager networkManager;
    
    public WatchedSolver(NetworkManager networkManager, WatchedFormula formula) {
        this.networkManager = networkManager;
        this.formula = formula;
    }
    
    public WatchedSolver(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.ISatSolver#solve()
     */
    public boolean solve() {
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

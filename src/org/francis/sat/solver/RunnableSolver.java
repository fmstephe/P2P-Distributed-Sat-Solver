package org.francis.sat.solver;



public class RunnableSolver implements Runnable {
    
    public final SatSolver solver;
    
    public RunnableSolver(SatSolver solver) {
        this.solver = solver;
    }

    @Override
    public void run() {
        solver.solve();
    }
}

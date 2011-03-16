package org.francis.sat.solver.watched;

import org.francis.sat.network.NetworkManager;
import org.francis.sat.solver.BooleanFormula;
import org.francis.sat.solver.SatSolver;
import org.francis.sat.solver.SatSolverFactory;

public class WatchedSolverFactory implements SatSolverFactory {

    @Override
    public SatSolver createSatSolver(NetworkManager networkManager, BooleanFormula formula) {
        return new WatchedSolver(networkManager,(WatchedFormula)formula);
    }
}

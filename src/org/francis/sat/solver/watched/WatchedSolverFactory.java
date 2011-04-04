package org.francis.sat.solver.watched;

import org.francis.sat.network.NetworkManager;
import org.francis.sat.solver.BooleanFormula;
import org.francis.sat.solver.SatSolver;

public class WatchedSolverFactory {

    public SatSolver createSatSolver(NetworkManager networkManager, BooleanFormula formula,boolean firstWorkingSolver) {
        return new WatchedSolver(networkManager,(WatchedFormula)formula,firstWorkingSolver);
    }
}
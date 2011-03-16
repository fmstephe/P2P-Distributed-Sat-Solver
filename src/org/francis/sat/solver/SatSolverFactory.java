package org.francis.sat.solver;

import org.francis.sat.network.NetworkManager;

public interface SatSolverFactory {

    public SatSolver createSatSolver(NetworkManager networkManager, BooleanFormula formula);
}

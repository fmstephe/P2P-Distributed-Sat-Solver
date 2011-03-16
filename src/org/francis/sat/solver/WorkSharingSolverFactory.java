package org.francis.sat.solver;

import org.francis.sat.network.NetworkManager;

public interface WorkSharingSolverFactory {

    public SatSolver createWorkSharingSolver(NetworkManager networkManager, BooleanFormula formula);

    public SatSolver createWorkSharingSolver(NetworkManager nwm);
}

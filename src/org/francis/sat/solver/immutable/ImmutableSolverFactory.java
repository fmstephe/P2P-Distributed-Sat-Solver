package org.francis.sat.solver.immutable;

import org.francis.sat.network.NetworkManager;
import org.francis.sat.solver.BooleanFormula;
import org.francis.sat.solver.SatSolver;
import org.francis.sat.solver.SatSolverFactory;

public class ImmutableSolverFactory implements SatSolverFactory {

    @Override
    public SatSolver createSatSolver(NetworkManager networkManager, BooleanFormula formula) {
        if (formula == null) {
            return new ImmutableSolver(networkManager);
        }
        else {
            return new ImmutableSolver(networkManager,(ImmutableFormula)formula);
        }
    }
}

package org.francis.sat.solver.watched;

import org.francis.sat.network.Communicator;
import org.francis.sat.network.NetworkManager;
import org.francis.sat.network.smp.SMPCommunicator;
import org.francis.sat.network.smp.SMPMessageManager;
import org.francis.sat.network.smp.SMPWorkerId;
import org.francis.sat.solver.RunnableSolver;
import org.francis.sat.solver.SatSolver;

public class SMPThreadedWatchedSolverFactory {
    
    private final WatchedSolverFactory solverFactory;
    private final WatchedFormulaFactory formulaFactory;
    private SMPMessageManager messageManager;
    
    public SMPThreadedWatchedSolverFactory(WatchedSolverFactory solverFactory, WatchedFormulaFactory formulaFactory) {
        this.solverFactory = solverFactory;
        this.formulaFactory = formulaFactory;
    }
    
    public SMPMessageManager createAndRunSolversLocal(int networkSize, int worksharingThreshold, int initHibernate, int maxHibernate, String logFilePath) {
        NetworkManager[] networkManagers = createNetworkManagers(networkSize, worksharingThreshold, networkSize, initHibernate, maxHibernate, logFilePath);
        RunnableSolver[] rSolvers = createRunnableSolvers(networkManagers);
        setThoseBastardsRunning(rSolvers);
        return messageManager; // The contract between a caller who provides a listener and the callee that provides a message-manager for that listener is fucking tenuous and non-intuitive.  Bullshit code.
    }
    
    private void setThoseBastardsRunning(RunnableSolver[] rSolvers) {
        for (int i = 0; i < rSolvers.length; i++) {
            RunnableSolver rSolver = rSolvers[i];
            Thread thread = new Thread(rSolver);
            thread.setName("Solver_"+i);
            thread.start();
        }
    }

    private RunnableSolver[] createRunnableSolvers(NetworkManager[] networkManagers) {
        RunnableSolver[] rSolvers = new RunnableSolver[networkManagers.length];
        for (int i = 0; i < networkManagers.length; i++) {
            NetworkManager nwm = networkManagers[i];
            SatSolver solver = solverFactory.createSatSolver(nwm,formulaFactory.retrieveFormula());
            rSolvers[i] = new RunnableSolver(solver);
        }
        return rSolvers;
    }
    
    private NetworkManager[] createNetworkManagers(int networkSize, int workSharingThreshold, int threadCount, int initHibernate, int maxHibernate, String logFilePath) {
        NetworkManager[] networkManagers = new NetworkManager[threadCount];
        SMPWorkerId[] workers = new SMPWorkerId[threadCount];
        for (int i = 0; i < threadCount; i++) {
            workers[i] = new SMPWorkerId(i);
        }
        messageManager = new SMPMessageManager(workers);
        if (threadCount == 1) {
            Communicator comm = new SMPCommunicator(messageManager,null,null,workers[0]);
            networkManagers[0] = new NetworkManager(comm,networkSize,workSharingThreshold,initHibernate,maxHibernate,true,logFilePath);
        }
        else {
            for (int i = 0; i < threadCount; i++) {
                Communicator comm = null;
                if (i == 0)
                    comm = new SMPCommunicator(messageManager,null,workers[i+1],workers[i]);
                else if (i == threadCount-1)
                    comm = new SMPCommunicator(messageManager,workers[i-1],null,workers[i]);
                else
                    comm = new SMPCommunicator(messageManager,workers[i-1],workers[i+1],workers[i]);
                networkManagers[i] = new NetworkManager(comm,networkSize,workSharingThreshold,initHibernate,maxHibernate,(i==0),logFilePath);
            }
        }
        return networkManagers;
    }
}

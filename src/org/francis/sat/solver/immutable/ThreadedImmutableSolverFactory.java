package org.francis.sat.solver.immutable;

import org.francis.sat.network.NetworkManager;
import org.francis.sat.network.WorkerId;
import org.francis.sat.network.erlang.ErlangWorkerId;
import org.francis.sat.solver.RunnableSolver;
import org.francis.sat.solver.SatSolver;
import org.francis.sat.solver.ThreadedSolverFactory;

import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpMbox;
import com.ericsson.otp.erlang.OtpNode;

public class ThreadedImmutableSolverFactory implements ThreadedSolverFactory {
    
    ImmutableSolverFactory solverFactory;
    ImmutableFormulaFactory formulaFactory;
    
    public ThreadedImmutableSolverFactory(ImmutableSolverFactory solverFactory, ImmutableFormulaFactory formulaFactory) {
        this.solverFactory = solverFactory;
        this.formulaFactory = formulaFactory;
    }
    
    @Override
    public void createAndRunSolversLocal(OtpErlangPid listeningProcess, OtpNode node, int networkSize, int worksharingThreshold, int threadCount) {
        ErlangWorkerId topWorker = new ErlangWorkerId(null);
        WorkerId bottomWorker = new ErlangWorkerId(null);
        NetworkManager[] networkManagers = createNetworkManagers(topWorker, bottomWorker, listeningProcess, node, networkSize, worksharingThreshold, threadCount);
        RunnableSolver[] rSolvers = createRunnableSolvers(networkManagers);
        setThoseBastardsRunning(rSolvers);
    }
    
    private void setThoseBastardsRunning(RunnableSolver[] rSolvers) {
        for (RunnableSolver rSolver : rSolvers) {
            Thread thread = new Thread(rSolver);
            thread.start();
        }
    }

    private RunnableSolver[] createRunnableSolvers(NetworkManager[] networkManagers) {
        RunnableSolver[] rSolvers = new RunnableSolver[networkManagers.length];
        for (int i = 0; i < networkManagers.length; i++) {
            NetworkManager nwm = networkManagers[i];
            SatSolver solver;
            if (i == 0)
                solver = solverFactory.createSatSolver(nwm,formulaFactory.retrieveFormula());
            else
                solver = solverFactory.createSatSolver(nwm,null);
            rSolvers[i] = new RunnableSolver(solver);
        }
        return rSolvers;
    }

    private NetworkManager[] createNetworkManagers(ErlangWorkerId topWorker, WorkerId bottomWorker, OtpErlangPid listeningProcess, OtpNode node,
            int networkSize, int workSharingThreshold, int threadCount) {
        NetworkManager[] networkManagers = new NetworkManager[threadCount];
        OtpMbox newMbox = node.createMbox();
        ErlangWorkerId newWorker = new ErlangWorkerId(newMbox.self());
        WorkerId nextWorkerDown = createNetworkManagers(newWorker,bottomWorker,listeningProcess,node,networkSize,workSharingThreshold,threadCount-1,networkManagers);
        networkManagers[threadCount-1] = new NetworkManager(topWorker,nextWorkerDown,listeningProcess,newMbox,node,networkSize,workSharingThreshold,false);
        return networkManagers;
    }

    private WorkerId createNetworkManagers(ErlangWorkerId topProcess, WorkerId bottomProcess, OtpErlangPid listeningProcess, OtpNode node, int networkSize, int workSharingThreshold, int threadCount, NetworkManager[] networkManagers) {
        OtpMbox newMbox = node.createMbox();
        ErlangWorkerId newWorker = new ErlangWorkerId(newMbox.self());
        if (threadCount == 1) {
            networkManagers[0] = new NetworkManager(topProcess,bottomProcess,listeningProcess,newMbox,node,networkSize,workSharingThreshold,false);
            return newWorker;
        }
        WorkerId nextPidDown = createNetworkManagers(newWorker,bottomProcess,listeningProcess,node,networkSize,workSharingThreshold,threadCount-1,networkManagers);
        networkManagers[threadCount-1] = new NetworkManager(topProcess,nextPidDown,listeningProcess,newMbox,node,networkSize,workSharingThreshold,false);
        return newWorker;
    }
}

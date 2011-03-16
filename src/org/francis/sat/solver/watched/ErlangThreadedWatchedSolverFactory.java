package org.francis.sat.solver.watched;

import org.francis.sat.network.Communicator;
import org.francis.sat.network.NetworkManager;
import org.francis.sat.network.erlang.ErlangCommunicator;
import org.francis.sat.solver.RunnableSolver;
import org.francis.sat.solver.SatSolver;
import org.francis.sat.solver.ThreadedSolverFactory;

import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpMbox;
import com.ericsson.otp.erlang.OtpNode;

public class ErlangThreadedWatchedSolverFactory implements ThreadedSolverFactory {
    
    private final WatchedSolverFactory solverFactory;
    private final WatchedFormulaFactory formulaFactory;
    
    public ErlangThreadedWatchedSolverFactory(WatchedSolverFactory solverFactory, WatchedFormulaFactory formulaFactory) {
        this.solverFactory = solverFactory;
        this.formulaFactory = formulaFactory;
    }
    
    /* (non-Javadoc)
     * @see org.francis.sat.solver.watched.ThreadedSolverFactory#createAndRunSolversLocal(org.francis.sat.solver.watched.WatchedSolverFactory, org.francis.sat.solver.watched.WatchedFormulaFactory, com.ericsson.otp.erlang.OtpErlangPid, com.ericsson.otp.erlang.OtpNode, int, int, int)
     */
    @Override
    public void createAndRunSolversLocal(OtpErlangPid listeningProcess, OtpNode node, int networkSize, int worksharingThreshold) {
        NetworkManager[] networkManagers = createNetworkManagers(null, null, listeningProcess, node, networkSize, worksharingThreshold, networkSize);
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
            SatSolver solver = solverFactory.createSatSolver(nwm,formulaFactory.retrieveFormula());
            rSolvers[i] = new RunnableSolver(solver);
        }
        return rSolvers;
    }
    
    private NetworkManager[] createNetworkManagers(OtpErlangPid topProcess, OtpErlangPid bottomProcess, OtpErlangPid listeningProcess, OtpNode node,
            int networkSize, int workSharingThreshold, int threadCount) {
        NetworkManager[] networkManagers = new NetworkManager[threadCount];
        int threadIdx = threadCount-1;
        OtpMbox newMbox = node.createMbox();
        OtpErlangPid newProcess = newMbox.self();
        OtpErlangPid nextProcessDown;
        if (threadCount > 1) {
            nextProcessDown = createNetworkManagers(newProcess,bottomProcess,listeningProcess,node,networkSize,workSharingThreshold,threadIdx-1,networkManagers);
        }
        else {
            nextProcessDown = null;
        }
        Communicator comm = new ErlangCommunicator(newMbox, topProcess, nextProcessDown, listeningProcess);
        networkManagers[threadIdx] = new NetworkManager(comm,networkSize,workSharingThreshold,true);
        return networkManagers;
    }

    private OtpErlangPid createNetworkManagers(OtpErlangPid topProcess, OtpErlangPid bottomProcess, OtpErlangPid listeningProcess, OtpNode node, int networkSize, int workSharingThreshold, int threadIdx, NetworkManager[] networkManagers) {
        OtpMbox newMbox = node.createMbox();
        OtpErlangPid newProcess = newMbox.self();
        if (threadIdx == 0) {
            Communicator comm = new ErlangCommunicator(newMbox, topProcess, bottomProcess, listeningProcess);
            networkManagers[threadIdx] = new NetworkManager(comm,networkSize,workSharingThreshold,false);
            return newProcess;
        }
        OtpErlangPid nextProcessDown = createNetworkManagers(newProcess,bottomProcess,listeningProcess,node,networkSize,workSharingThreshold,threadIdx-1,networkManagers);
        Communicator comm = new ErlangCommunicator(newMbox, topProcess, nextProcessDown, listeningProcess);
        networkManagers[threadIdx] = new NetworkManager(comm,networkSize,workSharingThreshold,false);
        return newProcess;
    }
}

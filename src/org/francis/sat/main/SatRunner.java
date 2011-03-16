package org.francis.sat.main;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.francis.sat.io.DimacsReader;
import org.francis.sat.solver.watched.ErlangThreadedWatchedSolverFactory;
import org.francis.sat.solver.watched.WatchedFormulaFactory;
import org.francis.sat.solver.watched.WatchedSolverFactory;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.reader.Reader;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangBoolean;
import com.ericsson.otp.erlang.OtpErlangDecodeException;
import com.ericsson.otp.erlang.OtpErlangExit;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangString;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpMbox;
import com.ericsson.otp.erlang.OtpNode;

public class SatRunner {
    
    // Arg fields
    int uniqueId;
    String nodeName;
    String regName;
    String cookie;
    String listeningName;
    String listeningNode;
    String clauseFile;
    boolean isDistributed;
    
    // Formula
    WatchedFormulaFactory formulaFactory;
    WatchedSolverFactory solverFactory;
    
    // Erlang fields
    OtpNode node;
    OtpMbox mbox;
    
    // Work-sharing fields
    private OtpErlangPid topProcess;
    private OtpErlangPid bottomProcess;
    private OtpErlangPid listeningProcess;
    private OtpErlangLong networkSize;
    private OtpErlangLong worksharingThreshold;
    private int threadCount;

    public static void mainDisabled (String[] args) throws Exception {
        try {
            SatRunner runner = null;
            try {
                runner = new SatRunner();
                runner.initFactories();
                runner.initArgs(args);
                runner.setupErlang();
                runner.initComm();
                runner.readFormula();
                runner.runSolver();
                while(true) {
                    Thread.sleep(10000);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                if (runner != null) runner.sendFailMsg();
                while(true) {
                    Thread.sleep(100000);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            while(true) {
                Thread.sleep(100000);
            }
        }
    }
    
    private void initFactories() {
        solverFactory = new WatchedSolverFactory();
        formulaFactory = new WatchedFormulaFactory();
    }

    private void sendFailMsg() {
        mbox.send(listeningName, listeningNode, new OtpErlangAtom("failure"));
    }

    private void runSolver() throws Exception {
//        BufferedReader bReader;
//        bReader = new BufferedReader(new InputStreamReader(System.in));
//        bReader.readLine();
        if (isDistributed) {
            System.out.println("Solving using distributed solver");
            ErlangThreadedWatchedSolverFactory threadedSolverFactory = new ErlangThreadedWatchedSolverFactory(solverFactory,formulaFactory);
//            threadedSolverFactory.createAndRunSolversLocal(listeningProcess, node, networkSize.intValue(), worksharingThreshold.intValue(), threadCount);
//            NetworkManager networkManager = new NetworkManager(topProcess,bottomProcess,listeningProcess,mbox,node,networkSize.intValue());
//            BasicSolver solver = clauses == null ? new BasicSolver(networkManager,worksharingThreshold.intValue()) : new BasicSolver(networkManager,clauses,worksharingThreshold.intValue());
//            boolean result = solver.solve();
//            System.out.println("Solution Result = "+result);
        }
        else {
            System.out.println("Solving using Sat4J solver");
            System.out.println(clauseFile);
            File file = new File(clauseFile);
            ISolver solver = SolverFactory.newDefault();
            solver.setTimeout(3600); // 1 hour timeout
            Reader reader = new org.sat4j.reader.DimacsReader(solver);
            IProblem problem = reader.parseInstance(file.getAbsolutePath());
            boolean result = problem.isSatisfiable();
            System.out.println("Sat4J solution result = "+result);
            mbox.send(listeningName, listeningNode, new OtpErlangBoolean(result));
            System.exit(0);
        }
    }

    private void initComm() throws OtpErlangExit, OtpErlangDecodeException {
        OtpErlangObject[] replyBody = new OtpErlangObject[] {new OtpErlangAtom("ready"),mbox.self()};
        mbox.send(listeningName, listeningNode, new OtpErlangTuple(replyBody));
        OtpErlangTuple initMessage = (OtpErlangTuple)mbox.receive();
        System.out.println("InitMessage = "+initMessage);
//        OtpErlangAtom messageHeader = (OtpErlangAtom)initMessage.elementAt(0);
        topProcess = initMessage.elementAt(1) instanceof OtpErlangPid ? (OtpErlangPid)initMessage.elementAt(1) : null;
        bottomProcess = initMessage.elementAt(2) instanceof OtpErlangPid ? (OtpErlangPid)initMessage.elementAt(2) : null;
        listeningProcess = (OtpErlangPid)initMessage.elementAt(3);
        networkSize = (OtpErlangLong)initMessage.elementAt(4);
        worksharingThreshold = (OtpErlangLong)initMessage.elementAt(5);
        System.out.println(initMessage.elementAt(6).getClass());
        clauseFile = initMessage.elementAt(6) instanceof OtpErlangString ? ((OtpErlangString)initMessage.elementAt(6)).stringValue() : null;
    }

    private void readFormula() {
        if (clauseFile != null) {
            DimacsReader.parseInstance(clauseFile,formulaFactory);
        }
    }

    private void setupErlang() throws IOException {
        node = new OtpNode(nodeName,cookie);
        System.out.println(node);
        mbox = node.createMbox(regName);
        System.out.println(mbox);
    }

    private void initArgs(String[] args) {
        System.out.println(Arrays.toString(args));
        uniqueId = Integer.parseInt(args[0]);
        nodeName = args[1];
        regName = args[2];
        cookie = args[3];
        listeningName = args[4];
        listeningNode = args[5];
        threadCount = Integer.parseInt(args[6]);
        isDistributed = Boolean.parseBoolean(args[7]);
    }
}
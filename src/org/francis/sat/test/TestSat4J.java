package org.francis.sat.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.francis.sat.io.DimacsWriter;
import org.francis.sat.network.message.SatResult;
import org.francis.sat.network.smp.SMPMessageManager;
import org.francis.sat.solver.Clause;
import org.francis.sat.solver.watched.SMPThreadedWatchedSolverFactory;
import org.francis.sat.solver.watched.WatchedFormulaFactory;
import org.francis.sat.solver.watched.WatchedSolverFactory;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.reader.DimacsReader;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.reader.Reader;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

public class TestSat4J {

    static final int MAXVAR = 130;// 100;
    static final int NBCLAUSES = Math.round(((float)MAXVAR)*4.25f);// 425;
    static final int FILE_NUM = 3;
    
    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
//        runOneProblem();
        if (args.length == 4) createTimedFormula(args);
        if (args.length == 1) runCNFDir(args);
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println(totalTime/1000);
    }
    
    public static void runOneProblem() throws IOException {
        WatchedFormulaFactory formulaFactory = new WatchedFormulaFactory();
        generateFormulaeList(50, 400, formulaFactory);
        compareWithSat4J(new WatchedSolverFactory(),formulaFactory,8,2,1024,null,null);
    }
    
    public static void runCNFDir(String[] args) throws IOException {
        int processorCount = Integer.parseInt(args[0]);
        int initHibernate = 2;
        int maxHibernate = 1024;
        if (args.length == 3) {
            initHibernate = Integer.parseInt(args[1]);
            maxHibernate = Integer.parseInt(args[2]);
        }
        String workingPath = System.getProperty("user.dir");
        File cnfDir = new File(workingPath+"/cnf");
        File logDir = new File(workingPath+"/logs");
        File[] formulaDirs = cnfDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            };
        });
        for (File formulaDir : formulaDirs) {
            String[] dirs = formulaDir.getAbsolutePath().split("\\Q"+File.separator+"\\E");
            String timeBasedLogDir = logDir.getAbsolutePath()+"/"+dirs[dirs.length-1];
            ensureDir(timeBasedLogDir);
            File[] formulaFiles = formulaDir.listFiles();
            for (File formulaFile : formulaFiles) {
                String formulaDirName = formulaFile.getName().substring(0,formulaFile.getName().indexOf('.'));
                String formulaLoggingDir = timeBasedLogDir+"/"+formulaDirName;
                ensureDir(formulaLoggingDir);
                int threads = processorCount;
                File runLoggingDir = ensureDir(formulaLoggingDir+"/"+threads);
                WatchedFormulaFactory formulaFactory = new WatchedFormulaFactory();
                org.francis.sat.io.DimacsReader.parseInstance(formulaFile,formulaFactory);
                runMySolversSMPThreaded(new WatchedSolverFactory(),formulaFactory,threads,initHibernate,maxHibernate,runLoggingDir,true,1200000);
//                compareWithSat4J(new WatchedSolverFactory(),new WatchedFormulaFactory(),threads,initHibernate,maxHibernate,formulaFile,null);
            }
        }
    }
    
    /**
     * @param args
     * @throws IOException
     */
    public static void createTimedFormula(String[] args) throws IOException {
        int threads = Integer.parseInt(args[0]);
        int staticVarNum = Integer.parseInt(args[1]);
        int randomVarNum = Integer.parseInt(args[2]);
        int timeout = Integer.parseInt(args[3]); // timeout in minutes
        String workingPath = System.getProperty("user.dir");
        int count = 0;
        WatchedFormulaFactory formulaFactory = null;
        Random rnd = new Random();
        while (count < 300) {
            int varNum = rnd.nextInt(randomVarNum)+staticVarNum;
            int clauseNum = Math.round(((float)varNum)*4.25f);
            formulaFactory = new WatchedFormulaFactory();
            generateFormulaeList(varNum, clauseNum, formulaFactory);
            System.out.println(varNum+", "+clauseNum);
            long runTime = runMySolversSMPThreaded(new WatchedSolverFactory(),formulaFactory,threads,2,1024,null,true,(timeout*60000)+1);
            if (runTime >= timeout*60000) continue;
            long runCatagory = runTime/60000; // Give number of minutes for run
            String path = workingPath+"/cnf/"+runCatagory+"-"+(runCatagory+1)+"_minutes";
            ensureDir(path);
            File newCnfFile = new File(path+"/"+count+".cnf.txt");
            int id = 0;
            while(newCnfFile.exists()) {
                newCnfFile = new File(path+"/"+count+"_"+id+".cnf.txt");
                id++;
            }
            System.out.println(newCnfFile.toString());
            DimacsWriter.writeInstance(newCnfFile, formulaFactory);
            count++;
        }
    }
    
    public static void generateFormulaeList(int varNum, int clauseNum, WatchedFormulaFactory formulaFactory) {
        formulaFactory.init(varNum, clauseNum);
        Random rnd = new Random(System.currentTimeMillis());
        for (int j = 0; j < clauseNum; j++) {
            Set<Integer> clause = new HashSet<Integer>(3);
            while(clause.size() < 3) {
                int d = rnd.nextInt(varNum) + 1;
                d = rnd.nextBoolean() ? d : -d;
                int l = Clause.literalFromDimacs(d);
                clause.add(l);
            }
            formulaFactory.addClause(new ArrayList<Integer>(clause));
        }
    }

    public static void generateFormulaeFile(String cnfDirPath) throws IOException {
        Random rnd = new Random(System.currentTimeMillis());
        File cnfDir = ensureDir(cnfDirPath);
        File[] files = cnfDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".cnf.txt");
            };
        });
        for (File file : files) {
            file.delete();
        }
        for (int i = 0; i < FILE_NUM; i++) {
            File cnfFile = new File(cnfDirPath + "/" + i + ".cnf.txt");
            cnfFile.createNewFile();
            Writer writer = new BufferedWriter(new FileWriter(cnfFile));
            writer.write("p cnf " + MAXVAR + " " + NBCLAUSES + "\n");
            for (int j = 0; j < NBCLAUSES; j++) {
                int a = rnd.nextInt(MAXVAR) + 1;
                a = rnd.nextBoolean() ? a : -a;
                int b = rnd.nextInt(MAXVAR) + 1;
                b = rnd.nextBoolean() ? b : -b;
                int c = rnd.nextInt(MAXVAR) + 1;
                c = rnd.nextBoolean() ? c : -c;
                writer.write(a + " " + b + " " + c + " 0\n");
            }
            writer.close(); // Obviously this is terrible code
        }
    }
    
    private static void deleteFormula(String cnfDirPath) {
        File cnfDir = new File(cnfDirPath);
        if (cnfDir.exists()) {
            File[] files = cnfDir.listFiles();
            for (File file : files) {
                file.delete();
            }
            cnfDir.delete();
        }
    }
    
    private static long runMySolversSMPThreaded(WatchedSolverFactory solverFactory, WatchedFormulaFactory formulaFactory, int threadCount, int initHibernate, int maxHibernate, File logDir, boolean printResult, long timeout) throws IOException {
        long totalTime = 0;
        SMPThreadedWatchedSolverFactory threadedSolverFactory = new SMPThreadedWatchedSolverFactory(solverFactory,formulaFactory);
        long startTime = System.currentTimeMillis();
        SMPMessageManager messageManager = threadedSolverFactory.createAndRunSolversLocal(threadCount, 2, initHibernate, maxHibernate, logDir == null ? null : logDir.getAbsolutePath());
        SatResult satResult = messageManager.receiveResultOrShutDown(timeout);
        String resultMsg = satResult == null ? "Timeout: " : (satResult.result ? "Sat: " : "Unsat: ");
        totalTime += System.currentTimeMillis() - startTime;
        if (logDir != null) {
            File runLog = new File(logDir.getAbsolutePath()+"/run.log");
            runLog.createNewFile();
            BufferedWriter fileWriter = null;
            try {
                runLog.createNewFile();
                fileWriter = new BufferedWriter(new FileWriter(runLog));
                fileWriter.append(resultMsg);
                fileWriter.newLine();
                fileWriter.append(Long.toString(totalTime)+" milliseconds");
            } catch (IOException e) {
                System.out.println(runLog.getAbsolutePath());
                e.printStackTrace();
            }
            finally {
                if (fileWriter != null) fileWriter.close();
            }
        }
        String print = resultMsg;
        print += ((double)totalTime)/1000;
        if (printResult) System.out.println(print);
        return totalTime;
    }
    
    public static File ensureDir(String path) {
//        System.out.println(path);
        String[] dirs = path.split("/");
        String currentPath = "";
        File currentDir = null;
        for (String dirName : dirs) {
            currentPath += "/"+dirName;
            File dir = new File(currentPath);
            if (!dir.exists()) dir.mkdir();
            currentDir = dir;
        }
        return currentDir;
    }
    
    public static void compareWithSat4J(WatchedSolverFactory solverFactory, WatchedFormulaFactory formulaFactory, int threadCount, int initHibernate, int maxHibernate, File cnfFile, String logDirPath) {
        ISolver jsolver = SolverFactory.newDefault();
        jsolver.setTimeout(3600); // 1 hour timeout
        Reader reader = new DimacsReader(jsolver);
        System.out.println(cnfFile);
        try {
            long startTime = System.currentTimeMillis();
            org.francis.sat.io.DimacsReader.parseInstance(cnfFile,formulaFactory);
            SMPThreadedWatchedSolverFactory threadedSolverFactory = new SMPThreadedWatchedSolverFactory(solverFactory,formulaFactory);
            SMPMessageManager messageManager = threadedSolverFactory.createAndRunSolversLocal(threadCount,2, initHibernate, maxHibernate, logDirPath);
            SatResult satResult = messageManager.receiveResult();
            System.out.println("My Solver : " + ((double)System.currentTimeMillis()-startTime)/1000);
            boolean mySolver = satResult.result;
            boolean sat4JSolver;
            try {
                startTime = System.currentTimeMillis();
                IProblem problem = reader.parseInstance(cnfFile.getAbsolutePath());
                sat4JSolver = problem.isSatisfiable();
                System.out.println("Sat4J : " + ((double)System.currentTimeMillis()-startTime)/1000);
            } catch (ContradictionException e) {
                sat4JSolver = false;
            }
            if (mySolver != sat4JSolver) {
                org.francis.sat.io.DimacsReader.parseInstance(cnfFile,formulaFactory);
                SMPThreadedWatchedSolverFactory threadedSolverFactoryDebug = new SMPThreadedWatchedSolverFactory(solverFactory,formulaFactory);
                SMPMessageManager messageManagerDebug = threadedSolverFactoryDebug.createAndRunSolversLocal(threadCount, 2, initHibernate, maxHibernate, logDirPath);
                SatResult satResultDebug = messageManagerDebug.receiveResult();
                boolean mySolverDebug = satResultDebug.result;
                if (sat4JSolver) {
                    System.out.println("False Negative");
                }
                else {
                    System.out.println("False Positive");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            System.out.println("Timeout, sorry!");
        }
    }
    
    public static void runSat4J(String cnfDirPath) {
        long startTime = System.currentTimeMillis();
        File cnfDirectory = new File(cnfDirPath);
        File[] files = cnfDirectory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".cnf.txt");
            };
        });
        int successCount = 0;
        int failureCount = 0;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            ISolver jsolver = SolverFactory.newDefault();
            jsolver.setTimeout(3600); // 1 hour timeout
            Reader reader = new DimacsReader(jsolver);
            try {
                boolean sat4JSolver;
                try {
                    IProblem problem = reader.parseInstance(file.getAbsolutePath());
                    sat4JSolver = problem.isSatisfiable();
                } catch (ContradictionException e) {
//                    System.out.println("Trivially Unsat!");
                    sat4JSolver = false;
                }
                if (sat4JSolver) successCount++;
                else failureCount++;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (ParseFormatException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                System.out.println("Timeout, sorry!");
            }
        }
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Successes: " + successCount);
        System.out.println("Failures: " + failureCount);
        System.out.println("Run Time: " + ((double)totalTime)/1000);
    }

    public static void runSolverRandomly() throws Exception {
        Random rnd = new Random(System.currentTimeMillis());
        ISolver solver = SolverFactory.newDefault();

        // prepare the solver to accept MAXVAR variables. MANDATORY
        solver.newVar(MAXVAR);
        // not mandatory for SAT solving. MANDATORY for MAXSAT solving
        solver.setExpectedNumberOfClauses(NBCLAUSES);
        // Feed the solver using Dimacs format, using arrays of int
        // (best option to avoid dependencies on SAT4J IVecInt)
        for (int i = 0; i < NBCLAUSES; i++) {
            int a = rnd.nextInt(MAXVAR) + 1;
            a = rnd.nextBoolean() ? a : -a;
            int b = rnd.nextInt(MAXVAR) + 1;
            b = rnd.nextBoolean() ? b : -b;
            int c = rnd.nextInt(MAXVAR) + 1;
            c = rnd.nextBoolean() ? c : -c;
            int[] clause = { a, b, c };// get the clause from somewhere
            // the clause should not contain a 0, only integer (positive or
            // negative)
            // with absolute values less or equal to MAXVAR
            // e.g. int [] clause = {1, -3, 7}; is fine
            // while int [] clause = {1, -3, 7, 0}; is not fine
            solver.addClause(new VecInt(clause)); // adapt Array to IVecInt
        }

        // we are done. Working now on the IProblem interface
        IProblem problem = solver;
        if (problem.isSatisfiable()) {
            System.out.println("Satisfiable");
        } else {
            System.out.println("Not Satisfiable");
        }
    }
}

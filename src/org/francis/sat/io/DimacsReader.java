package org.francis.sat.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.francis.sat.solver.FormulaFactory;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.specs.ContradictionException;

public class DimacsReader {

    public static void parseDimacsFile(File cnfFile, FormulaFactory factory) {
        LineNumberReader in = null;
        try {
            in = new LineNumberReader(new InputStreamReader(new FileInputStream(cnfFile)));
            skipComments(in);
            readProblemLine(in,factory);
            readConstrs(in,factory);
        } catch (Exception e) {
            String workingDir = System.getProperty("user.dir");
            System.out.println("Failed to parse "+workingDir+"\\"+cnfFile);
            e.printStackTrace();
        }
        finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    protected static void skipComments(final LineNumberReader in) throws IOException {
        int c;
        do {
            in.mark(4);
            c = in.read();
            if (c == 'c') {
                in.readLine();
            } else {
                in.reset();
            }
        } while (c == 'c');
    }

    protected static void readProblemLine(LineNumberReader in, FormulaFactory factory) throws IOException,
            ParseFormatException {
        String line = in.readLine();
        if (line.charAt(0) != 'p') {
            throw new RuntimeException("No problem line found.");
        }
        String[] problemDef = line.split(" ");
        int varNum = Integer.parseInt(problemDef[2]);
        int clauseNum = Integer.parseInt(problemDef[3]);
        factory.init(varNum, clauseNum);
    }

    protected static void readConstrs(LineNumberReader in, FormulaFactory factory) throws IOException,
            ParseFormatException, ContradictionException {
        String line;
        while (true) {
            line = in.readLine();
            if (line == null)
                break;
            if (line.startsWith("c "))
                continue;
            Scanner scanner = new Scanner(line);
            List<Integer> literals = new ArrayList<Integer>();
            while (scanner.hasNext()) {
                int literal = scanner.nextInt();
                if (literal == 0) break;
                literal = literal < 0 ? (literal*-2)+1 : literal*2;
                literals.add(literal);
            }
            factory.addClause(literals);
//            System.out.println(clause);
        }
    }
}

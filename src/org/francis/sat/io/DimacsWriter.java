package org.francis.sat.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.francis.sat.solver.BooleanFormula;
import org.francis.sat.solver.FormulaFactory;

public class DimacsWriter {

    public static void writeInstance(File dimacsFile, FormulaFactory factory) {
        BufferedWriter dimacsWriter = null;
        try {
            dimacsFile.createNewFile();
            dimacsWriter = new BufferedWriter(new FileWriter(dimacsFile));
            BooleanFormula formula = factory.retrieveFormula();
            dimacsWriter.append("p cnf "+formula.getVarNum()+" "+formula.getClauseNum());
            dimacsWriter.newLine();
            for (List<Integer> clause : formula.getDimacsClauses()) {
                for (int d : clause) {
                    dimacsWriter.append(Integer.toString(d));
                    dimacsWriter.append(" ");
                }
                dimacsWriter.append("0");
                dimacsWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (dimacsWriter != null) dimacsWriter.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

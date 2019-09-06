/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.tripmodif;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CharRemovalModifier implements TaxiDataModifier {
    private final String string;

    public CharRemovalModifier(String string) {
        this.string = string;
    }

    @Override
    public File modify(File taxiData) throws Exception {
        File outFile = new File(taxiData.getAbsolutePath().replace(".csv", "_prepared.csv"));
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(taxiData)); //
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outFile))) {
            System.out.println("INFO start data correction");
            bufferedReader.lines().forEachOrdered(line -> {
                try {
                    bufferedWriter.write(line.replace(string, ""));
                    bufferedWriter.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            System.out.println("INFO successfully stored corrected data in " + outFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outFile;
    }

    public static void main(String[] args) {
        System.out.println("\"");
    }
}

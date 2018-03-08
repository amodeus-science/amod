package amod.demo;

import java.io.File;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.data.ReferenceFrames;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;

enum AnalysisDemo {
    ;

    /** to be executed in simulation directory to perform analysis
     * 
     * @throws Exception */
    public static void main(String[] args) throws Exception {
        File workingDirectory = new File("").getCanonicalFile();
        ScenarioOptions scenOptions = ScenarioOptions.load(workingDirectory);
        File configFile = new File(workingDirectory, scenOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(configFile.toString());
        String outputdirectory = config.controler().getOutputDirectory();
        // Analysis.now(configFile, outputdirectory, null, ReferenceFrames.IDENTITY);

        Analysis analysis = new Analysis( //
                configFile, //
                workingDirectory, //
                outputdirectory, //
                ReferenceFrames.IDENTITY);
        analysis.run();
    }

}
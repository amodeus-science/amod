package amod.demo;

import java.io.File;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import amod.demo.ext.Static;
import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;

enum StandaloneAnalysis {
    ;

    /** to be executed in simulation directory to perform analysis
     * 
     * @throws Exception */
    public static void main(String[] args) throws Exception {
        Static.setup();
        File workingDirectory = new File("").getCanonicalFile();
        ScenarioOptions scenOptions = ScenarioOptions.load(workingDirectory);
        File configFile = new File(workingDirectory, scenOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(configFile.toString());
        String outputdirectory = config.controler().getOutputDirectory();

        Analysis analysis = Analysis.setup(workingDirectory, configFile, new File(outputdirectory));
        analysis.run();
    }

}
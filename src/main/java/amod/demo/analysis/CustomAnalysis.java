/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.analysis;

import java.io.File;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import amod.demo.ext.Static;
import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;

enum CustomAnalysis {
    ;

    /** to be executed in simulation directory to perform analysis
     * 
     * @throws Exception */
    public static void main(String[] args) throws Exception {
        Static.setup();
        File workingDirectory = new File("").getCanonicalFile();
        ScenarioOptions scenOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());
        File configFile = new File(workingDirectory, scenOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(configFile.toString());
        String outputdirectory = config.controler().getOutputDirectory();

        Analysis analysis = Analysis.setup(workingDirectory, configFile, new File(outputdirectory));
        SingleCarElement singleCarElement = new SingleCarElement();
        analysis.addAnalysisElement(singleCarElement);
        SingleCarHtml singleCarHtml = new SingleCarHtml(singleCarElement);
        analysis.addHtmlElement(singleCarHtml);
        analysis.run();
    }

}
/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.analysis;

import java.io.File;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import amod.demo.ext.Static;
import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.analysis.cost.CostFunctionLinearCombination;
import ch.ethz.idsc.amodeus.analysis.cost.RoboTaxiCostParametersImplAmodeus;
import ch.ethz.idsc.amodeus.analysis.report.TotalValueIdentifiersAmodeus;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;

/** This is a demonstration of the functionality of AMoDeus that customized analysis and reporting
 * elements can be easily added. In this example, we present the case in which for every
 * {@link RoboTaxi} the number of served requests should be recorded. */
public enum CustomAnalysis {
    ;

    /** to be executed in simulation directory to perform analysis, i.e., the directory must
     * contain the "output" folder that is compiled during a simulation. In the output folder, there
     * is a list of {@link SimulationObject} which contain the data stored for the simulation.
     * 
     * @throws Exception */
    public static void main(String[] args) throws Exception {
        Static.setup();
        File workingDirectory = new File("").getCanonicalFile();
        ScenarioOptions scenOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());
        File configFile = new File(workingDirectory, scenOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(configFile.toString());
        String outputdirectory = config.controler().getOutputDirectory();

        /** the analysis is created */
        Analysis analysis = Analysis.setup(workingDirectory, configFile, new File(outputdirectory));

        /** here we define and add a custom element to the analysis */
        RoboTaxiRequestRecorder roboTaxiRequestRecorder = new RoboTaxiRequestRecorder();
        analysis.addAnalysisElement(roboTaxiRequestRecorder);
        RoboTaxiRequestRecorderHtml singleCarHtml = new RoboTaxiRequestRecorderHtml(roboTaxiRequestRecorder);
        analysis.addHtmlElement(singleCarHtml);
        analysis.addCostAnalysis(new CostFunctionLinearCombination(TotalValueIdentifiersAmodeus.ANNUALFLEETCOST), new RoboTaxiCostParametersImplAmodeus(1.5));

        /** finally, the analysis is run with the introduced custom element */
        analysis.run();
    }

}
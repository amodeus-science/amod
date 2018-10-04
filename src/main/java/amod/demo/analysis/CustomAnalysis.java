/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.analysis;

import java.io.File;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import amod.demo.ext.Static;
import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.data.LocationSpec;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.matsim.NetworkLoader;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.net.SimulationObject;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;

/** This is a demonstration of the functionality of AMoDeus that customized analysis and reporting
 * elements can be easily added. In this example, we present the case in which for every
 * {@link RoboTaxi} the number of served requests is recorded and then a Histogram image is added
 * to the HTML report */
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
        Network network = NetworkLoader.fromConfigFile(configFile);
        LocationSpec locationSpec = scenOptions.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);

        /** the analysis is created */
        Analysis analysis = Analysis.setup(workingDirectory, configFile, new File(outputdirectory), db);

        /** create and add a custom element */
        addTo(analysis);

        /** finally, the analysis is run with the introduced custom element */
        analysis.run();
    }

    public static void addTo(Analysis analysis) {
        /** first an element to gather the necessary data is defined, it is an implementation of the
         * interface AnalysisElement */
        RoboTaxiRequestRecorder roboTaxiRequestRecorder = new RoboTaxiRequestRecorder();

        /** next an element to export the processed data to an image or other element is defined, it
         * is an implementation of the interface AnalysisExport */
        RoboTaxiRequestHistoGramExport roboTaxiRequestExport = new RoboTaxiRequestHistoGramExport(roboTaxiRequestRecorder);

        /** finally a section for the HTML report is defined, which is an implementation of the
         * interface
         * HtmlReportElement */
        RoboTaxiRequestRecorderHtml roboTaxiRequestRecorderHtml = new RoboTaxiRequestRecorderHtml(roboTaxiRequestRecorder);

        /** all are added to the Analysis before running */
        analysis.addAnalysisElement(roboTaxiRequestRecorder);
        analysis.addAnalysisExport(roboTaxiRequestExport);
        analysis.addHtmlElement(roboTaxiRequestRecorderHtml);

    }

}
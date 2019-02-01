/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import amod.demo.ext.Static;
import ch.ethz.idsc.amodeus.data.LocationSpec;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.gfx.AmodeusComponent;
import ch.ethz.idsc.amodeus.gfx.AmodeusViewerFrame;
import ch.ethz.idsc.amodeus.gfx.ViewerConfig;
import ch.ethz.idsc.amodeus.matsim.NetworkLoader;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.amodeus.virtualnetwork.core.VirtualNetworkGet;

/** the viewer allows to connect to the scenario server or to view saved simulation results. */
public enum ScenarioViewer {
    ;

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        run(workingDirectory);
    }

    /** Execute in simulation folder to view past results or connect to simulation server
     * 
     * @param args not used
     * @throws FileNotFoundException
     * @throws IOException */
    public static void run(File workingDirectory) throws FileNotFoundException, IOException {
        Static.setup();

        /** load options */
        ScenarioOptions simOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());
        Config config = ConfigUtils.loadConfig(simOptions.getSimulationConfigName());
        System.out.println(simOptions.getSimulationConfigName());
        final File outputSubDirectory = new File(config.controler().getOutputDirectory()).getAbsoluteFile();
        if(!outputSubDirectory.isDirectory()){
            System.err.println("output directory: " +  outputSubDirectory.getAbsolutePath() + " not found.");
            GlobalAssert.that(false);
        }
        System.out.println("outputSubDirectory=" + outputSubDirectory);
        System.out.println(outputSubDirectory.getAbsolutePath());
        File outputDirectory = outputSubDirectory.getParentFile();
        System.out.println("showing simulation results from outputDirectory=" + outputDirectory);

        /** geographic information, .e.g., coordinate system */
        LocationSpec locationSpec = simOptions.getLocationSpec();
        ReferenceFrame referenceFrame = locationSpec.referenceFrame();

        /** MATSim simulation network */
        Network network = NetworkLoader.fromConfigFile(new File(workingDirectory, simOptions.getString("simuConfig")));
        System.out.println("INFO network loaded");
        System.out.println("INFO total links " + network.getLinks().size());
        System.out.println("INFO total nodes " + network.getNodes().size());

        /** initializing the viewer */
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, referenceFrame);
        AmodeusComponent amodeusComponent = AmodeusComponent.createDefault(db);

        /** virtual network layer, should not cause problems if layer does not exist */
        amodeusComponent.virtualNetworkLayer.setVirtualNetwork(VirtualNetworkGet.readDefault(network));

        /** starting the viewer */
        ViewerConfig viewerConfig = ViewerConfig.fromDefaults(db);
        System.out.println(viewerConfig);
        AmodeusViewerFrame amodeusViewerFrame = new AmodeusViewerFrame(amodeusComponent, outputDirectory, network);
        amodeusViewerFrame.setDisplayPosition(viewerConfig.settings.coord, viewerConfig.settings.zoom);
        amodeusViewerFrame.jFrame.setSize(viewerConfig.settings.dimensions);
        amodeusViewerFrame.jFrame.setVisible(true);
    }

}

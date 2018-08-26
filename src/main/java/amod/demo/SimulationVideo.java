/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo;

import java.awt.Dimension;
import java.io.File;
import java.util.Objects;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import amod.demo.ext.Static;
import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.gfx.AmodeusComponent;
import ch.ethz.idsc.amodeus.gfx.ClockLayer;
import ch.ethz.idsc.amodeus.gfx.HudLayer;
import ch.ethz.idsc.amodeus.gfx.LoadLayer;
import ch.ethz.idsc.amodeus.gfx.RequestsLayer;
import ch.ethz.idsc.amodeus.gfx.RoboTaxiStatusColors;
import ch.ethz.idsc.amodeus.gfx.TilesLayer;
import ch.ethz.idsc.amodeus.gfx.VehiclesLayer;
import ch.ethz.idsc.amodeus.gfx.VirtualNetworkLayer;
import ch.ethz.idsc.amodeus.gfx.VirtualNodeShader;
import ch.ethz.idsc.amodeus.matsim.NetworkLoader;
import ch.ethz.idsc.amodeus.net.IterationFolder;
import ch.ethz.idsc.amodeus.net.MatsimStaticDatabase;
import ch.ethz.idsc.amodeus.net.SimulationObject;
import ch.ethz.idsc.amodeus.net.StorageSupplier;
import ch.ethz.idsc.amodeus.net.StorageUtils;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.amodeus.video.AmodeusComponentUtil;
import ch.ethz.idsc.amodeus.video.SimulationObjectsVideo;
import ch.ethz.idsc.amodeus.view.gheat.gui.ColorSchemes;
import ch.ethz.idsc.amodeus.view.jmapviewer.tilesources.GrayMapnikTileSource;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetwork;
import ch.ethz.idsc.amodeus.virtualnetwork.VirtualNetworkGet;

/** demo shows how to compile the simulation output to a video animation
 * 
 * run ScenarioServer first to generate the simulation objects */
public enum SimulationVideo {
    ;
    public static void main(String[] args) throws Exception {
        Static.setup();

        File workingDirectory = MultiFileTools.getWorkingDirectory();

        // load options
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());
        Config config = ConfigUtils.loadConfig(scenarioOptions.getSimulationConfigName());
        final File outputSubDirectory = new File(config.controler().getOutputDirectory()).getAbsoluteFile();
        GlobalAssert.that(outputSubDirectory.isDirectory());

        ReferenceFrame referenceFrame = scenarioOptions.getLocationSpec().referenceFrame();
        /** reference frame needs to be set manually in IDSCOptions.properties file */

        Network network = NetworkLoader.fromNetworkFile(new File(workingDirectory, config.network().getInputFile()));

        GlobalAssert.that(Objects.nonNull(network));

        System.out.println("INFO network loaded");
        System.out.println("INFO total links " + network.getLinks().size());
        System.out.println("INFO total nodes " + network.getNodes().size());

        // load viewer
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
        AmodeusComponent amodeusComponent = new AmodeusComponent(MatsimStaticDatabase.INSTANCE);

        amodeusComponent.setTileSource(GrayMapnikTileSource.INSTANCE);

        amodeusComponent.mapGrayCover = 255;
        amodeusComponent.mapAlphaCover = 128;
        amodeusComponent.addLayer(new TilesLayer());

        VehiclesLayer vehiclesLayer = new VehiclesLayer();
        vehiclesLayer.showLocation = true;
        vehiclesLayer.statusColors = RoboTaxiStatusColors.Standard;
        amodeusComponent.addLayer(vehiclesLayer);

        RequestsLayer requestsLayer = new RequestsLayer();
        requestsLayer.drawNumber = false;
        requestsLayer.requestHeatMap.setShow(false);
        requestsLayer.requestHeatMap.setColorSchemes(ColorSchemes.Jet);
        requestsLayer.requestDestMap.setShow(true);
        requestsLayer.requestDestMap.setColorSchemes(ColorSchemes.Sunset);
        amodeusComponent.addLayer(requestsLayer);

        // LinkLayer linkLayer = new LinkLayer();
        // linkLayer.linkLimit = 16384;
        // amodeusComponent.addLayer(linkLayer);

        LoadLayer loadLayer = new LoadLayer();
        loadLayer.drawLoad = true;
        loadLayer.historyLength = 5;
        loadLayer.loadScale = 15;
        amodeusComponent.addLayer(loadLayer);

        amodeusComponent.addLayer(new HudLayer());
        amodeusComponent.setFontSize(0);
        ClockLayer clockLayer = new ClockLayer();
        clockLayer.alpha = 128;
        amodeusComponent.addLayer(clockLayer);

        /** this is optional and should not cause problems if file does not
         * exist. temporary solution */
        VirtualNetworkLayer virtualNetworkLayer = new VirtualNetworkLayer();
        amodeusComponent.addLayer(virtualNetworkLayer);
        VirtualNetwork<Link> virtualNetwork = VirtualNetworkGet.readDefault(network); // may be null
        System.out.println("has vn: " + (virtualNetwork != null));
        amodeusComponent.virtualNetworkLayer.setVirtualNetwork(virtualNetwork);
        amodeusComponent.virtualNetworkLayer.drawVNodes = true;
        amodeusComponent.virtualNetworkLayer.virtualNodeShader = VirtualNodeShader.MaxRequestWaiting;
        amodeusComponent.virtualNetworkLayer.colorSchemes = ColorSchemes.Parula;

        Dimension resolution = SimulationObjectsVideo.RESOLUTION_FullHD;
        amodeusComponent.setSize(resolution);
        AmodeusComponentUtil.adjustMapZoom(amodeusComponent, network, scenarioOptions);
        amodeusComponent.zoomIn();
        // amodeusComponent.zoomIn();
        // amodeusComponent.zoomIn();
        // amodeusComponent.moveMap(-100, -750);

        StorageUtils storageUtils = new StorageUtils(outputSubDirectory);
        IterationFolder iterationFolder = storageUtils.getAvailableIterations().get(0);
        // storageSupplier typically has size = 10800
        StorageSupplier storageSupplier = iterationFolder.storageSupplier();

        int count = 0;
        int base = 1;
        try (SimulationObjectsVideo simulationObjectsVideo = //
                new SimulationObjectsVideo("20180825_berlin_10k.mp4", resolution, 25, amodeusComponent)) {

            simulationObjectsVideo.millis = 20000;

            int intervalEstimate = storageSupplier.getIntervalEstimate(); // 10
            int hrs = 60 * 60 / intervalEstimate;
            final int start = 5 * hrs;
            final int end = Math.min((int) (24.0 * hrs), storageSupplier.size());
            for (int index = start; index < end; index += 1) {
                SimulationObject simulationObject = storageSupplier.getSimulationObject(index);
                simulationObjectsVideo.append(simulationObject);

                if (++count >= base) {
                    System.out.println("render simObj " + count + "/" + (end - start));
                    base *= 2;
                }
            }

        }

        System.exit(0);
    }

}

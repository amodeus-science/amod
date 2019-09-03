/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario;

import java.io.File;
import java.time.LocalDate;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.util.AmodeusTimeConvert;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;

public class ScenarioCreator {
    private final File dataDir;
    private final File taxiData;
    private final DataOperator dataOperator;
    private final File destinDir;
    private final File processingDir;
    private final ScenarioOptions simOptions;
    private final Network network;
    private final String tripId;
    private final LocalDate simulationDate;
    private final AmodeusTimeConvert timeConvert;

    public ScenarioCreator(File dataDir, File taxiData, DataOperator dataOperator, //
            File workingDirectory, ScenarioOptions scenarioOptions, File processingDir, //
            Network network, String tripId, LocalDate simulationDate, //
            AmodeusTimeConvert timeConvert) throws Exception {
        GlobalAssert.that(dataDir.isDirectory());
        GlobalAssert.that(taxiData.exists());
        this.dataDir = dataDir;
        this.taxiData = taxiData;
        this.dataOperator = dataOperator;
        destinDir = new File(workingDirectory, "CreatedScenario");
        this.processingDir = processingDir;
        simOptions = scenarioOptions;
        this.network = network;
        this.tripId = tripId;
        this.simulationDate = simulationDate;
        this.timeConvert = timeConvert;
        run();

    }

    private void run() throws Exception {
        ScenarioAssembler.copyInitialFiles(processingDir, dataDir);
        InitialNetworkPreparer.run(processingDir);
        dataOperator.setFilters();
        dataOperator.fleetConverter.run(processingDir, taxiData, dataOperator, simOptions, network, tripId, //
                simulationDate, timeConvert);
        ScenarioAssembler.copyFinishedScenario(processingDir.getAbsolutePath(), destinDir.getAbsolutePath());
    }
}

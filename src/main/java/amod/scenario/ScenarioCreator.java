/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario;

import java.io.File;
import java.time.LocalDate;

import amod.scenario.fleetconvert.TripFleetConverter;
import ch.ethz.idsc.amodeus.util.AmodeusTimeConvert;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;

public class ScenarioCreator {
    private final File dataDir;
    private final File taxiData;
    private final File destinDir;
    private final File processingDir;
    private final LocalDate simulationDate;
    private final AmodeusTimeConvert timeConvert;
    public final TripFleetConverter fleetConverter;

    public ScenarioCreator(File dataDir, File taxiData, //
            TripFleetConverter converter, //
            File workingDirectory, File processingDir, //
            LocalDate simulationDate, //
            AmodeusTimeConvert timeConvert) throws Exception {
        GlobalAssert.that(dataDir.isDirectory());
        GlobalAssert.that(taxiData.exists());
        this.dataDir = dataDir;
        this.taxiData = taxiData;
        destinDir = new File(workingDirectory, "CreatedScenario");
        this.processingDir = processingDir;
        this.simulationDate = simulationDate;
        this.timeConvert = timeConvert;
        this.fleetConverter = converter;
        run();

    }

    private void run() throws Exception {
        InitialFiles.copyToDir(processingDir, dataDir);
        fleetConverter.setFilters();
        fleetConverter.run(processingDir, taxiData, //
                simulationDate, timeConvert);
        FinishedScenario.copyToDir(processingDir.getAbsolutePath(), destinDir.getAbsolutePath());
    }
}

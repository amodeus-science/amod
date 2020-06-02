/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket;

import java.io.File;

import amodeus.amodeus.options.ScenarioOptions;
import amodeus.amodeus.prep.LegCount;
import amodeus.amodeus.prep.PopulationCutter;
import amodeus.amodeus.prep.TheRequestApocalypse;
import amodeus.amodeus.util.io.GZHandler;
import amodeus.amodeus.util.math.GlobalAssert;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.io.PopulationWriter;

/* package */ enum SocketPopulationPreparer {
    ;

    public static void run(Network network, Population population, ScenarioOptions scenOptions, //
            Config config, long seed, int numReqDes) throws Exception {
        /** ensure population contained in network */
        PopulationCutter populationCutter = scenOptions.getPopulationCutter();
        populationCutter.cut(population, network, config);

        /** apocalypse reduces the number of requests */
        TheRequestApocalypse.reducesThe(population).toNoMoreThan(numReqDes, seed);
        GlobalAssert.that(0 < population.getPersons().size());
        GlobalAssert.that(LegCount.of(population, AmodeusModeConfig.DEFAULT_MODE) == numReqDes);

        final File fileExportGz = new File(scenOptions.getPreparedPopulationName() + ".xml.gz");
        final File fileExport = new File(scenOptions.getPreparedPopulationName() + ".xml");

        /** write the modified population to file */
        PopulationWriter populationWriter = new PopulationWriter(population);
        populationWriter.write(fileExportGz.toString());

        /** extract the created .gz file */
        GZHandler.extract(fileExportGz, fileExport);
    }
}

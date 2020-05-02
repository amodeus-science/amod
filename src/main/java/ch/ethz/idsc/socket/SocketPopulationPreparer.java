/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket;

import java.io.File;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.io.PopulationWriter;

import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.prep.LegCount;
import ch.ethz.idsc.amodeus.prep.PopulationCutter;
import ch.ethz.idsc.amodeus.prep.TheRequestApocalypse;
import ch.ethz.idsc.amodeus.util.io.GZHandler;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;

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
        GlobalAssert.that(LegCount.of(population, "av") == numReqDes);

        final File fileExportGz = new File(scenOptions.getPreparedPopulationName() + ".xml.gz");
        final File fileExport = new File(scenOptions.getPreparedPopulationName() + ".xml");

        /** write the modified population to file */
        PopulationWriter populationWriter = new PopulationWriter(population);
        populationWriter.write(fileExportGz.toString());

        /** extract the created .gz file */
        GZHandler.extract(fileExportGz, fileExport);
    }
}

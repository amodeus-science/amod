/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;
import java.io.IOException;

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
import ch.ethz.idsc.tensor.RationalScalar;

public enum AidoPopulationPreparer {
    ;

    public static void run(Network network, Population population, ScenarioOptions scenOptions, //
            Config config, long seed, int numReqDes) throws Exception {

        /** ensure population contained in network */
        PopulationCutter populationCutter = scenOptions.getPopulationCutter();
        populationCutter.cut(population, network, config);

        /** apocalypse reduces the number of requests */
        TheRequestApocalypse.reducesThe(population).toNoMoreThan(RationalScalar.of(numReqDes, 1), seed);
        GlobalAssert.that(0 < population.getPersons().size());
        GlobalAssert.that(LegCount.of(population, "av").number().intValue() == numReqDes);

        final File fileExportGz = new File(scenOptions.getPreparedPopulationName() + ".xml.gz");
        final File fileExport = new File(scenOptions.getPreparedPopulationName() + ".xml");

        /** write the modified population to file */
        PopulationWriter pw = new PopulationWriter(population);
        pw.write(fileExportGz.toString());

        /** extract the created .gz file */
        try {
            GZHandler.extract(fileExportGz, fileExport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

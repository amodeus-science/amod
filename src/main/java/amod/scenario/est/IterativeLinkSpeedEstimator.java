/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import amod.scenario.chicago.ChicagoGeoInformation;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.linkspeed.create.LinkSpeedsExport;
import ch.ethz.idsc.amodeus.matsim.NetworkLoader;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.taxitrip.ImportTaxiTrips;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;

public class IterativeLinkSpeedEstimator {

    private static final int maxIter = 20;
    private static final Scalar tolerance = RealScalar.of(0.01);
    /** this is a value in (0,1] which determines the convergence
     * speed of the algorithm, a value close to 1 may lead to
     * loss of convergence, it is advised o chose slow values for
     * epsilon. No changes are applied for epsilon == 0. */
    private static final Scalar epsilon = RealScalar.of(0.1);
    private static final Random random = new Random(123);
    private static final int dt = 1800;

    public static void compute(File processingDir, File finalTripsFile) throws IOException {

        // network and database
        ScenarioOptions scenarioOptions = new ScenarioOptions(processingDir, //
                ScenarioOptionsBase.getDefault());
        File configFile = new File(scenarioOptions.getPreparerConfigName());
        System.out.println(configFile.getAbsolutePath());
        GlobalAssert.that(configFile.exists());
        Config configFull = ConfigUtils.loadConfig(configFile.toString());
        Network network = NetworkLoader.fromNetworkFile(new File(processingDir, configFull.network().getInputFile()));
        MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(network, scenarioOptions.getLocationSpec().referenceFrame());

        /** create linkspeeddatacontainer */
        LinkSpeedDataContainer lsData = new LinkSpeedDataContainer();

        /** load initial trips */
        List<TaxiTrip> trips = new ArrayList<>();
        ImportTaxiTrips.fromFile(finalTripsFile).//
                forEach(tt -> trips.add(tt));
        System.out.println("Number of trips: " + trips.size());

        for (int i = 1; i < 5; ++i) {
            System.out.println("step, i = : " + i);
            new LSDataStep(network, db, lsData, trips, maxIter, tolerance, epsilon, random, dt);
            /** intermediate export */
            export(processingDir, lsData);
        }

        /** final export */
        export(processingDir, lsData);

    }

    private static void export(File processingDir, LinkSpeedDataContainer lsData) //
            throws IOException {
        /** exporting final link speeds file */
        File linkSpeedsFile = new File(processingDir, "/linkSpeedData");
        LinkSpeedsExport.using(linkSpeedsFile, lsData);
    }

    public static void main(String[] args) throws IOException {
        ChicagoGeoInformation.setup();
        File processingDir = new File("/home/clruch/data/TaxiComparison_ChicagoScCr/Scenario");
        File finalTripsFile = new File("/home/clruch/data/TaxiComparison_ChicagoScCr/Scenario/"//
                + "tripData/Taxi_Trips_2019_07_19_prepared_filtered_modified_final.csv");
        compute(processingDir, finalTripsFile);
    }

}

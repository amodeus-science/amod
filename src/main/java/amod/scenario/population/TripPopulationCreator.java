/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.population;

import java.io.File;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.utils.collections.QuadTree;

import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.taxitrip.PersonCreate;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.AmodeusTimeConvert;
import ch.ethz.idsc.amodeus.util.CsvReader;
import ch.ethz.idsc.amodeus.util.geo.ClosestLinkSelect;
import ch.ethz.idsc.amodeus.util.io.GZHandler;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

public class TripPopulationCreator {

    private final String fileName = "population.xml";
    private final ClosestLinkSelect linkSelect;
    private final LocalDate simulationDate;
    private final AmodeusTimeConvert timeConvert;
    private final Config config;
    private final Network network;
    private final File populationFile;
    private final File populationFileGz;

    public TripPopulationCreator(File processingDir, Config config, Network network, //
            MatsimAmodeusDatabase db, DateTimeFormatter dateFormat, QuadTree<Link> qt, //
            LocalDate simualtionDate, AmodeusTimeConvert timeConvert) {
        this.linkSelect = new ClosestLinkSelect(db, qt);
        this.simulationDate = simualtionDate;
        this.timeConvert = timeConvert;
        this.config = config;
        this.network = network;
        populationFile = new File(processingDir, fileName);
        populationFileGz = new File(processingDir, fileName + ".gz");
    }

    public void process(File inFile) throws MalformedURLException, Exception {
        System.out.println("INFO start population creation");
        GlobalAssert.that(inFile.isFile());

        // Population init
        Population population = PopulationUtils.createPopulation(config, network);
        PopulationFactory populationFactory = population.getFactory();

        // Population creation (iterate trough all id's)
        System.out.println("Reading inFile:");
        System.out.println(inFile.getAbsolutePath());
        new CsvReader(inFile, ";").rows(row -> {
            try {
                processLine(row, population, populationFactory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Validity Check
        GlobalAssert.that(PopulationHelper.checkAllActivitiesInNetwork(population, network));

        // Write new population to xml

        // write the modified population to file
        PopulationWriter populationWriter = new PopulationWriter(population);
        populationWriter.write(populationFileGz.toString());

        // extract the created .gz file
        GZHandler.extract(populationFileGz, populationFile);

        System.out.println("PopulationSize: " + population.getPersons().size());
        if (population.getPersons().size() > 0)
            System.out.println("INFO successfully created population");
        else
            System.err.println("WARN created population is empty");
    }

    private void processLine(CsvReader.Row line, Population population, //
            PopulationFactory populationFactory) throws Exception {

        // Possible keys: duration,pickupLoc,distance,dropoffDate,taxiId,pickupDate,dropoffLoc,localId,waitTime,

        // create a taxi trip
        Integer globalId = Integer.parseInt(line.get("localId"));
        String taxiId = line.get("taxiId");
        Tensor pickupLoc = Tensors.fromString(line.get("pickupLoc"));
        Tensor dropoffLoc = Tensors.fromString(line.get("dropoffLoc"));
        Scalar distance = Scalars.fromString(line.get("distance"));
        Scalar waitTime = Scalars.fromString(line.get("waitTime"));
        LocalDateTime pickupDate = LocalDateTime.parse(line.get("pickupDate"));
        Scalar duration = Scalars.fromString(line.get("duration"));

        TaxiTrip taxiTrip = TaxiTrip.of(globalId, taxiId, pickupLoc, dropoffLoc, //
                distance, waitTime, //
                pickupDate, duration);

        // Create Person
        Person person = PersonCreate.fromTrip(taxiTrip, globalId, populationFactory, //
                linkSelect, simulationDate, timeConvert);

        population.addPerson(person);
    }

}

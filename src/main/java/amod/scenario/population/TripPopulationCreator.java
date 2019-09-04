/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.population;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.QuadTree;

import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.util.AmodeusTimeConvert;
import ch.ethz.idsc.amodeus.util.CsvReader;
import ch.ethz.idsc.amodeus.util.PersonCreate;
import ch.ethz.idsc.amodeus.util.TaxiTrip;
import ch.ethz.idsc.amodeus.util.geo.ClosestLinkSelect;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

public class TripPopulationCreator extends AbstractPopulationCreator {

    private final ClosestLinkSelect linkSelect;
    private final LocalDate simulationDate;
    private final AmodeusTimeConvert timeConvert;

    public TripPopulationCreator(File processingDir, Config config, Network network, //
            MatsimAmodeusDatabase db, DateTimeFormatter dateFormat, QuadTree<Link> qt, //
            String tripId, LocalDate simualtionDate, AmodeusTimeConvert timeConvert) {
        super(processingDir, config, network, db, dateFormat, tripId);
        this.linkSelect = new ClosestLinkSelect(db, qt);
        this.simulationDate = simualtionDate;
        this.timeConvert = timeConvert;
    }

    @Override
    protected void processLine(CsvReader.Row line, Population population, //
            PopulationFactory populationFactory, String tripId) throws Exception {

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
        
        System.out.println("duration: " +  duration);
        

        TaxiTrip taxiTrip = TaxiTrip.of(globalId, taxiId, pickupLoc, dropoffLoc, //
                distance, waitTime, //
                pickupDate, duration);

        // Create Person
        Person person = PersonCreate.fromTrip(taxiTrip, globalId, populationFactory, //
                linkSelect, simulationDate, timeConvert);

        population.addPerson(person);
    }

}

// TODO remove old solution below
// ++++++++++++++++

// int tripNum = Integer.parseInt(line.get("localId"));
// System.out.println("tripNum: " + tripNum);
// Id<Person> personID = Id.create(tripNum, Person.class);
// Person person = populationFactory.createPerson(personID);
// Plan plan = populationFactory.createPlan();
//
//// Coord to link
// int linkIndexStart = linkSelect.indexFromWGS84(Tensors.fromString(line.get("pickupLoc")));
// int linkIndexEnd = linkSelect.indexFromWGS84(Tensors.fromString(line.get("dropoffLoc")));
// Id<Link> idStart = db.getOsmLink(linkIndexStart).link.getId();
// Id<Link> idEnd = db.getOsmLink(linkIndexEnd).link.getId();
// System.out.println("linkIndexStart: " + linkIndexStart);
// System.out.println("linkIndexEnd: " + linkIndexEnd);
//
//// Start Activity because with have waiting time
// Activity startActivity = populationFactory.createActivityFromLinkId("activity", idStart);
//
//// Start time = PickupTime - WaitingTime
// double waitTime;
// try {
// waitTime = Double.valueOf(line.get(6));
// } catch (Exception e) {
// waitTime = 0.;
// }
//// TODO below was removed as not understandable.
//// startActivity.setEndTime(dateToSeconds(LocalDateTime.parse(reader.get(line, "PickupDate"), dateFormat) dateFormat.parse()) - waitTime);
//
//// Legs
// Leg leg = populationFactory.createLeg("av");
// GlobalAssert.that(startActivity.getEndTime() >= 0);
// leg.setDepartureTime(startActivity.getEndTime() + waitTime);
// GlobalAssert.that(leg.getDepartureTime() >= 0);
//
//// End Activity
// Activity endActivity = populationFactory.createActivityFromLinkId("activity", idEnd);
//// TODO below was removed as not understandable.
//// endActivity.setStartTime(dateToSeconds(dateFormat.parse(reader.get(line, "DropoffDate"))));
//
//// Put together
// plan.addActivity(startActivity);
// plan.addLeg(leg);
// plan.addActivity(endActivity);
// person.addPlan(plan);
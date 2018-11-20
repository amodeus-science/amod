package preparer.population;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree.Rect;

import amod.demo.ext.Static;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.util.io.GZHandler;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.matsim.av.framework.AVConfigGroup;

public class LinkFinder {
    public static void main(String[] args) throws IOException {
        Static.setup();

        File workingDirectory = MultiFileTools.getWorkingDirectory();
        ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

        // Referenceframe needs to be set manually in Amodeus.properties
        // Locationspec needs to be set manually in Amodeus.properties
        GlobalAssert.that(!Objects.isNull(scenarioOptions.getLocationSpec()));

        // load MATSim configs - including av.xml where dispatcher is selected.
        File configFile = new File(workingDirectory, scenarioOptions.getPreparerConfigName());
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);

        // load scenario for simulation
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        double[] networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        Rect outerBoundsRect = new Rect(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);

        /** this is required to not have nodes without links asociated to it. Actually it would be saver t use a network which is already cleaned*/
        NetworkUtils.runNetworkCleaner(network);
        
        Population population = scenario.getPopulation();

        Iterator<? extends Person> itPerson = population.getPersons().values().iterator();
        while (itPerson.hasNext()) {
            Person person = itPerson.next();
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity act = (Activity) planElement;
                        /** if you want to remove a person with activities outside of the network bounds, you can use this condition: */
                        if (!outerBoundsRect.contains(act.getCoord().getX(), act.getCoord().getY())) {
                            System.err.println("There was an activity outside of the Bounding box of the Network. But Lets proceed for now");
                            System.err.println(" Its Coordinates: X: " + act.getCoord().getX() + ", Y: " + act.getCoord().getY());
                        }
                        if (!outerBoundsRect.contains(act.getCoord().getX(), act.getCoord().getY())) {
                            /** This is where the actual magic happens. We find for each activity the closest link and save it to the activity */
                            Link link = NetworkUtils.getNearestLink(network, act.getCoord());
                            Id<Link> linkId = link.getId();
                            act.setLinkId(linkId);
                        }
                    }
                }
            }
        }

        // 8. Export the new generated Population to the defined Name
        final File fileExportGz = new File("population_withLinks" + ".xml.gz");
        final File fileExport = new File("population_withLinks" + ".xml");

        {
            // write the modified population to file
            PopulationWriter pw = new PopulationWriter(population);
            pw.write(fileExportGz.toString());
        }

        // extract the created .gz file
        try {
            GZHandler.extract(fileExportGz, fileExport);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

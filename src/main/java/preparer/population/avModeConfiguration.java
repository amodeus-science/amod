package preparer.population;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import amod.demo.ext.Static;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.util.io.GZHandler;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.matsim.av.framework.AVConfigGroup;

public class avModeConfiguration {
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

        Population population = scenario.getPopulation();

        makeConsistentDepartureAndTravelTimes(population);

        // 8. Export the new generated Population to the defined Name
        final File fileExportGz = new File("population_withLinksAndAV" + ".xml.gz");
        final File fileExport = new File("population_withLinksAndAv" + ".xml");

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

    private static void makeConsistentDepartureAndTravelTimes(Population population) {
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                makeConsistentDepartureAndTravelTimes(plan);
            }
        }
    }

    private static void makeConsistentDepartureAndTravelTimes(Plan plan) {
        // double endTimeActivity = 0;
        Leg prevLeg = null;
        Activity prevActivity = null;
        for (PlanElement pE1 : plan.getPlanElements()) {
            if (pE1 instanceof Activity) {
                Activity act = (Activity) pE1;
                if (prevLeg != null && prevActivity != null) {
                    prevLeg.setTravelTime(act.getStartTime() - prevActivity.getEndTime());
                }
                if (act.getEndTime() == Double.NEGATIVE_INFINITY) {
                    act.setEndTime(act.getStartTime() + act.getMaximumDuration());
                }
                prevActivity = act;
            }
            if (pE1 instanceof Leg) {
                Leg leg = (Leg) pE1;
                leg.setDepartureTime(prevActivity.getEndTime());
                prevLeg = leg;
            }
        }
    }
}

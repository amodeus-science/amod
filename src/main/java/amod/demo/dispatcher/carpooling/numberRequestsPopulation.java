package amod.demo.dispatcher.carpooling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.amodeus.prep.PopulationTools;
import ch.ethz.idsc.amodeus.prep.Request;

enum NumberRequestsPopulation {
    ;
    
    public static double[] getNumberRequests(Network network, int timeStep, Config config) {
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        
        int endTime = (int) config.qsim().getEndTime();
        int timeLength = (endTime/60)/timeStep;
        double[] numberRequests = new double[timeLength];
        Set<Request> avRequests = PopulationTools.getAVRequests(population, network, endTime);
        for (int t = 0; t < timeLength; t++) {
            for (Request avRequest : avRequests) {
                double startTime = avRequest.startTime();
                if (startTime >= (t * timeStep * 60)
                        && startTime < ((t + 1) * timeStep * 60)) {
                    numberRequests[t] = numberRequests[t] + 1;

                }
            }
        }

        return numberRequests;

    }

}

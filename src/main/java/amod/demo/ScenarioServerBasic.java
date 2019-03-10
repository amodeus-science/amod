/* amod - Copyright (c) 2019, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

import amod.demo.analysis.CustomAnalysis;
import amod.demo.dispatcher.DemoDispatcher;
import amod.demo.ext.AmodeusParkingModuleConstantSpots;
import amod.demo.ext.Static;
import amod.demo.generator.DemoGenerator;
import ch.ethz.idsc.amodeus.analysis.Analysis;
import ch.ethz.idsc.amodeus.dispatcher.parking.AmodeusParkingModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVehicleToVSGeneratorModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVirtualNetworkModule;
import ch.ethz.idsc.amodeus.net.SimulationServer;
import ch.ethz.idsc.amodeus.simulation.SimulationProperties;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.matsim.av.framework.AVUtils;

public class ScenarioServerBasic {

    public static void main(String[] args) throws IOException {
        Static.setup();
        Static.checkGLPKLib();

        File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
        SimulationProperties simulationProperties = SimulationProperties.load(workingDirectory);

        simulate(simulationProperties);

        analyze(simulationProperties);
    }

    private static void analyze(SimulationProperties simulationProperties) {

        Analysis analysis = ScenarioServerHelper.setUpAnalysis(simulationProperties);
        /**********************************************************/
        /** Here is the space to add new elements to the analysis */
        /**********************************************************/

        /** Here we add other Analysis Elements, Exports and Total Values to the Analysis */
        CustomAnalysis.addTo(analysis);

        /**********************************************************/
        /**********************************************************/
        /** until here! Now you should be finished because we run the Analysis */
        ScenarioServerHelper.runAnalysis(analysis);
    }

    /** runs a simulation run using input data from Amodeus.properties, av.xml and MATSim config.xml
     * 
     * @throws MalformedURLException
     * @throws Exception */
    private static void simulate(SimulationProperties simulationProperties) {
        ScenarioServerHelper.startServerPort(simulationProperties);

        ScenarioServerHelper.setActivityDurations(simulationProperties, 3600.0);

        Controler controler = ScenarioServerHelper.setUpStandardControler(simulationProperties);

        /**********************************************************/
        /** Here is the space to add new modules to the controler */
        /**********************************************************/
        /** You need to activate this if you want to use a dispatcher that needs a virtual
         * network! */
        controler.addOverridingModule(new AmodeusVirtualNetworkModule());
        controler.addOverridingModule(new AmodeusVehicleToVSGeneratorModule());

        /**********************************************************/
        /** uncomment to include parking. The Capacity has to be stored in the Network
         * You have to choose which strategy you would like to have to solve the problem.
         * Write one of the Options: LP, RANDOMDIFUSION, ADVANCEDDIFUSION, NONE behind the parkingStrategy Tag in the Amodeus Options */
        // For a NetworkBased Capacitiy use the following Line and write NETWORKBASED in the Amodeus Options as the parkingCapacityGenerator.
        // controler.addOverridingModule(new AmodeusParkingModule(simulationProperties.getScenarioOptions()));

        // If you prefer to have a Uniform distibution you can use the following Module or write a similar one yourself.
        Long numberSpotsPerLink = (long) 100;
        controler.addOverridingModule(new AmodeusParkingModuleConstantSpots(simulationProperties.getScenarioOptions(), numberSpotsPerLink));

        /**********************************************************/
        /** uncomment to include custom routers */
        /** controler.addOverridingModule(new AbstractModule() {
         * 
         * @Override
         *           public void install() {
         *           bind(CustomRouter.Factory.class);
         *           AVUtils.bindRouterFactory(binder(),
         *           CustomRouter.class.getSimpleName()).to(CustomRouter.Factory.class);
         * 
         *           }
         *           }); */

        /**********************************************************/
        /** here an additional user-defined dispatcher is added, functionality in class
         * DemoDispatcher */
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AVUtils.registerDispatcherFactory(binder(), "DemoDispatcher", DemoDispatcher.Factory.class);
            }
        });

        /**********************************************************/
        /** here an additional user-defined initial placement logic called generator is added,
         * functionality in class DemoGenerator */
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                AVUtils.registerGeneratorFactory(binder(), "DemoGenerator", DemoGenerator.Factory.class);
            }
        });

        /**********************************************************/
        /**********************************************************/
        /**********************************************************/

        /** run simulation */
        controler.run();

        /** close port for visualizaiton */
        SimulationServer.INSTANCE.stopAccepting();

    }

}

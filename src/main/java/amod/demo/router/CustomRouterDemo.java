/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.router;

import ch.ethz.idsc.amodeus.util.math.GlobalAssert;

/** Nonfunctional code explaining how a custom router would be incluced to an AMoDeus simulation. */
public class CustomRouterDemo {

    public static void main(String[] args) {
        /** In order to include a custom router, you should add this line in ScenarioServer
         * before starting the simualtion:
         * 
         * controler.addOverridingModule(new AbstractModule() {
         * 
         * @Override
         *           public void install() {
         *           bind(CustomRouter.Factory.class);
         *           AVUtils.bindRouterFactory(binder(), CustomRouter.class.getSimpleName()).to(CustomRouter.Factory.class);
         *           }
         *           });
         *
         * 
         *           Then, in av.xml, specify your custom router as follows:
         * 
         *           <?xml version="1.0" encoding="UTF-8"?>
         *           <!DOCTYPE av SYSTEM "https://www.matsim.org/files/dtd/av_v1.dtd">
         * 
         *           <av>
         *           <param name="marginalUtilityOfWaitingTime" value="-12.86" />
         *           <timing>
         *           <param name="pickupDurationPerStop" value="15.0" />
         *           <param name="pickupDurationPerPassenger" value="0.0" />
         *           <param name="dropoffDurationPerStop" value="10.0" />
         *           <param name="dropoffDurationPerPassenger" value="0.0" />
         *           </timing>
         *           <operator id="op1">
         *           <param name="routerName" value="CustomRouter"/>
         *           <generator strategy="PopulationDensity">
         *           <param name="numberOfVehicles" value="5" />
         *           </generator>
         *           <dispatcher strategy="GlobalBipartiteMatchingDispatcher">
         *           <param name="dispatchPeriod" value="30" />
         *           <param name="distanceHeuristics" value="EUCLIDEAN"/>
         * 
         *           </dispatcher>
         *           <pricing>
         *           <param name="pricePerKm" value="0.001" />
         *           <param name="pricePerMin" value="0.0" />
         *           <param name="pricePerTrip" value="3.0" />
         *           <param name="dailySubscriptionFee" value="0.0" />
         *           </pricing>
         *           </operator>
         *           </av>
         * 
         * 
         *           If no router is specified, then the standard router is chosen. Invalid specifications
         *           lead to exceptions. */
        GlobalAssert.that(false);
    }
}
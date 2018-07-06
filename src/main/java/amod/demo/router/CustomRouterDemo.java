package amod.demo.router;

import ch.ethz.idsc.amodeus.util.math.GlobalAssert;

/** Nonfunctional code explaining how a custom router would be incluced to an AMoDeus simulation. */
public class CustomRouterDemo {

    public static void main(String[] args) {
        /** In order to include a custom router, you should add this line in ScenarioServer
         * before starting the simualtion:
         * 
         * controler.addOverridingModule(new AmodeusRouterModule());
         * 
         * Then, in av.xml, specify your custom router as follows:
         * 
         * <av>
         * <param name="marginalUtilityOfWaitingTime" value="-12.86" />
         * <timing>
         * <param name="pickupDurationPerStop" value="15.0" />
         * <param name="pickupDurationPerPassenger" value="0.0" />
         * <param name="dropoffDurationPerStop" value="10.0" />
         * <param name="dropoffDurationPerPassenger" value="0.0" />
         * </timing>
         * <parm name="routerName" value="CustomRouter"/>
         * <operator id="op1">
         * <generator strategy="PopulationDensity">
         * <param name="numberOfVehicles" value="60" />
         * </generator>
         * <dispatcher strategy="SelfishDispatcher">
         * <param name="dispatchPeriod" value="30" />
         * <param name="fareRatio" value="100.0" />
         * </dispatcher>
         * <pricing>
         * <param name="pricePerKm" value="0.001" />
         * <param name="pricePerMin" value="0.0" />
         * <param name="pricePerTrip" value="3.0" />
         * <param name="dailySubscriptionFee" value="0.0" />
         * </pricing>
         * </operator>
         * </av>
         * 
         * 
         * If no router is specified, then the standard router is chosen. Invalid specifications
         * lead to exceptions. */
        GlobalAssert.that(false);
    }
}
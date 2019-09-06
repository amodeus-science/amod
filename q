[1mdiff --git a/src/main/java/amod/scenario/fleetconvert/TripFleetConverter.java b/src/main/java/amod/scenario/fleetconvert/TripFleetConverter.java[m
[1mindex d8ae6fd..963a3b6 100644[m
[1m--- a/src/main/java/amod/scenario/fleetconvert/TripFleetConverter.java[m
[1m+++ b/src/main/java/amod/scenario/fleetconvert/TripFleetConverter.java[m
[36m@@ -4,8 +4,10 @@[m [mpackage amod.scenario.fleetconvert;[m
 import java.io.File;[m
 import java.time.LocalDate;[m
 import java.time.format.DateTimeFormatter;[m
[32m+[m[32mimport java.util.stream.Stream;[m
 [m
 import org.apache.commons.io.FileUtils;[m
[32m+[m[32mimport org.apache.commons.io.FilenameUtils;[m
 import org.matsim.api.core.v01.network.Link;[m
 import org.matsim.api.core.v01.network.Network;[m
 import org.matsim.core.config.Config;[m
[36m@@ -19,6 +21,7 @@[m [mimport amod.scenario.tripmodif.TripBasedModifier;[m
 import ch.ethz.idsc.amodeus.data.ReferenceFrame;[m
 import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;[m
 import ch.ethz.idsc.amodeus.options.ScenarioOptions;[m
[32m+[m[32mimport ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;[m
 import ch.ethz.idsc.amodeus.util.AmodeusTimeConvert;[m
 import ch.ethz.idsc.amodeus.util.math.CreateQuadTree;[m
 import ch.ethz.idsc.amodeus.util.math.GlobalAssert;[m
[1mdiff --git a/src/main/java/amod/scenario/tripfilter/TaxiTripFilter.java b/src/main/java/amod/scenario/tripfilter/TaxiTripFilter.java[m
[1mindex a867594..1b6f0e0 100644[m
[1m--- a/src/main/java/amod/scenario/tripfilter/TaxiTripFilter.java[m
[1m+++ b/src/main/java/amod/scenario/tripfilter/TaxiTripFilter.java[m
[36m@@ -31,6 +31,7 @@[m [mpublic class TaxiTripFilter {[m
 [m
     public final File filter(File file)//[m
             throws IOException {[m
[32m+[m
         GlobalAssert.that(file.exists());[m
         System.out.println("Start to clean " + file.getAbsolutePath() + " data.");[m
         // read the file[m
[36m@@ -56,7 +57,7 @@[m [mpublic class TaxiTripFilter {[m
         return outFile;[m
     }[m
     [m
[31m-    private final Stream<TaxiTrip> filterStream(Stream<TaxiTrip> inStream){[m
[32m+[m[32m    public final Stream<TaxiTrip> filterStream(Stream<TaxiTrip> inStream){[m
         System.out.println("Number of filters: " + filters.size());[m
         for (Predicate<TaxiTrip> dataFilter : filters) {[m
             System.out.println("Applying " + dataFilter.getClass().getSimpleName() + " on data.");[m

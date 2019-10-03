/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario.est;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import ch.ethz.idsc.amodeus.analysis.plot.ColorDataAmodeus;
import ch.ethz.idsc.amodeus.analysis.plot.DiagramSettings;
import ch.ethz.idsc.amodeus.analysis.plot.HistogramPlot;
import ch.ethz.idsc.amodeus.linkspeed.LinkSpeedDataContainer;
import ch.ethz.idsc.amodeus.linkspeed.create.LinkSpeedsExport;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.taxitrip.TaxiTrip;
import ch.ethz.idsc.amodeus.util.io.SaveFormats;
import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.img.ColorDataIndexed;
import ch.ethz.idsc.tensor.pdf.BinCounts;

/* package */ enum StaticHelper {
    ;

    public static int startTime(TaxiTrip trip) {
        return trip.pickupDate.getHour() * 3600//
                + trip.pickupDate.getMinute() * 60//
                + trip.pickupDate.getSecond();

    }

    public static int endTime(TaxiTrip trip) {
        return startTime(trip) + trip.duration.number().intValue();
    }

    public static boolean ratioDidImprove(Scalar ratioBefore, Scalar ratioAfter) {
        Scalar s1 = ratioBefore.subtract(RealScalar.ONE).abs();
        Scalar s2 = ratioAfter.subtract(RealScalar.ONE).abs();
        return Scalars.lessThan(s2, s1);
    }

    public static void export(File processingDir, LinkSpeedDataContainer lsData, String nameAdd) {
        /** exporting final link speeds file */
        File linkSpeedsFile = new File(processingDir, "/linkSpeedData" + nameAdd);
        try {
            LinkSpeedsExport.using(linkSpeedsFile, lsData);
        } catch (IOException e) {
            System.err.println("Export of LinkSpeedDataContainer failed: ");
            e.printStackTrace();
        }
    }

    public static void exportRatioMap(File relativeDirectory, Map<TaxiTrip, Scalar> ratioLookupMap, String append) {
        Tensor all = Tensors.empty();
        ratioLookupMap.values().forEach(s -> all.append(s));
        try {
            SaveFormats.MATHEMATICA.save(all, new File("/home/clruch/Downloads/"), "diff" + append);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void plotRatioMap(File relativeDirectory, Tensor ratios, String append) {
        String fileName = ("histogram_" + append);
        String title = "Differences in Link Speeds";
        String xLabel = "networkspeed / duration";
        String yLabel = "% of requests";
        ColorDataIndexed colorDataIndexed = ColorDataAmodeus.indexed("097");
        Scalar totalRequests = RationalScalar.of(ratios.length(), 1);
        Tensor binValues = BinCounts.of(ratios, RealScalar.of(0.01)).map(s -> s.divide(totalRequests));
        try {
            HistogramPlot.of( //
                    binValues, relativeDirectory, //
                    fileName, title, 0.01, yLabel, //
                    xLabel, DiagramSettings.WIDTH, DiagramSettings.HEIGHT, colorDataIndexed);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        File relativeDirectory = new File("/home/clruch/data/TaxiComparison_ChicagoScCr/Scenario");
        Tensor ratios = Tensors.vector(0.5, 0.3, 0.2, 0.9, 1.0, 1.0, 0.8, 0.7, 0.8, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1);
        plotRatioMap(relativeDirectory, ratios, "jagaharahu");

    }

}

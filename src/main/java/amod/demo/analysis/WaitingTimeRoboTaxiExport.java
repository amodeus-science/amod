package amod.demo.analysis;

import java.io.File;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisExport;
import ch.ethz.idsc.amodeus.analysis.plot.DiagramSettings;
import ch.ethz.idsc.amodeus.analysis.plot.HistogramPlot;
import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.img.ColorDataIndexed;
import ch.ethz.idsc.tensor.pdf.BinCounts;
import ch.ethz.idsc.tensor.red.Total;

public class WaitingTimeRoboTaxiExport implements AnalysisExport {
    public final static String FILENAME = "totalWaitTimePerRoboTaxi";
    private final WaitingTimeRoboTaxiRecorder waitingTimeRoboTaxiRecorder;
    private static final Scalar S2M = RationalScalar.of(1, 60);

    public WaitingTimeRoboTaxiExport(WaitingTimeRoboTaxiRecorder waitingTimeRoboTaxiRecorder) {
        this.waitingTimeRoboTaxiRecorder = waitingTimeRoboTaxiRecorder;
    }

    @Override
    public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorDataIndexed colorScheme) {

        /** the data for the histogram is gathered from the RoboTaxiRequestRecorder, basic
         * information can also be retrieved from the analsysisSummary */
        Tensor totalWaitTimesPerRoboTaxi = waitingTimeRoboTaxiRecorder.getWaitTimePerRoboTaxi();
        Scalar numberOfRoboTaxis = RealScalar.of(totalWaitTimesPerRoboTaxi.length());
        Scalar totalDriveTime = (Scalar) Total.of(totalWaitTimesPerRoboTaxi);
        Scalar histoGrambinSize = Scalars.lessThan(RealScalar.ZERO, totalDriveTime) ? //
        		totalDriveTime.divide(numberOfRoboTaxis.multiply(RealScalar.of(10))) : RealScalar.ONE;

        Tensor histoGramEntryPairs = BinCounts.of(//
        		totalWaitTimesPerRoboTaxi, //
                histoGrambinSize);

        try {
            HistogramPlot.of( //
                    histoGramEntryPairs.divide(numberOfRoboTaxis).multiply(RealScalar.of(100)), //
                    relativeDirectory, //
                    FILENAME, //
                    "Total Wait Times per RoboTaxi", //
                    Math.round(histoGrambinSize.number().doubleValue()), //
                    "% of RoboTaxis", //
                    "Total Wait Times [s]", //
                    DiagramSettings.WIDTH, DiagramSettings.HEIGHT, colorScheme);
        } catch (Exception e) {
            System.err.println("Plot of the Total Wait Times per RoboTaxi Failed");
            e.printStackTrace();
        }

    }

}
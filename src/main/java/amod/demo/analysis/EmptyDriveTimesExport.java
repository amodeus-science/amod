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

public class EmptyDriveTimesExport implements AnalysisExport {
    public final static String FILENAME = "emptyDriveTimePerRoboTaxi";
    private final EmptyDriveTimesElement timeElement;
    private static final Scalar S2M = RationalScalar.of(1, 60);

    public EmptyDriveTimesExport(EmptyDriveTimesElement timeElement) {
        this.timeElement = timeElement;
    }

    @Override
    public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorDataIndexed colorScheme) {

        /** the data for the histogram is gathered from the RoboTaxiRequestRecorder, basic
         * information can also be retrieved from the analsysisSummary */
        Tensor totalDriveTimesPerRoboTaxi = timeElement.getDriveTimePerRoboTaxi().multiply(S2M);
        Scalar numberOfRoboTaxis = RealScalar.of(totalDriveTimesPerRoboTaxi.length());
        Scalar totalDriveTime = (Scalar) Total.of(totalDriveTimesPerRoboTaxi);
        Scalar histoGrambinSize = Scalars.lessThan(RealScalar.ZERO, totalDriveTime) ? //
        		totalDriveTime.divide(numberOfRoboTaxis.multiply(RealScalar.of(10))) : RealScalar.ONE;

        Tensor histoGramEntryPairs = BinCounts.of(//
        		totalDriveTimesPerRoboTaxi, //
                histoGrambinSize);

        try {
            HistogramPlot.of( //
                    histoGramEntryPairs.divide(numberOfRoboTaxis).multiply(RealScalar.of(100)), //
                    relativeDirectory, //
                    FILENAME, //
                    "Empty Drive Times per RoboTaxi", //
                    Math.round(histoGrambinSize.number().doubleValue()), //
                    "% of RoboTaxis", //
                    "Empty Drive Times [min]", //
                    DiagramSettings.WIDTH, DiagramSettings.HEIGHT, colorScheme);
        } catch (Exception e) {
            System.err.println("Plot of the Empty Drive Times per RoboTaxi Failed");
            e.printStackTrace();
        }

    }

}
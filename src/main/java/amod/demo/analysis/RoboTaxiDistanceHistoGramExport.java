package amod.demo.analysis;

import java.io.File;


import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisExport;
import ch.ethz.idsc.amodeus.analysis.plot.DiagramSettings;
import ch.ethz.idsc.amodeus.analysis.plot.HistogramPlot;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;

import ch.ethz.idsc.tensor.img.ColorDataIndexed;
import ch.ethz.idsc.tensor.pdf.BinCounts;
import ch.ethz.idsc.tensor.red.Total;

public class RoboTaxiDistanceHistoGramExport implements AnalysisExport {
    public final static String FILENAME = "distancePerRoboTaxi";
    private final RoboTaxiDistanceRecorder roboTaxiDistanceRecorder;

    public RoboTaxiDistanceHistoGramExport(RoboTaxiDistanceRecorder roboTaxiDistanceRecorder) {
        this.roboTaxiDistanceRecorder = roboTaxiDistanceRecorder;
    }

    @Override
    public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorDataIndexed colorScheme) {

        /** the data for the histogram is gathered from the RoboTaxiRequestRecorder, basic
         * information can also be retrieved from the analsysisSummary */
        Tensor totaldistancePerRoboTaxi = roboTaxiDistanceRecorder.totalDistancesPerVehicle;
        Scalar numberOfRoboTaxis = RealScalar.of(totaldistancePerRoboTaxi.length());
        Scalar totalDistanceDriven = (Scalar) Total.of(totaldistancePerRoboTaxi);
        Scalar histoGrambinSize = Scalars.lessThan(RealScalar.ZERO, totalDistanceDriven) ? //
        		totalDistanceDriven.divide(numberOfRoboTaxis.multiply(RealScalar.of(10))) : RealScalar.ONE;

        Tensor histoGramEntryPairs = BinCounts.of(//
        		totaldistancePerRoboTaxi, //
                histoGrambinSize);

        try {
            HistogramPlot.of( //
                    histoGramEntryPairs.divide(numberOfRoboTaxis).multiply(RealScalar.of(100)), //
                    relativeDirectory, //
                    FILENAME, //
                    "Total Distance per RoboTaxi", //
                    Math.round(histoGrambinSize.number().doubleValue()), //
                    "% of RoboTaxis", //
                    "Distance [m]", //
                    DiagramSettings.WIDTH, DiagramSettings.HEIGHT, colorScheme);
        } catch (Exception e) {
            System.err.println("Plot of the Total Distance per RoboTaxi Failed");
            e.printStackTrace();
        }

    }

}
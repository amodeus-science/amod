/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.analysis;

import java.io.File;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisExport;
import ch.ethz.idsc.amodeus.analysis.plot.DiagramSettings;
import ch.ethz.idsc.amodeus.analysis.plot.HistogramPlot;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.img.ColorDataIndexed;
import ch.ethz.idsc.tensor.pdf.BinCounts;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.matsim.av.passenger.AVRequest;

/** This class generates a png Histogram image of the number of {@link AVRequest} served by each
 * {@link RoboTaxi} */
/* package */ class RoboTaxiRequestHistoGramExport implements AnalysisExport {
    public final static String FILENAME = "requestsPerRoboTaxi";
    private final RoboTaxiRequestRecorder roboTaxiRequestRecorder;

    public RoboTaxiRequestHistoGramExport(RoboTaxiRequestRecorder roboTaxiRequestRecorder) {
        this.roboTaxiRequestRecorder = roboTaxiRequestRecorder;
    }

    @Override
    public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorDataIndexed colorScheme) {

        /** the data for the histogram is gathered from the RoboTaxiRequestRecorder, basic
         * information can also be retrieved from the analsysisSummary */
        Tensor requestsPerRoboTaxi = roboTaxiRequestRecorder.getRequestsPerRoboTaxi();
        Scalar numberOfRoboTaxis = RealScalar.of(requestsPerRoboTaxi.length());
        Scalar totalRequestsServed = (Scalar) Total.of(requestsPerRoboTaxi);
        Scalar histoGrambinSize = Scalars.lessThan(RealScalar.ZERO, totalRequestsServed) ? //
                totalRequestsServed.divide(numberOfRoboTaxis.multiply(RealScalar.of(10))) : RealScalar.ONE;

        Tensor histoGramEntryPairs = BinCounts.of(//
                requestsPerRoboTaxi, //
                histoGrambinSize);

        try {
            HistogramPlot.of( //
                    histoGramEntryPairs.divide(numberOfRoboTaxis).multiply(RealScalar.of(100)), //
                    relativeDirectory, //
                    FILENAME, //
                    "Number of Requests Served per RoboTaxi", //
                    histoGrambinSize.number().doubleValue(), //
                    "% of RoboTaxis", //
                    "Requests", //
                    DiagramSettings.WIDTH, DiagramSettings.HEIGHT, colorScheme);
        } catch (Exception e) {
            System.err.println("Plot of the Number of Requests per RoboTaxi Failed");
            e.printStackTrace();
        }

    }

}

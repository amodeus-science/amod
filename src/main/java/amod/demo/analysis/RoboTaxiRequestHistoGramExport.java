/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.analysis;

import java.io.File;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisExport;
import ch.ethz.idsc.amodeus.dispatcher.core.RoboTaxi;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.subare.plot.Histogram;
import ch.ethz.idsc.subare.plot.VisualRow;
import ch.ethz.idsc.subare.plot.VisualSet;
import ch.ethz.idsc.tensor.*;
import ch.ethz.idsc.tensor.img.ColorDataIndexed;
import ch.ethz.idsc.tensor.pdf.BinCounts;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.matsim.av.passenger.AVRequest;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAnchor;
import org.jfree.chart.axis.CategoryLabelPositions;

/** This class generates a png Histogram image of the number of {@link AVRequest} served by each
 * {@link RoboTaxi} */
/* package */ class RoboTaxiRequestHistoGramExport implements AnalysisExport {
    public final static String FILENAME = "requestsPerRoboTaxi";
    private final RoboTaxiRequestRecorder roboTaxiRequestRecorder;
    public static final int WIDTH = 1000; /* Width of the image */
    public static final int HEIGHT = 750; /* Height of the image */

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
        Scalar histogramBinSize = Scalars.lessThan(RealScalar.ZERO, totalRequestsServed) ? //
                totalRequestsServed.divide(numberOfRoboTaxis.multiply(RealScalar.of(10))) : RealScalar.ONE; // why not round here to integer values?

        Tensor histogramEntryPairs = BinCounts.of(requestsPerRoboTaxi, histogramBinSize);
        histogramEntryPairs = histogramEntryPairs.divide(numberOfRoboTaxis).multiply(RealScalar.of(100));

//        VisualRow visualRow = new VisualRow();
        Tensor points = Tensors.empty();
        for (int i = 0; i < histogramEntryPairs.length(); i++) {
//            visualRow.add(RealScalar.of(i).multiply(histogramBinSize), histogramEntryPairs.Get(i));
            points.append(Tensors.of(RealScalar.of(i).multiply(histogramBinSize), histogramEntryPairs.Get(i)));
        }
        VisualSet visualSet = new VisualSet(colorScheme); // new VisualSet(visualRow);
        VisualRow visualRow = visualSet.add(points);
        visualSet.setPlotLabel("Number of Requests Served per RoboTaxi");
        visualSet.setAxesLabelY("% of RoboTaxis");
        visualSet.setAxesLabelX("Requests");
//        visualSet.setColors(colorScheme);

        final Scalar size = histogramBinSize;
        JFreeChart chart = Histogram.of(visualSet, s -> "[" + s.number() + " , " + s.add(size).number() + ")");
        chart.getCategoryPlot().getDomainAxis().setLowerMargin(0.0);
        chart.getCategoryPlot().getDomainAxis().setUpperMargin(0.0);
        chart.getCategoryPlot().getDomainAxis().setCategoryMargin(0.0);
        chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        chart.getCategoryPlot().setDomainGridlinePosition(CategoryAnchor.START);

        try {
            File fileChart = new File(relativeDirectory, FILENAME + ".png");
            ChartUtilities.saveChartAsPNG(fileChart, chart, WIDTH, HEIGHT);
            GlobalAssert.that(fileChart.isFile());
            System.out.println("Exported " + FILENAME + ".png");
        } catch (Exception e) {
            System.err.println("Plotting " + FILENAME + " failed");
            e.printStackTrace();
        }
    }
}

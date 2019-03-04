/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.analysis;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.report.HtmlBodyElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlReportElement;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.red.Max;
import ch.ethz.idsc.tensor.red.Min;
import ch.ethz.idsc.tensor.red.Total;

/** This class adds a histogram image to the AMoDeus HTML report which was previously
 * compiled in the class {@link RoboTaxiRequestHistoGramExport} */
/* package */ class RoboTaxiRequestRecorderHtml implements HtmlReportElement {
    private static final String IMAGE_FOLDER = "../data"; // relative to report folder
    private static final DecimalFormat DECIMAL = new DecimalFormat("#0.00");
    RoboTaxiRequestRecorder roboTaxiRequestRecorder;

    public RoboTaxiRequestRecorderHtml(RoboTaxiRequestRecorder roboTaxiRequestRecorder) {
        this.roboTaxiRequestRecorder = roboTaxiRequestRecorder;
    }

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {
        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        HtmlBodyElement aRElement = new HtmlBodyElement();
        
        Tensor requestsPerRoboTaxi = roboTaxiRequestRecorder.getRequestsPerRoboTaxi();
        Scalar numberOfRoboTaxis = RealScalar.of(requestsPerRoboTaxi.length());
        Scalar totalRequestsServed = (Scalar) Total.of(requestsPerRoboTaxi);
        double maxNumberRequests = requestsPerRoboTaxi.stream().reduce(Max::of).get().Get().number().doubleValue();
        double minNumberRequests = requestsPerRoboTaxi.stream().reduce(Min::of).get().Get().number().doubleValue();
        double meanNumberRequests = totalRequestsServed.number().doubleValue()/numberOfRoboTaxis.number().doubleValue();

        /** histogram of number of requests per robotaxi */
        aRElement.getHTMLGenerator().insertTextLeft("Maximum Number of Requests of a Vehice:" + //
        		"\n" + "Minimum Number of Requests of a Vehice" + "\n" + "Average Number of Requests per Vehice");
        aRElement.getHTMLGenerator().insertTextLeft(String.valueOf(maxNumberRequests) + //
        		"\n" + String.valueOf(minNumberRequests) + "\n" + String.valueOf(DECIMAL.format(meanNumberRequests)));
        aRElement.getHTMLGenerator().newLine();
        aRElement.getHTMLGenerator().insertTextLeft("This histogram shows the number of reuquest served by each RoboTaxi:");
        aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + RoboTaxiRequestHistoGramExport.FILENAME + ".png", 800, 600);

        /** add together with title of section */
        bodyElements.put("Requests per RoboTaxi Analysis", aRElement);
        return bodyElements;
    }

}

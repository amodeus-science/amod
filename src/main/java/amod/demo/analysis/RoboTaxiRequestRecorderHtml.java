/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.analysis;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.report.HtmlBodyElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlReportElement;

/** This class adds a histogram image to the AMoDeus HTML report which was previously
 * compiled in the class {@link RoboTaxiRequestHistoGramExport} */
/* package */ class RoboTaxiRequestRecorderHtml implements HtmlReportElement {
    private static final String IMAGE_FOLDER = "../data"; // relative to report folder

    RoboTaxiRequestRecorder roboTaxiRequestRecorder;

    public RoboTaxiRequestRecorderHtml(RoboTaxiRequestRecorder roboTaxiRequestRecorder) {
        this.roboTaxiRequestRecorder = roboTaxiRequestRecorder;
    }

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {
        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        HtmlBodyElement aRElement = new HtmlBodyElement();

        /** histogram of number of requests per robotaxi */
        aRElement.getHTMLGenerator().insertTextLeft("This histogram shows the number of reuquest served by each RoboTaxi:");
        aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + RoboTaxiRequestHistoGramExport.FILENAME + ".png", 800, 600);

        /** add together with title of section */
        bodyElements.put("Requests per RoboTaxi Analysis", aRElement);
        return bodyElements;
    }

}

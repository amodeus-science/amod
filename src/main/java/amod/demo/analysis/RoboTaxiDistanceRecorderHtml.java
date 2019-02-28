package amod.demo.analysis;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.report.HtmlBodyElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlReportElement;

public class RoboTaxiDistanceRecorderHtml implements HtmlReportElement {
    private static final String IMAGE_FOLDER = "../data"; // relative to report folder

    RoboTaxiDistanceRecorder roboTaxiDistanceRecorder;

    public RoboTaxiDistanceRecorderHtml(RoboTaxiDistanceRecorder roboTaxiDistanceRecorder) {
        this.roboTaxiDistanceRecorder = roboTaxiDistanceRecorder;
    }

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {
        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        HtmlBodyElement aRElement = new HtmlBodyElement();

        /** histogram of number of requests per robotaxi */
        aRElement.getHTMLGenerator().insertTextLeft("This histogram shows the total distance driven per RoboTaxi:");
        aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + RoboTaxiDistanceHistoGramExport.FILENAME + ".png", 800, 600);

        /** add together with title of section */
        bodyElements.put("Distance per RoboTaxi Analysis", aRElement);
        return bodyElements;
    }

}
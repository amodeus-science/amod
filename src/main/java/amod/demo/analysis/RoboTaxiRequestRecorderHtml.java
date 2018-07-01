/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo.analysis;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.report.BodyElementKeys;
import ch.ethz.idsc.amodeus.analysis.report.HtmlBodyElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlReportElement;

public class RoboTaxiRequestRecorderHtml implements HtmlReportElement {
    private static final String IMAGE_FOLDER = "../data"; // relative to report folder
    private static final String STATUSDISTRIBUTIONFILENAME = "statusDistribution";

    RoboTaxiRequestRecorder sce;

    public RoboTaxiRequestRecorderHtml(RoboTaxiRequestRecorder sce) {
        this.sce = sce;
    }

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {
        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        HtmlBodyElement aRElement = new HtmlBodyElement();
        aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + STATUSDISTRIBUTIONFILENAME + ".png", 800, 600);
        bodyElements.put(BodyElementKeys.FLEETEFFICIENCY, aRElement);
        return bodyElements;
    }

}

package amod.demo.analysis;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.html.HTMLBodyElement;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.report.BodyElementKeys;
import ch.ethz.idsc.amodeus.analysis.report.HtmlReportElement;

public class SingleCarHtml implements HtmlReportElement {
    private static final String IMAGE_FOLDER = "../data"; // relative to report folder
    private static final String STATUSDISTRIBUTIONFILENAME = "statusDistribution";

    SingleCarElement sce;

    public SingleCarHtml(SingleCarElement sce) {
        this.sce = sce;
    }

    @Override
    public Map<String, HTMLBodyElement> process(AnalysisSummary analysisSummary) {
        Map<String, HTMLBodyElement> bodyElements = new HashMap<>();
        HTMLBodyElement aRElement = new HTMLBodyElement();
        aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + STATUSDISTRIBUTIONFILENAME + ".png", 800, 600);
        bodyElements.put(BodyElementKeys.FLEETEFFICIENCY, aRElement);
        return bodyElements;
    }

}

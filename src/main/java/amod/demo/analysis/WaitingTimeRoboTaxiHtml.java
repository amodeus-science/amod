package amod.demo.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.misc.Time;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.report.HtmlBodyElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlReportElement;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.red.Max;
import ch.ethz.idsc.tensor.red.Min;
import ch.ethz.idsc.tensor.red.Total;

public class WaitingTimeRoboTaxiHtml implements HtmlReportElement {
    private static final String IMAGE_FOLDER = "../data"; // relative to report folder

    WaitingTimeRoboTaxiRecorder waitingTimeRoboTaxiRecorder;

    public WaitingTimeRoboTaxiHtml(WaitingTimeRoboTaxiRecorder waitingTimeRoboTaxiRecorder) {
        this.waitingTimeRoboTaxiRecorder = waitingTimeRoboTaxiRecorder;
    }

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {
        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        HtmlBodyElement aRElement = new HtmlBodyElement();
        
        Tensor totalWaitTimesPerRoboTaxi = waitingTimeRoboTaxiRecorder.getWaitTimePerRoboTaxi();
        Scalar numberOfRoboTaxis = RealScalar.of(totalWaitTimesPerRoboTaxi.length());
        Scalar totalWaitTimes = (Scalar) Total.of(totalWaitTimesPerRoboTaxi);
        double maxWaitTime = totalWaitTimesPerRoboTaxi.stream().reduce(Max::of).get().Get().number().doubleValue();
        double minWaitTime = totalWaitTimesPerRoboTaxi.stream().reduce(Min::of).get().Get().number().doubleValue();
        double meanWaitTime = totalWaitTimes.number().doubleValue()/numberOfRoboTaxis.number().doubleValue();

        /** histogram of number of requests per robotaxi */
        aRElement.getHTMLGenerator().insertTextLeft("Maximum Total Wait Time of a Vehice:" + //
        		"\n" + "Minimum Total Wait Time of a Vehice" + "\n" + "Average Total Wait Time per Vehice");
        aRElement.getHTMLGenerator().insertTextLeft(Time.writeTime(maxWaitTime) + //
        		"\n" + Time.writeTime(minWaitTime) + "\n" + Time.writeTime(meanWaitTime));
        aRElement.getHTMLGenerator().newLine();
        aRElement.getHTMLGenerator().insertTextLeft("This histogram shows the total waiting times by each RoboTaxi:");
        aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + WaitingTimeRoboTaxiExport.FILENAME + ".png", 800, 600);

        /** add together with title of section */
        bodyElements.put("Total Drive Times per RoboTaxi Analysis", aRElement);
        return bodyElements;
    }

}

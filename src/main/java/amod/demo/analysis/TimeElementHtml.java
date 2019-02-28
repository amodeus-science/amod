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

public class TimeElementHtml implements HtmlReportElement {
    private static final String IMAGE_FOLDER = "../data"; // relative to report folder

    TimeElement timeElement;

    public TimeElementHtml(TimeElement timeElement) {
        this.timeElement = timeElement;
    }

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {
        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        HtmlBodyElement aRElement = new HtmlBodyElement();
        
        Tensor totalDriveTimesPerRoboTaxi = timeElement.getDriveTimePerRoboTaxi();
        Scalar numberOfRoboTaxis = RealScalar.of(totalDriveTimesPerRoboTaxi.length());
        Scalar totalDriveTimes = (Scalar) Total.of(totalDriveTimesPerRoboTaxi);
        double maxDriveTime = totalDriveTimesPerRoboTaxi.stream().reduce(Max::of).get().Get().number().doubleValue();
        double minDriveTime = totalDriveTimesPerRoboTaxi.stream().reduce(Min::of).get().Get().number().doubleValue();
        double meanDriveTime = totalDriveTimes.number().doubleValue()/numberOfRoboTaxis.number().doubleValue();

        /** histogram of number of requests per robotaxi */
        aRElement.getHTMLGenerator().insertTextLeft("Maximum Total Drive Time of a Vehice:" + //
        		"\n" + "Minimum Total Drive Time of a Vehice" + "\n" + "Average Total Drive Time per Vehice");
        aRElement.getHTMLGenerator().insertTextLeft(Time.writeTime(maxDriveTime) + //
        		"\n" + Time.writeTime(minDriveTime) + "\n" + Time.writeTime(meanDriveTime));
        aRElement.getHTMLGenerator().newLine();
        aRElement.getHTMLGenerator().insertTextLeft("This histogram shows the total distance driven (all drive modes) by each RoboTaxi:");
        aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + TimeHistoGramExport.FILENAME + ".png", 800, 600);

        /** add together with title of section */
        bodyElements.put("Total Drive Times per RoboTaxi Analysis", aRElement);
        return bodyElements;
    }

}

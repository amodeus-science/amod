package amod.demo.analysis;

import java.text.DecimalFormat;
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

public class EmptyDriveTimeHtml implements HtmlReportElement {
    private static final String IMAGE_FOLDER = "../data"; // relative to report folder

    EmptyDriveTimesElement timeElement;
    TimeElement totalTimeElement;
    private static final DecimalFormat DECIMAL = new DecimalFormat("#0.00");

    public EmptyDriveTimeHtml(EmptyDriveTimesElement timeElement, TimeElement totalTimeElement) {
        this.timeElement = timeElement;
        this.totalTimeElement = totalTimeElement;
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
        Tensor totalDriveTimesAllModesPerRoboTaxi = totalTimeElement.getDriveTimePerRoboTaxi();
        Scalar totalDriveTimesAllModes = (Scalar) Total.of(totalDriveTimesAllModesPerRoboTaxi);
        double ratio = totalDriveTimes.number().doubleValue()/totalDriveTimesAllModes.number().doubleValue() * 100;

        /** histogram of number of requests per robotaxi */
        aRElement.getHTMLGenerator().insertTextLeft("Maximum Empty Drive Time of a Vehice:" + //
        		"\n" + "Minimum Empty Drive Time of a Vehice:" + "\n" + "Average Total Drive Time per Vehice:" + "\n" + "Empty Drive Time Ratio:");
        aRElement.getHTMLGenerator().insertTextLeft(Time.writeTime(maxDriveTime) + //
        		"\n" + Time.writeTime(minDriveTime) + "\n" + Time.writeTime(meanDriveTime) + "\n" + DECIMAL.format(ratio) + "%");
        aRElement.getHTMLGenerator().newLine();
        aRElement.getHTMLGenerator().insertTextLeft("This histogram shows the empty drive time (pickup, parking, rebalancing) by each RoboTaxi:");
        aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + EmptyDriveTimesExport.FILENAME + ".png", 800, 600);

        /** add together with title of section */
        bodyElements.put("Total Drive Times per RoboTaxi Analysis", aRElement);
        return bodyElements;
    }

}

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

public class TotalDriveTimeHtml implements HtmlReportElement {
    private static final String IMAGE_FOLDER = "../data"; // relative to report folder
    private final TimeElement timeElement;
	private final EmptyDriveTimesElement emptyTimeElement;
	private final PickupTimeElement pickupTimeElement;
	private final RebalanceTimeElement rebTimeElement;
	private final ParkingTimeElement parkTimeElement;
	private final CustomerDriveTimeElement custTimeElement;

    public TotalDriveTimeHtml(TimeElement timeElement, EmptyDriveTimesElement emptyTimeElement,
			PickupTimeElement pickupTimeElement, RebalanceTimeElement rebTimeElement,
			ParkingTimeElement parkTimeElement, CustomerDriveTimeElement custTimeElement) {
		this.timeElement = timeElement;
		this.emptyTimeElement = emptyTimeElement;
		this.pickupTimeElement = pickupTimeElement;
		this.rebTimeElement = rebTimeElement;
		this.parkTimeElement = parkTimeElement;
		this.custTimeElement = custTimeElement;
    }

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {
        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        HtmlBodyElement aRElement = new HtmlBodyElement();
        
        Tensor totalDriveTimesPerRoboTaxi = pickupTimeElement.getPickupTimePerRoboTaxi()
				.add(rebTimeElement.getRebalanceTimePerRoboTaxi()).add(parkTimeElement.getParkingTimePerRoboTaxi())
				.add(custTimeElement.getCustDriveTimePerRoboTaxi());
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
        aRElement.getHTMLGenerator().insertTextLeft("This histogram shows the total drive time (all drive modes) by each RoboTaxi:");
        aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + TotalDriveTimeExport.FILENAME + ".png", 800, 600);

        /** add together with title of section */
        bodyElements.put("Total Drive Times per RoboTaxi Analysis", aRElement);
        return bodyElements;
    }

}

package amod.demo.analysis;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.misc.Time;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.plot.CompositionStack;
import ch.ethz.idsc.amodeus.analysis.report.HtmlBodyElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlGenerator;
import ch.ethz.idsc.amodeus.analysis.report.HtmlReportElement;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.red.Max;
import ch.ethz.idsc.tensor.red.Min;
import ch.ethz.idsc.tensor.red.Total;

public class DrivetimeAnalysisHtml implements HtmlReportElement {
	private static final String IMAGE_FOLDER = "../data"; // relative to report folder

	private final TimeElement timeElement;
	private final EmptyDriveTimesElement emptyTimeElement;
	private final PickupTimeElement pickupTimeElement;
	private final RebalanceTimeElement rebTimeElement;
	private final ParkingTimeElement parkTimeElement;
	private final CustomerDriveTimeElement custTimeElement;
	private static final DecimalFormat DECIMAL = new DecimalFormat("#0.00");

	public DrivetimeAnalysisHtml(TimeElement timeElement, EmptyDriveTimesElement emptyTimeElement,
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

		Tensor totalDriveTimesPerRoboTaxi = timeElement.getDriveTimePerRoboTaxi();
		Tensor emptyTotalDriveTimesPerRoboTaxi = emptyTimeElement.getDriveTimePerRoboTaxi();
		Tensor pickupTotalDriveTimesPerRoboTaxi = pickupTimeElement.getPickupTimePerRoboTaxi();
		Tensor rebTotalDriveTimesPerRoboTaxi = rebTimeElement.getRebalanceTimePerRoboTaxi();
		Tensor parkTotalDriveTimesPerRoboTaxi = parkTimeElement.getParkingTimePerRoboTaxi();
		Tensor custTotalDirveTimesPerRoboTaxi = custTimeElement.getCustDriveTimePerRoboTaxi();
		Scalar totalDriveTime = (Scalar) Total.of(totalDriveTimesPerRoboTaxi);
		Scalar emptyTotalDriveTime = (Scalar) Total.of(emptyTotalDriveTimesPerRoboTaxi);
		Scalar pickupTotalDriveTime = (Scalar) Total.of(pickupTotalDriveTimesPerRoboTaxi);
		Scalar rebTotalDriveTime = (Scalar) Total.of(rebTotalDriveTimesPerRoboTaxi);
		Scalar parkTotalDriveTime = (Scalar) Total.of(parkTotalDriveTimesPerRoboTaxi);
		Scalar custTotalDriveTime = (Scalar) Total.of(custTotalDirveTimesPerRoboTaxi);
		
		/** histogram of number of requests per robotaxi */
		aRElement.getHTMLGenerator().insertTextLeft(HtmlGenerator.bold("Times") + //
                "\n\tTotal:" + //
                "\n\tRebalancing:" + //
                "\n\tPickup:" + //
                "\n\tParking:" + //
                "\n\tEmpty Time:" + //
                "\n\tWith Customer:");
		aRElement.getHTMLGenerator().insertTextLeft("\n\n" + Time.writeTime(totalDriveTime.number().doubleValue()) + //
                "\n" + Time.writeTime(rebTotalDriveTime.number().doubleValue()) + " (" + //
                DECIMAL.format(100 * rebTotalDriveTime.number().doubleValue() / totalDriveTime.number().doubleValue()) + "%)" + //
                "\n" + Time.writeTime(pickupTotalDriveTime.number().doubleValue()) + " ("+ //
                DECIMAL.format(100 * pickupTotalDriveTime.number().doubleValue() / totalDriveTime.number().doubleValue()) + "%)" + //
                "\n" + Time.writeTime(parkTotalDriveTime.number().doubleValue()) + " ("+ //
                DECIMAL.format(100 * parkTotalDriveTime.number().doubleValue() / totalDriveTime.number().doubleValue()) + "%)" + //
                "\n" + Time.writeTime(emptyTotalDriveTime.number().doubleValue()) + " ("+ //
                DECIMAL.format(100 * emptyTotalDriveTime.number().doubleValue() / totalDriveTime.number().doubleValue()) + "%)" + //
                "\n" + Time.writeTime(custTotalDriveTime.number().doubleValue()) + " ("+ //
                DECIMAL.format(100 * (custTotalDriveTime.number().doubleValue()) / totalDriveTime.number().doubleValue()) + "%)");
		aRElement.getHTMLGenerator().newLine();
		aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + DriveTimeAnalysisExport.FILENAME + ".png", CompositionStack.WIDTH, CompositionStack.HEIGHT);

		/** add together with title of section */
		bodyElements.put("Total Drive Times Analysis", aRElement);
		return bodyElements;
	}

}

package amod.demo.analysis;

import java.io.File;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisExport;
import ch.ethz.idsc.amodeus.analysis.element.DistanceElement;
import ch.ethz.idsc.amodeus.analysis.plot.CompositionStack;
import ch.ethz.idsc.amodeus.analysis.plot.DiagramSettings;
import ch.ethz.idsc.amodeus.analysis.plot.HistogramPlot;
import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.img.ColorDataIndexed;
import ch.ethz.idsc.tensor.pdf.BinCounts;
import ch.ethz.idsc.tensor.red.Total;

public class DriveTimeAnalysisExport implements AnalysisExport {
	public final static String FILENAME = "driveTimeAnalysis";
	private final TimeElement timeElement;
	private final EmptyDriveTimesElement emptyTimeElement;
	private final PickupTimeElement pickupTimeElement;
	private final RebalanceTimeElement rebTimeElement;
	private final ParkingTimeElement parkTimeElement;
	private final CustomerDriveTimeElement custTimeElement;
	private static final Scalar S2M = RationalScalar.of(1, 60);

	public DriveTimeAnalysisExport(TimeElement timeElement, EmptyDriveTimesElement emptyTimeElement, PickupTimeElement pickupTimeElement,
			RebalanceTimeElement rebTimeElement, ParkingTimeElement parkTimeElement, CustomerDriveTimeElement custTimeElement) {
		this.timeElement = timeElement;
		this.emptyTimeElement = emptyTimeElement;
		this.pickupTimeElement = pickupTimeElement;
		this.rebTimeElement = rebTimeElement;
		this.parkTimeElement = parkTimeElement;
		this.custTimeElement = custTimeElement;
	}

	@Override
	public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorDataIndexed colorScheme) {

		/**
		 * the data for the histogram is gathered from the RoboTaxiRequestRecorder,
		 * basic information can also be retrieved from the analsysisSummary
		 */
		Tensor totalDriveTimesPerRoboTaxi = timeElement.getDriveTimePerRoboTaxi().multiply(S2M);
		Tensor emptyTotalDriveTimesPerRoboTaxi = emptyTimeElement.getDriveTimePerRoboTaxi().multiply(S2M);
		Tensor pickupTotalDriveTimesPerRoboTaxi = pickupTimeElement.getPickupTimePerRoboTaxi().multiply(S2M);
		Tensor rebTotalDriveTimesPerRoboTaxi = rebTimeElement.getRebalanceTimePerRoboTaxi().multiply(S2M);
		Tensor parkTotalDriveTimesPerRoboTaxi = parkTimeElement.getParkingTimePerRoboTaxi().multiply(S2M);
		Tensor custTotalDriveTimesPerRoboTaxi = custTimeElement.getCustDriveTimePerRoboTaxi().multiply(S2M);
		Scalar totalDriveTime = (Scalar) Total.of(totalDriveTimesPerRoboTaxi);
		Scalar emptyTotalDriveTime = (Scalar) Total.of(emptyTotalDriveTimesPerRoboTaxi);
		Scalar pickupTotalDriveTime = (Scalar) Total.of(pickupTotalDriveTimesPerRoboTaxi);
		Scalar rebTotalDriveTime = (Scalar) Total.of(rebTotalDriveTimesPerRoboTaxi);
		Scalar parkTotalDriveTime = (Scalar) Total.of(parkTotalDriveTimesPerRoboTaxi);
		Scalar custTotalDriveTime = (Scalar) Total.of(custTotalDriveTimesPerRoboTaxi);

		String[] labels = { "With Customer", "Pickup", "Rebalancing", "Parking" };
		double[] values = new double[] { //
				(custTotalDriveTime.number().doubleValue()) / totalDriveTime.number().doubleValue(), //
				pickupTotalDriveTime.number().doubleValue() / totalDriveTime.number().doubleValue(), //
				rebTotalDriveTime.number().doubleValue() / totalDriveTime.number().doubleValue(), //
				parkTotalDriveTime.number().doubleValue() / totalDriveTime.number().doubleValue() };

		try {
			CompositionStack.of( //
					relativeDirectory, //
					FILENAME, //
					"Total Drive Time Distribution", //
					values, //
					labels, //
					colorScheme);
		} catch (Exception e) {
			System.err.println("The Stacked Distance Plot was not successfull");
			e.printStackTrace();
		}

	}

}
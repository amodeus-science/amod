/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.idsc.amodeus.aido.AidoScoreElement;
import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.BinnedWaitingTimesImage;
import ch.ethz.idsc.amodeus.analysis.RequestsPerWaitingTimeImage;
import ch.ethz.idsc.amodeus.analysis.element.DistanceElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlBodyElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlReportElement;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/* package */ class AidoHtmlReport implements HtmlReportElement {

    private static final String IMAGE_FOLDER = "../data"; // relative to report folder
    private Scalar totalMeanWaitingTime;
    private Scalar totalEfficiencyRatio; /* empty distance divided by total distance */
    private Scalar numberOfVehicles;
    private final AidoScoreElement aidoScoreElement;

    public AidoHtmlReport(AidoScoreElement aidoScoreElement) {
        this.aidoScoreElement = aidoScoreElement;
    }

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {

        this.totalMeanWaitingTime = analysisSummary.getWaitingTimes().totalWaitTimeMean;

        {
            DistanceElement distanceElement = analysisSummary.getDistanceElement();
            double sum = distanceElement.totalDistancePicku + distanceElement.totalDistanceRebal;
            // if totalDistance == 0.0, the ratio is NaN
            this.totalEfficiencyRatio = RealScalar.of(sum / distanceElement.totalDistance);
        }

        this.numberOfVehicles = RealScalar.of(analysisSummary.getSimulationInformationElement().vehicleSize());

        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        {
            HtmlBodyElement aRElement = new HtmlBodyElement();

            aRElement.getHTMLGenerator().insertTextLeft(aRElement.getHTMLGenerator().bold("Scores during Simulation"));
            aRElement.getHTMLGenerator().newLine();
            aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + AidoExport.FILENAME_WAIT + ".png", 800, 600);
            aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + AidoExport.FILENAME_DIST + ".png", 800, 600);

            aRElement.getHTMLGenerator()
                    .insertTextLeft(aRElement.getHTMLGenerator().bold("Final Scores") + //
                            "\n\t" + "total waiting time:" + //
                            "\n\t" + "total full distance:" + //
                            "\n\t" + "total empty distance:" //
            );
            aRElement.getHTMLGenerator()
                    .insertTextLeft(" " + //
                            "\n" + aidoScoreElement.getCurrentScore().Get(0) + //
                            "\n" + aidoScoreElement.getCurrentScore().Get(1) + //
                            "\n" + aidoScoreElement.getCurrentScore().Get(2) //
            );
            aRElement.getHTMLGenerator().newLine();

            bodyElements.put("Aido Scores", aRElement);
        }
        return bodyElements;

    }

    /** @return */
    public Tensor getFinalScore() {
        return Tensors.of(totalMeanWaitingTime, totalEfficiencyRatio, numberOfVehicles);
    }

}

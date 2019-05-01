/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.util.HashMap;
import java.util.Map;

import amod.aido.core.AidoScoreElement;
import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.element.DistanceElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlBodyElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlGenerator;
import ch.ethz.idsc.amodeus.analysis.report.HtmlReportElement;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.red.Total;

/* package */ class AidoHtmlReport implements HtmlReportElement {

    /** relative to report folder */
    private static final String IMAGE_FOLDER = "../data";
    // ---
    private final AidoScoreElement aidoScoreElement;
    private Scalar totalMeanWaitingTime;
    private Scalar totalEfficiencyRatio; /* empty distance divided by total distance */
    private Scalar numberOfVehicles;

    public AidoHtmlReport(AidoScoreElement aidoScoreElement) {
        this.aidoScoreElement = aidoScoreElement;
    }

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {

        totalMeanWaitingTime = analysisSummary.getTravelTimeAnalysis().getWaitAggrgte().Get(1);

        {
            DistanceElement distanceElement = analysisSummary.getDistanceElement();
            double sum = distanceElement.totalDistancePicku + distanceElement.totalDistanceRebal;
            // if totalDistance == 0.0, the ratio is NaN
            totalEfficiencyRatio = RealScalar.of(sum / distanceElement.totalDistance);
        }

        numberOfVehicles = RealScalar.of(analysisSummary.getSimulationInformationElement().vehicleSize());

        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        {
            HtmlBodyElement aRElement = new HtmlBodyElement();

            aRElement.getHTMLGenerator().insertTextLeft(HtmlGenerator.bold("Scores during Simulation"));
            aRElement.getHTMLGenerator().newLine();
            aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + AidoExport.FILENAME_SCORE_INCR + ".png", 800, 600);
            aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + AidoExport.FILENAME_SCORE_INTG + ".png", 800, 600);
            aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + AidoExport.FILENAME_SCORE3_INTG + ".png", 800, 600);

            aRElement.getHTMLGenerator().insertTextLeft(HtmlGenerator.bold("Final Scores") + //
                    "\n\t" + "final service quality score:" + //
                    "\n\t" + "final efficiency score:" + //
                    "\n\t" + "final fleet size score:" //
            );
            aRElement.getHTMLGenerator().insertTextLeft(" " + //
                    "\n" + Total.of(Transpose.of(aidoScoreElement.getScoreDiffHistory()).get(1)) + //
                    "\n" + Total.of(Transpose.of(aidoScoreElement.getScoreDiffHistory()).get(2)) + //
                    "\n" + Total.of(Transpose.of(aidoScoreElement.getScoreDiffHistory()).get(3)) //
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

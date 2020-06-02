/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.socket;

import java.util.HashMap;
import java.util.Map;

import amodeus.amodeus.analysis.AnalysisSummary;
import amodeus.amodeus.analysis.element.DistanceElement;
import amodeus.amodeus.analysis.report.HtmlBodyElement;
import amodeus.amodeus.analysis.report.HtmlGenerator;
import amodeus.amodeus.analysis.report.HtmlReportElement;
import ch.ethz.idsc.socket.core.SocketScoreElement;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.red.Total;

/* package */ class SocketHtmlReport implements HtmlReportElement {

    /** relative to report folder */
    private static final String IMAGE_FOLDER = "../data";
    // ---
    private final SocketScoreElement socketScoreElement;
    private Scalar totalMeanWaitingTime;
    private Scalar totalEfficiencyRatio; /* empty distance divided by total distance */
    private Scalar numberOfVehicles;

    public SocketHtmlReport(SocketScoreElement socketScoreElement) {
        this.socketScoreElement = socketScoreElement;
    }

    @Override
    public Map<String, HtmlBodyElement> process(AnalysisSummary analysisSummary) {

        totalMeanWaitingTime = analysisSummary.getTravelTimeAnalysis().getWaitAggrgte().Get(1);

        {
            DistanceElement distanceElement = analysisSummary.getDistanceElement();
            Scalar sum = distanceElement.totalDistancePicku.add(distanceElement.totalDistanceRebal);
            // if totalDistance == 0.0, the ratio is NaN
            totalEfficiencyRatio = sum.divide(distanceElement.totalDistance);
        }

        numberOfVehicles = RealScalar.of(analysisSummary.getSimulationInformationElement().vehicleSize());

        Map<String, HtmlBodyElement> bodyElements = new HashMap<>();
        {
            HtmlBodyElement aRElement = new HtmlBodyElement();

            aRElement.getHTMLGenerator().insertTextLeft(HtmlGenerator.bold("Scores during Simulation"));
            aRElement.getHTMLGenerator().newLine();
            aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + SocketExport.FILENAME_SCORE_INCR, 800, 600);
            aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + SocketExport.FILENAME_SCORE_INTG, 800, 600);
            aRElement.getHTMLGenerator().insertImg(IMAGE_FOLDER + "/" + SocketExport.FILENAME_SCORE3_INTG, 800, 600);

            aRElement.getHTMLGenerator()
                    .insertTextLeft(HtmlGenerator.bold("Final Scores") + //
                            "\n\t" + "final service quality score:" + //
                            "\n\t" + "final efficiency score:" + //
                            "\n\t" + "final fleet size score:" //
            );
            aRElement.getHTMLGenerator()
                    .insertTextLeft(" " + //
                            "\n" + Total.of(Transpose.of(socketScoreElement.getScoreDiffHistory()).get(1)) + //
                            "\n" + Total.of(Transpose.of(socketScoreElement.getScoreDiffHistory()).get(2)) + //
                            "\n" + Total.of(Transpose.of(socketScoreElement.getScoreDiffHistory()).get(3)) //
            );
            aRElement.getHTMLGenerator().newLine();

            bodyElements.put("Socket Scores", aRElement);
        }
        return bodyElements;

    }

    /** @return */
    public Tensor getFinalScore() {
        return Tensors.of(totalMeanWaitingTime, totalEfficiencyRatio, numberOfVehicles);
    }
}

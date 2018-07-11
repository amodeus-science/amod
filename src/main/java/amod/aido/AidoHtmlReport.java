/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.element.DistanceElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlBodyElement;
import ch.ethz.idsc.amodeus.analysis.report.HtmlReportElement;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/* package */ class AidoHtmlReport implements HtmlReportElement {

    private Scalar totalMeanWaitingTime;
    private Scalar totalEfficiencyRatio; /* empty distance divided by total distance */
    private Scalar numberOfVehicles;

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
            aRElement.getHTMLGenerator().insertTextLeft(aRElement.getHTMLGenerator().bold("Individual Scores") + //
                    "\n\t" + "mean waiting time:" + //
                    "\n\t" + "empty distance / total distance:" + //
                    "\n\t" + "number of RoboTaxis:" //
            );
            aRElement.getHTMLGenerator().insertTextLeft(" " + //
                    "\n" + totalMeanWaitingTime + //
                    "\n" + totalEfficiencyRatio + //
                    "\n" + numberOfVehicles //
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

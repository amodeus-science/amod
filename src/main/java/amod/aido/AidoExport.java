package amod.aido;

import java.io.File;

import ch.ethz.idsc.amodeus.aido.AidoScoreElement;
import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.UnitSaveUtils;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisExport;
import ch.ethz.idsc.amodeus.analysis.plot.ColorScheme;
import ch.ethz.idsc.amodeus.analysis.plot.TimeChart;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Transpose;

/* package */ class AidoExport implements AnalysisExport {

    /** settings for plot */
    /* package */ static final String FILENAME_WAIT = "aidoWaitingScores";
    /* package */ static final String FILENAME_DIST = "aidoDistnceScores";
    private static final int FILTERSIZE = 50;
    private static final boolean FILTER_ON = true;

    /** aido score element */
    private final AidoScoreElement aidoScoreElem;

    public AidoExport(AidoScoreElement aidoScoreElem) {
        this.aidoScoreElem = aidoScoreElem;
    }

    @Override
    public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorScheme colorScheme) {

        /** produce chart that shows all 3 Aido scores during simulation */
        Tensor time = Transpose.of(aidoScoreElem.getScoreHistory()).get(0);

        Tensor waiting = Transpose.of(Tensors.of(Transpose.of(aidoScoreElem.getScoreHistory()).get(1)));

        Tensor distances = Transpose.of(Tensors.of(//
                Transpose.of(aidoScoreElem.getScoreHistory()).get(2), //
                Transpose.of(aidoScoreElem.getScoreHistory()).get(3)));

        /** figure for waiting times */
        try {
            TimeChart.of(relativeDirectory, FILENAME_WAIT, "Total Waiting Time", FILTER_ON, FILTERSIZE, //
                    new double[] { 1.0 }, new String[] { "total waited time [s]" }, "time of day", "waiting time", //
                    time, waiting, aidoScoreElem.getCurrentScore().Get(0).number().doubleValue(), colorScheme);
        } catch (Exception e1) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e1.printStackTrace();
        }

        /** figure for distances */
        try {
            TimeChart.of(relativeDirectory, FILENAME_DIST, "Total Distances", FILTER_ON, FILTERSIZE, //
                    new double[] { 1.0, 1.0 }, new String[] { "total distance with customer [m]", "total empty distance [m]" }, "time of day", "distance", //
                    time, distances, //
                    Math.max(aidoScoreElem.getCurrentScore().Get(1).number().doubleValue(), aidoScoreElem.getCurrentScore().Get(2).number().doubleValue()),
                    colorScheme); // TODO read from data
        } catch (Exception e1) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e1.printStackTrace();
        }

        /** export aggregated aido scores data */
        try {
            UnitSaveUtils.saveFile(aidoScoreElem.getScoreHistory(), "aidoScores", relativeDirectory);
        } catch (Exception e) {
            System.err.println("Saving aido score history was unsuccessful.");
            e.printStackTrace();
        }

    }
}

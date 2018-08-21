/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
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
    private final AidoScoreElement aidoScoreElement;

    public AidoExport(AidoScoreElement aidoScoreElement) {
        this.aidoScoreElement = aidoScoreElement;
    }

    @Override
    public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorScheme colorScheme) {

        Tensor scoreHistory = aidoScoreElement.getScoreHistory();
        /** produce chart that shows all 3 Aido scores during simulation */
        Tensor time = scoreHistory.get(Tensor.ALL, 0);

        Tensor waiting = Transpose.of(Tensors.of(scoreHistory.get(Tensor.ALL, 1)));

        // TODO can use the following?
        // Tensor distances = Tensor.of(scoreHistory.stream().map(row->row.extract(2, 4)));
        Tensor distances = Transpose.of(Tensors.of( //
                scoreHistory.get(Tensor.ALL, 2), //
                scoreHistory.get(Tensor.ALL, 3)));

        /** figure for waiting times */
        try {
            TimeChart.of(relativeDirectory, FILENAME_WAIT, "Total Waiting Time", FILTER_ON, FILTERSIZE, //
                    new double[] { 1.0 }, new String[] { "total waited time [s]" }, "time of day", "waiting time", //
                    time, waiting, aidoScoreElement.getCurrentScore().Get(0).number().doubleValue(), colorScheme);
        } catch (Exception e1) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e1.printStackTrace();
        }

        /** figure for distances */
        try {
            TimeChart.of(relativeDirectory, FILENAME_DIST, "Total Distances", FILTER_ON, FILTERSIZE, //
                    new double[] { 1.0, 1.0 }, new String[] { "total distance with customer [m]", "total empty distance [m]" }, "time of day", "distance", //
                    time, distances, //
                    Math.max(aidoScoreElement.getCurrentScore().Get(1).number().doubleValue(), aidoScoreElement.getCurrentScore().Get(2).number().doubleValue()), colorScheme); // TODO
                                                                                                                                                                                // read
                                                                                                                                                                                // from
                                                                                                                                                                                // data
        } catch (Exception e1) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e1.printStackTrace();
        }

        /** export aggregated aido scores data */
        try {
            UnitSaveUtils.saveFile(aidoScoreElement.getScoreHistory(), "aidoScores", relativeDirectory);
        } catch (Exception e) {
            System.err.println("Saving aido score history was unsuccessful.");
            e.printStackTrace();
        }

    }
}

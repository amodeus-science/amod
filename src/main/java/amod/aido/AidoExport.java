/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;

import amod.aido.core.AidoScoreElement;
import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.UnitSaveUtils;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisExport;
import ch.ethz.idsc.amodeus.analysis.plot.TimeChart;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.img.ColorDataIndexed;

/* package */ class AidoExport implements AnalysisExport {

    /** settings for plot */
    /* package */ static final String FILENAME_SCORE_INCR = "aidoScores1and2Diff";
    /* package */ static final String FILENAME_SCORE_INTG = "aidoScores1and2Intg";
    /* package */ static final String FILENAME_SCORE3_INTG = "aidoScore3Intg";
    private static final int FILTERSIZE = 50;
    private static final boolean FILTER_ON = true;

    /** aido score element */
    private final AidoScoreElement aidoScoreElement;

    public AidoExport(AidoScoreElement aidoScoreElement) {
        this.aidoScoreElement = aidoScoreElement;
    }

    @Override
    public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorDataIndexed colorScheme) {

        Tensor scoreDiffHistory = aidoScoreElement.getScoreDiffHistory();
        Tensor scoreIntgHistory = aidoScoreElement.getScoreIntgHistory();

        /** produce charts that show 2 Aido scores during simulation (integrated and increment) */
        Tensor time = scoreDiffHistory.get(Tensor.ALL, 0);
        Tensor linCombScoresDiff = Tensor.of(scoreDiffHistory.stream().map(row -> row.extract(1, 3)));
        Tensor linCombScoresIntg = Tensor.of(scoreIntgHistory.stream().map(row -> row.extract(1, 3)));

        /** figures for service quality score and efficiency score */
        try {
            TimeChart.of(relativeDirectory, FILENAME_SCORE_INCR, "Service Quality and Efficiency Score Increments", //
                    FILTER_ON, FILTERSIZE, new double[] { 1.0, 1.0 }, //
                    new String[] { "service quality performance [1]", "efficiency performance [1]" }, //
                    "time of day", "scores increments", //
                    time, linCombScoresDiff, //
                    null, colorScheme);
        } catch (Exception e1) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e1.printStackTrace();
        }

        try {
            TimeChart.of(relativeDirectory, FILENAME_SCORE_INTG, "Service Quality and Efficiency Score Integrated", //
                    FILTER_ON, FILTERSIZE, new double[] { 1.0, 1.0 }, //
                    new String[] { "service quality performance [1]", "efficiency performance [1]" }, //
                    "time of day", "scores integrated", //
                    time, linCombScoresIntg, //
                    null, colorScheme);
        } catch (Exception e1) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e1.printStackTrace();
        }

        /** figures for fleet size score */
        Tensor fleetSizeScoreIntg = Tensor.of(scoreIntgHistory.stream().map(row -> row.extract(3, 4)));

        try {
            TimeChart.of(relativeDirectory, FILENAME_SCORE3_INTG, "Fleet Size Score Integrated", //
                    FILTER_ON, FILTERSIZE, new double[] { 1.0 }, //
                    new String[] { "fleet size score integrated" }, //
                    "time of day", "scores integrated", //
                    time, fleetSizeScoreIntg, //
                    new Double[] { fleetSizeScoreIntg.get(0).Get(0).number().intValue() * 2.0, 0.0 }, colorScheme);

        } catch (Exception e1) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e1.printStackTrace();
        }

        /** export incremental and integrated aido scores data */
        try {
            UnitSaveUtils.saveFile(aidoScoreElement.getScoreDiffHistory(), "aidoScoresIncr", relativeDirectory);
            UnitSaveUtils.saveFile(aidoScoreElement.getScoreIntgHistory(), "aidoScoresIntg", relativeDirectory);
        } catch (Exception e) {
            System.err.println("Saving aido score history was unsuccessful.");
            e.printStackTrace();
        }
    }
}

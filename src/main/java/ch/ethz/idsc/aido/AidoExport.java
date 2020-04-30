/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.aido;

import java.io.File;

import org.jfree.chart.JFreeChart;

import ch.ethz.idsc.aido.core.AidoScoreElement;
import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.UnitSaveUtils;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisExport;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisMeanFilter;
import ch.ethz.idsc.amodeus.analysis.plot.AmodeusChartUtils;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Unprotect;
import ch.ethz.idsc.tensor.fig.TimedChart;
import ch.ethz.idsc.tensor.fig.VisualRow;
import ch.ethz.idsc.tensor.fig.VisualSet;
import ch.ethz.idsc.tensor.img.ColorDataIndexed;

/* package */ class AidoExport implements AnalysisExport {

    /** settings for plot */
    /* package */ static final String FILENAME_SCORE_INCR = "aidoScores1and2Diff.png";
    /* package */ static final String FILENAME_SCORE_INTG = "aidoScores1and2Intg.png";
    /* package */ static final String FILENAME_SCORE3_INTG = "aidoScore3Intg.png";
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 750;

    /** aido score element */
    private final AidoScoreElement aidoScoreElement;

    public AidoExport(AidoScoreElement aidoScoreElement) {
        this.aidoScoreElement = aidoScoreElement;
    }

    @Override
    public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorDataIndexed colorScheme) {

        Tensor scoreDiffHistory = aidoScoreElement.getScoreDiffHistory();
        Tensor scoreIntgHistory = aidoScoreElement.getScoreIntgHistory();

        /** produce charts that show 2 Aido scores during simulation (integrated and
         * increment) */
        Tensor time = scoreDiffHistory.get(Tensor.ALL, 0);
        Tensor linCombScoresDiff = Tensor.of(scoreDiffHistory.stream().map(row -> row.extract(1, 3)));
        Tensor linCombScoresIntg = Tensor.of(scoreIntgHistory.stream().map(row -> row.extract(1, 3)));

        /** figures for service quality score and efficiency score */
        try {
            JFreeChart chart = timeChart("Service Quality and Efficiency Score Increments", //
                    new String[] { "service quality performance [1]", "efficiency performance [1]" }, //
                    "time of day", "scores increments", time, linCombScoresDiff, colorScheme);
            File fileChart = new File(relativeDirectory, FILENAME_SCORE_INCR);
            AmodeusChartUtils.saveAsPNG(chart, fileChart.toString(), WIDTH, HEIGHT);
            GlobalAssert.that(fileChart.isFile());
        } catch (Exception e1) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e1.printStackTrace();
        }

        try {
            JFreeChart chart = timeChart("Service Quality and Efficiency Score Integrated", //
                    new String[] { "service quality performance [1]", "efficiency performance [1]" }, //
                    "time of day", "scores integrated", time, linCombScoresIntg, colorScheme);
            File fileChart = new File(relativeDirectory, FILENAME_SCORE_INTG);
            AmodeusChartUtils.saveAsPNG(chart, fileChart.toString(), WIDTH, HEIGHT);
            GlobalAssert.that(fileChart.isFile());
        } catch (Exception e1) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e1.printStackTrace();
        }

        /** figures for fleet size score */
        Tensor fleetSizeScoreIntg = Tensor.of(scoreIntgHistory.stream().map(row -> row.extract(3, 4)));

        try {
            JFreeChart chart = timeChart("Fleet Size Score Integrated", //
                    new String[] { "fleet size score integrated" }, //
                    "time of day", "scores integrated", time, fleetSizeScoreIntg, colorScheme);
            File fileChart = new File(relativeDirectory, FILENAME_SCORE_INTG);
            chart.getXYPlot().getRangeAxis().setRange(fleetSizeScoreIntg.get(0).Get(0).number().intValue() * 2.0, 0.0);
            AmodeusChartUtils.saveAsPNG(chart, fileChart.toString(), WIDTH, HEIGHT);
            GlobalAssert.that(fileChart.isFile());
        } catch (Exception e1) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e1.printStackTrace();
        }

        /** export incremental and integrated aido scores data */
        try {
            UnitSaveUtils.saveFile(aidoScoreElement.getScoreDiffHistory(), "aidoScoresIncr", relativeDirectory);
            UnitSaveUtils.saveFile(aidoScoreElement.getScoreIntgHistory(), "aidoScoresIntg", relativeDirectory);
        } catch (Exception exception) {
            System.err.println("Saving aido score history was unsuccessful.");
            exception.printStackTrace();
        }
    }

    private JFreeChart timeChart(String title, String[] labels, String xAxisLabel, String yAxisLabel, Tensor time, Tensor values, ColorDataIndexed colorDataIndexed) {
        GlobalAssert.that(Unprotect.dimension1(values) == labels.length);

        VisualSet visualSet = new VisualSet(colorDataIndexed);
        for (int i = 0; i < labels.length; ++i) {
            Tensor vector = values.get(Tensor.ALL, i);
            vector = AnalysisMeanFilter.of(vector);
            VisualRow visualRow = visualSet.add(time, vector);
            visualRow.setLabel(labels[i]);
        }

        visualSet.setPlotLabel(title);
        visualSet.setAxesLabelX(xAxisLabel);
        visualSet.setAxesLabelY(yAxisLabel);

        return TimedChart.of(visualSet);
    }
}

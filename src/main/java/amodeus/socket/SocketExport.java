/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amodeus.socket;

import java.io.File;

import org.jfree.chart.JFreeChart;

import amodeus.amodeus.analysis.AnalysisSummary;
import amodeus.amodeus.analysis.UnitSaveUtils;
import amodeus.amodeus.analysis.element.AnalysisExport;
import amodeus.amodeus.analysis.element.AnalysisMeanFilter;
import amodeus.amodeus.analysis.plot.AmodeusChartUtils;
import amodeus.amodeus.util.math.GlobalAssert;
import amodeus.socket.core.SocketScoreElement;
import amodeus.tensor.fig.TimedChart;
import amodeus.tensor.fig.VisualRow;
import amodeus.tensor.fig.VisualSet;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Unprotect;
import ch.ethz.idsc.tensor.img.ColorDataIndexed;

/* package */ class SocketExport implements AnalysisExport {

    /** settings for plot */
    /* package */ static final String FILENAME_SCORE_INCR = "socketScores1and2Diff.png";
    /* package */ static final String FILENAME_SCORE_INTG = "socketScores1and2Intg.png";
    /* package */ static final String FILENAME_SCORE3_INTG = "socketScore3Intg.png";
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 750;

    /** socket score element */
    private final SocketScoreElement socketScoreElement;

    public SocketExport(SocketScoreElement socketScoreElement) {
        this.socketScoreElement = socketScoreElement;
    }

    @Override
    public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorDataIndexed colorScheme) {

        Tensor scoreDiffHistory = socketScoreElement.getScoreDiffHistory();
        Tensor scoreIntgHistory = socketScoreElement.getScoreIntgHistory();

        /** produce charts that show 2 scores during simulation (integrated and
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
            System.err.println("Plotting the scores was unsuccessful.");
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
            System.err.println("Plotting the scores was unsuccessful.");
            e1.printStackTrace();
        }

        /** figures for fleet size score */
        Tensor fleetSizeScoreIntg = Tensor.of(scoreIntgHistory.stream().map(row -> row.extract(3, 4)));

        try {
            JFreeChart chart = timeChart("Fleet Size Score Integrated", //
                    new String[] { "fleet size score integrated" }, //
                    "time of day", "scores integrated", time, fleetSizeScoreIntg, colorScheme);
            File fileChart = new File(relativeDirectory, FILENAME_SCORE_INTG);
            chart.getXYPlot().getRangeAxis().setRange(Unprotect.withoutUnit(fleetSizeScoreIntg.Get(0,0)).number().intValue() * 2.0, 0.0);
            AmodeusChartUtils.saveAsPNG(chart, fileChart.toString(), WIDTH, HEIGHT);
            GlobalAssert.that(fileChart.isFile());
        } catch (Exception e1) {
            System.err.println("Plotting the scores was unsuccessful.");
            e1.printStackTrace();
        }

        /** export incremental and integrated scores data */
        try {
            UnitSaveUtils.saveFile(socketScoreElement.getScoreDiffHistory(), "socketScoresIncr", relativeDirectory);
            UnitSaveUtils.saveFile(socketScoreElement.getScoreIntgHistory(), "socketScoresIntg", relativeDirectory);
        } catch (Exception exception) {
            System.err.println("Saving score history was unsuccessful.");
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

/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;
import java.util.Objects;

import amod.aido.core.AidoScoreElement;
import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.UnitSaveUtils;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisExport;
import ch.ethz.idsc.amodeus.util.math.GlobalAssert;
import ch.ethz.idsc.subare.plot.TimedChart;
import ch.ethz.idsc.subare.plot.VisualRow;
import ch.ethz.idsc.subare.plot.VisualSet;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.img.ColorDataIndexed;
import ch.ethz.idsc.tensor.img.MeanFilter;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

/* package */ class AidoExport implements AnalysisExport {

    /** settings for plot */
    /* package */ static final String FILENAME_SCORE_INCR = "aidoScores1and2Diff";
    /* package */ static final String FILENAME_SCORE_INTG = "aidoScores1and2Intg";
    /* package */ static final String FILENAME_SCORE3_INTG = "aidoScore3Intg";
    private static final int FILTERSIZE = 50;
    private static final boolean FILTER_ON = true;
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

        /** produce charts that show 2 Aido scores during simulation (integrated and increment) */
        Tensor time = scoreDiffHistory.get(Tensor.ALL, 0);
        Tensor linCombScoresDiff = Tensor.of(scoreDiffHistory.stream().map(row -> row.extract(1, 3)));
        Tensor linCombScoresIntg = Tensor.of(scoreIntgHistory.stream().map(row -> row.extract(1, 3)));

        /** figures for service quality score and efficiency score */
        try {
            generatePlot(relativeDirectory, FILENAME_SCORE_INCR, time, linCombScoresDiff, null, //
                    new String[] { "service quality performance [1]", "efficiency performance [1]" }, //
                    "Service Quality and Efficiency Score Increments", colorScheme);
        } catch (Exception e) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e.printStackTrace();
        }

        try {
            generatePlot(relativeDirectory, FILENAME_SCORE_INTG, time, linCombScoresIntg, null, //
                    new String[] { "service quality performance [1]", "efficiency performance [1]" }, //
                    "Service Quality and Efficiency Score Integrated", colorScheme);
        } catch (Exception e) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e.printStackTrace();
        }

        /** figures for fleet size score */
        Tensor fleetSizeScoreIntg = Tensor.of(scoreIntgHistory.stream().map(row -> row.extract(3, 4)));

        try {
            generatePlot(relativeDirectory, FILENAME_SCORE3_INTG, time, fleetSizeScoreIntg, //
                    new Double[]{fleetSizeScoreIntg.get(0).Get(0).number().intValue() * 2., 0.}, //
                    new String[]{"fleet size score integrated"}, //
                    "Fleet Size Score Integrated", colorScheme);
        } catch (Exception e) {
            System.err.println("Plotting the aido scores was unsuccessful.");
            e.printStackTrace();
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

    private void generatePlot(File relativeDirectory, String fileName, Tensor time, Tensor vals, Double[] limits, //
                             String[] labels, String title, ColorDataIndexed colorDataIndexed) throws Exception {
        VisualSet visualSet = new VisualSet(colorDataIndexed);
        for (int i = 0; i < labels.length; i++) {
            Tensor values = Transpose.of(vals).get(i);
            values = FILTER_ON ? MeanFilter.of(values, FILTERSIZE) : values;
            VisualRow visualRow = visualSet.add(time, values);
            visualRow.setLabel(labels[i]);
        }

        visualSet.setPlotLabel(title);
        visualSet.setAxesLabelX("time of day");
        visualSet.setAxesLabelY("scores integrated");

        JFreeChart chart = TimedChart.of(visualSet);
        if (Objects.nonNull(limits))
            GlobalAssert.that(limits[0] < limits[1]);
            chart.getXYPlot().getRangeAxis().setRange(limits[0], limits[1]);

        File fileChart = new File(relativeDirectory, fileName + ".png");
        ChartUtilities.saveChartAsPNG(fileChart, chart, WIDTH, HEIGHT);
    }
}

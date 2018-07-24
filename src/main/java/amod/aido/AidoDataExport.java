package amod.aido;

import java.io.File;

import ch.ethz.idsc.amodeus.analysis.AnalysisSummary;
import ch.ethz.idsc.amodeus.analysis.UnitSaveUtils;
import ch.ethz.idsc.amodeus.analysis.element.AnalysisExport;
import ch.ethz.idsc.amodeus.analysis.plot.ColorScheme;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.qty.Quantity;

public class AidoDataExport implements AnalysisExport {

    @Override
    public void summaryTarget(AnalysisSummary analysisSummary, File relativeDirectory, ColorScheme colorScheme) {
        Tensor allWaitingTimes = Tensors.empty();
        analysisSummary.getWaitingTimes().requestWaitTimes.values().stream().map(RealScalar::of).forEach(s -> {
            allWaitingTimes.append(Quantity.of(s, "s"));
        });
        try {
            UnitSaveUtils.saveFile(allWaitingTimes, "AllWaitingTimes", relativeDirectory);
        } catch (Exception e) {
            System.err.println("Not able to  save all waiting times, check file AidoDataExport.");
            e.printStackTrace();
        }
    }
}

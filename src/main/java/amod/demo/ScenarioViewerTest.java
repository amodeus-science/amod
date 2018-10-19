package amod.demo;

import ch.ethz.idsc.amodeus.net.TensorCoords;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.ResourceData;
import ch.ethz.idsc.tensor.io.TensorProperties;
import org.matsim.api.core.v01.Coord;

import java.io.File;
import java.io.IOException;
import java.util.Properties;


// only for testing purpose, later tobe part of ScenarioViewer
public enum ScenarioViewerTest {
    ;

    public static void main(String[] args) throws IOException {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        run(workingDirectory);
    }

    public static void run(File workingDirectory) throws IOException {
        File config = new File(workingDirectory, "viewer.properties");
        ViewerConfig viewerConfig = new ViewerConfig();
        if (config.exists()) {
            TensorProperties.wrap(viewerConfig).load(config);
        } else {
            Properties properties = ResourceData.properties("/gui/viewer_default.properties");
            TensorProperties.wrap(viewerConfig).set(properties);
        }

        // Coord coord = TensorCoords.toCoord(Tensors.vector(1, 2));
        // System.out.println(coord);

        // can this be done directly or only with a tensor as above? (see tensor.io.StaticHelper)
        System.out.println("coord = " + viewerConfig.coord);
        System.out.println("zoom = " + viewerConfig.zoom.number().intValue());
        System.out.println("width = " + viewerConfig.width);
        System.out.println("height = " + viewerConfig.height);
        // what is the correct format of boolean in *.properties?
        System.out.println("bool = " + viewerConfig.visibility);
    }

}

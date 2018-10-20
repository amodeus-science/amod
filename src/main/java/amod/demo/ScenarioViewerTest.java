/* amod - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.demo;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.idsc.tensor.io.Import;
import ch.ethz.idsc.tensor.io.ResourceData;
import ch.ethz.idsc.tensor.io.TensorProperties;

// only for testing purpose, later tobe part of ScenarioViewer
public enum ScenarioViewerTest {
    ;

    public static void main(String[] args) throws IOException {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        run(workingDirectory);
    }

    public static void run(File workingDirectory) throws IOException {
        // TODO no need to create a default values properties file
        // ... as default values are declared in ViewerConfig class
        Properties properties = ResourceData.properties("/gui/viewer_default.properties");
        File config = new File(workingDirectory, "viewer.properties");
        if (config.exists())
            properties = Import.properties(config);
        // ---
        ViewerConfig viewerConfig = TensorProperties.wrap(new ViewerConfig()).set(properties);
        // ---
        // Coord coord = TensorCoords.toCoord(Tensors.vector(1, 2));
        // System.out.println(coord);

        // can this be done directly or only with a tensor as above? (see tensor.io.StaticHelper)
        System.out.println("coord = " + viewerConfig.coord);
        System.out.println("zoom = " + viewerConfig.zoom.number().intValue());
        System.out.println("width = " + viewerConfig.width);
        System.out.println("height = " + viewerConfig.height);
        // what is the correct format of boolean in *.properties?
        System.out.println("bool = " + viewerConfig.visibility);
        System.out.println(viewerConfig.getDimension());
    }

}

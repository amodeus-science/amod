/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.aido;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import ch.ethz.idsc.amodeus.util.math.GlobalAssert;

public class StaticHelper {

    public static void changeDispatcherTo(String newDispatcher, File simFolder) //
            throws JDOMException, IOException {
        System.out.println("changing fare ratio to " + newDispatcher);

        File xmlFile = new File(simFolder, "av.xml");

        System.out.println("looking for av.xml file at " + xmlFile.getAbsolutePath());

        GlobalAssert.that(xmlFile.exists());

        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        Document doc = builder.build(xmlFile);
        Element rootNode = doc.getRootElement();
        Element operator = rootNode.getChild("operator");
        Element dispatcher = operator.getChild("dispatcher");

        Attribute strategy = dispatcher.getAttribute("strategy");
        if (strategy.getValue() != newDispatcher)
            strategy.setValue(newDispatcher);

        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(doc, new FileWriter(xmlFile));

    }

    public static void changeVehicleNumberTo(int vehicleNumber, File simFolder) //
            throws JDOMException, IOException {
        System.out.println("changing vehicle number to " + vehicleNumber);

        File xmlFile = new File(simFolder, "av.xml");

        System.out.println("looking for av.xml file at " + xmlFile.getAbsolutePath());

        GlobalAssert.that(xmlFile.exists());
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        Document doc = builder.build(xmlFile);
        Element rootNode = doc.getRootElement();
        Element operator = rootNode.getChild("operator");
        Element dispatcher = operator.getChild("generator");
        @SuppressWarnings("unchecked")
        List<Element> children = dispatcher.getChildren();

        for (Element element : children) {
            @SuppressWarnings("unchecked")
            List<Attribute> theAttributes = element.getAttributes();

            if (theAttributes.get(0).getValue().equals("numberOfVehicles")) {
                theAttributes.get(1).setValue(Integer.toString(vehicleNumber));

            }

        }

        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(doc, new FileWriter(xmlFile));

    }

}

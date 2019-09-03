/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package amod.scenario;

import java.io.File;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;

import ch.ethz.idsc.amodeus.matsim.xml.XmlCustomModifier;

public class Pt2MatsimXML {

    public static void toLocalFileSystem(File xmlFile, String systemSpecificPath) throws Exception {

        System.out.println("xml file " + xmlFile.getAbsolutePath());

        try (XmlCustomModifier xmlModifier = new XmlCustomModifier(xmlFile)) {
            Document doc = xmlModifier.getDocument();
            Element rootNode = doc.getRootElement();
            Element module = rootNode.getChild("module");

            List<Element> children = module.getChildren();

            for (Element element : children) {
                List<Attribute> theAttributes = element.getAttributes();

                if (theAttributes.get(0).getValue().equals("osmFile")) {
                    String old = theAttributes.get(1).getValue();
                    theAttributes.get(1).setValue(systemSpecificPath + "/" + old);
                    System.out.println(theAttributes.get(1).getValue());
                }

                if (theAttributes.get(0).getValue().equals("outputNetworkFile")) {
                    String old = theAttributes.get(1).getValue();
                    theAttributes.get(1).setValue(systemSpecificPath + "/" + old);
                }
            }
        }
    }
}

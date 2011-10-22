package org.wdssii.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author lakshman
 * 
 */
public class XMLUtil {

    public static boolean getBooleanAttribute(Element vcpel, String name,
            boolean defaultValue) {
        if (vcpel.hasAttribute(name)) {
            String value = vcpel.getAttribute(name);
            return (value.equals("true") || value.equals("yes") || value.equals("1"));
        }
        return defaultValue;
    }

    public static int getIntegerAttribute(Element vcpel, String name,
            int defaultValue) {
        if (vcpel.hasAttribute(name)) {
            String value = vcpel.getAttribute(name);
            return Integer.parseInt(value);
        }
        return defaultValue;
    }

    public static void writeXmlFile(Document doc, String filename, boolean compress) throws IOException {
        OutputStream os = null;
        try {
            // in
            Source source = new DOMSource(doc);

            // out
            if (compress) {
                if (!filename.endsWith(".gz")) {
                    filename = filename + ".gz";
                }
                os = new GZIPOutputStream(new FileOutputStream(filename));
            } else {
                os = new FileOutputStream(filename);
            }
            Result result = new StreamResult(os);

            // write
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(source, result);

        } catch (TransformerException e) {
            throw new IllegalStateException("Couldn't find TRaX API to write out XML");
        } catch (TransformerFactoryConfigurationError e) {
            throw new IllegalStateException("Couldn't find TRaX API to write out XML");
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    /**
     * Returns the text content of a deeply embedded child element.
     * <pre>
     *    <parent><child>returned text</child><parent>
     * </pre>
     * @param parent
     * @param childName
     * @return null if there is no such child; zero-length string if the child element is empty
     */
    public static String getChildText(Element parent, String childName) {
        NodeList nodes = parent.getElementsByTagName(childName);
        if (nodes.getLength() >= 1) {
            Node child = nodes.item(0);
            if (child.hasChildNodes()) {
                return child.getFirstChild().getNodeValue();
            }
            return "";
        }
        return null; // no such child
    }
}

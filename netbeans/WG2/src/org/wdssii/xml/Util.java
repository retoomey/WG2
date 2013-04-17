package org.wdssii.xml;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.W2Config;

/**
 * XML Utilities for loading with JAXB We do two things. One..we lowercase all
 * tags because some of our old legacy files were hand types. Two. We allow
 * integers to be hex values by having 0x in front of them. We have to tell JAXB
 * about these changes.
 *
 * @author Robert Toomey
 */
public class Util {

    private final static Logger LOG = LoggerFactory.getLogger(Util.class);

    /**
     * A Stream reader delegate that will convert all xml tags to lower case
     * <thisWorks>data<ThiSworks> is treated as <thisworks>data<thisworks>
     *
     * This means your annotations for all tags should be in lower case
     * XmlAttribute(name="alllowercase") <-- the actual field, tag, etc.
     *
     */
    private static class lowerStreamReaderDelegate extends StreamReaderDelegate {

        public lowerStreamReaderDelegate(XMLStreamReader xsr) {
            super(xsr);
        }

        @Override
        public String getAttributeLocalName(int index) {
            return super.getAttributeLocalName(index).toLowerCase();
        }

        @Override
        public String getLocalName() {
            return super.getLocalName().toLowerCase();
        }
    }

    private static class CustomValidationEventHandler implements ValidationEventHandler {

        @Override
        public boolean handleEvent(ValidationEvent evt) {
            //System.out.println("Event Info: " + evt);
            if (evt.getMessage().contains("Unexpected element")) {

                // Just a tag we don't handle..don't want us dying
                // JAXB you're WAY TOO SENSITIVE
                LOG.debug("XML Error " + evt);
                return true; // Keep going....
            }
            LOG.debug("XML Error " + evt);
            return true;
            //return false; // All others stop?
        }
    }
    /*
     * We allow integers to be read of the form 0x where it's hex...
     * 0xFF --> 255 integer
     * Not sure we can override the default JAXB integer conversion..
     * What we want is this..
     * XmlJavaTypeAdapter(IntegerHexAdapter.class)
     * public Integer test  (xml = "255" or "0xFF"
     */

    public static class IntegerHexAdapter extends XmlAdapter<String, Integer> {

        /**
         * Java ==> XML Just write out normal integer....hex is typed by humans
         */
        @Override
        public String marshal(Integer value) throws Exception {
            return value.toString();
        }

        /**
         * XML --> Java. Check the attribute string for the '0x'
         */
        @Override
        public Integer unmarshal(String value) throws Exception {
            Integer anInt;
            if (value.toLowerCase().startsWith("0x")) {
                value = value.substring(2);
                anInt = Integer.parseInt(value, 16);
            } else {
                anInt = Integer.parseInt(value);
            }
            return anInt;
        }
    }

    public static class FloatAdapter extends XmlAdapter<String, Float> {

        /**
         * Java ==> XML Just write out normal integer....hex is typed by humans
         */
        @Override
        public String marshal(Float value) throws Exception {
            return value.toString();
        }

        /**
         * XML --> Java.
         */
        @Override
        public Float unmarshal(String value) throws Exception {
            float aFloat = Float.NaN;
            if (value.equalsIgnoreCase("infinity")) {
                aFloat = Float.POSITIVE_INFINITY;
            } else if (value.equalsIgnoreCase("-infinity")) {
                aFloat = Float.NEGATIVE_INFINITY;
            } else {
                aFloat = Float.parseFloat(value);
            }
            return aFloat;
        }
    }

    /**
     * Color java object doesn't have a empty attribute constructor, this lets
     * us use a Color object directly We can store a color in the format
     * #RRGGBBAA -- hex digits "red"
     * Since I'm making an editor, get away from the multiple attribute and 
     * just store a hex string of the color
     */
    public static class ColorAdapter extends XmlAdapter<String, Color> {

        /**
         * Java ==> XML write out a hex string 0xRRGGBBAA
         */
        @Override
        public String marshal(Color value) throws Exception {
            // Write out 0xRRGGBBAA of color....
            if (value != null) {
                String r, g, b, a;

                r = Integer.toHexString(value.getRed());
                if (r.length() < 2) {
                    r = "0" + r;
                }
                g = Integer.toHexString(value.getGreen());
                if (g.length() < 2) {
                    g = "0" + g;
                }
                b = Integer.toHexString(value.getBlue());
                if (b.length() < 2) {
                    b = "0" + b;
                }
                a = Integer.toHexString(value.getAlpha());
                if (a.length() < 2) {
                    a = "0" + a;
                }
                return ("0x" + r + g + b + a);

            } else {
                return "0xFFFFFFFF";
            }

        }

        /**
         * XML --> Java.
         */
        @Override
        public Color unmarshal(String value) throws Exception {

            int red = 255;
            int green = 255;
            int blue = 255;
            int alpha = 255;
            if (value.toLowerCase().startsWith("0x")) {
                if (value.length() > 7) { // at least "0xRRGGBB"
                    value = value.substring(2, 4);
                    try {
                        red = Integer.parseInt(value, 16);
                    } catch (Exception e) {
                        red = 255;
                    }
                     value = value.substring(4, 6);
                    try {
                        green = Integer.parseInt(value, 16);
                    } catch (Exception e) {
                        green = 255;
                    }
                    value = value.substring(6, 8);
                    try {
                        blue = Integer.parseInt(value, 16);
                    } catch (Exception e) {
                        blue = 255;
                    }
                }
                if (value.length() > 9) { // Alpha too
                    value = value.substring(8, 10);
                    try {
                        alpha = Integer.parseInt(value, 16);
                    } catch (Exception e) {
                        alpha = 255;
                    } 
                }
            }

            // Try to turn 'names' into a real color from color database?
            // this means some person edited the xml file since we don't
            // marshall this ourselves
            return new Color(red, green, blue, alpha);
        }
    }

    /**
     * Load a URL with JAXB.. Don't think we can get class in runtime in this
     * case...?
     *
     * TopNode test = loadJAXB("test.xml", TopNode.class);
     */
    public static <T> T loadURL(URL aURL, Class topClass) {
        T top = null;

        try {
            // setup object mapper using the AppConfig class
            JAXBContext context = JAXBContext.newInstance(topClass);
            //LOG.debug("JAXB "+context.toString());
            XMLInputFactory factory = XMLInputFactory.newInstance();

            try {
                //URLConnection urlConnection = aURL.openConnection();
                URLConnection urlConnection = W2Config.open(aURL);
                InputStream is = urlConnection.getInputStream();
                if (aURL.toString().contains(".gz")) {  // simple hack
                    is = new GZIPInputStream(is);
                }
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                        is));
                XMLStreamReader p = factory.createXMLStreamReader(in);

                // Forces all tags lowercase for fat-fingered humans
                // <thisWorks>data<ThiSworks> is treated as
                // <thisworks>data<thisworks>
                lowerStreamReaderDelegate l = new lowerStreamReaderDelegate(p);
                Unmarshaller u = context.createUnmarshaller();

                // Any strange tags and JAXB freaks.
                u.setEventHandler(new CustomValidationEventHandler());

                @SuppressWarnings("unchecked")
                T attemptOrException = (T) (u.unmarshal(l));

                top = attemptOrException;
            } catch (Exception ex) {
                LOG.error("JAXB Read exception " + ex.toString());
            }


        } catch (Exception e) {
            LOG.error("JAXB Exception " + e.toString());
        }


        return top;
    }

    /**
     * Load a string with JAXB.. Don't think we can get class in runtime in this
     * case...?
     *
     * TopNode test = loadJAXB("test.xml", TopNode.class);
     */
    public static <T> T load(String urlString, Class topClass) {
        T top = null;
        URL aURL;
        try {
            aURL = W2Config.getURL(urlString);
            if (aURL == null) {
                return null;
            }
            top = loadURL(aURL, topClass);
        } catch (Exception c) {
            LOG.debug("XML exception " + c.toString());
        }
        return top;
    }

    public static <T> void save(T root, String urlString, Class topClass) {
        try {

            File file = new File(urlString);
            JAXBContext jaxbContext = JAXBContext.newInstance(topClass);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty("com.sun.xml.internal.bind.xmlHeaders", "\n<!-- Created by WG2. Recommend not editing by hand -->");
            jaxbMarshaller.marshal(root, file);
            jaxbMarshaller.marshal(root, System.out);

        } catch (JAXBException e) {
            LOG.debug("Error writing file " + e.toString());
        }

    }
}

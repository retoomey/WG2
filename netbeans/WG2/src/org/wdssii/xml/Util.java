package org.wdssii.xml;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import javax.xml.bind.JAXBContext;
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

    private static Logger log = LoggerFactory.getLogger(Util.class);

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
                log.debug("XML Error "+evt);
                return true; // Keep going....
            }
            log.debug("XML Error "+evt);
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

            XMLInputFactory factory = XMLInputFactory.newInstance();

            try {
                URLConnection urlConnection = aURL.openConnection();
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
                log.error("JAXB Read exception " + ex.toString());
            }


        } catch (Exception e) {
            log.error("JAXB Exception " + e.toString());
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
            log.debug("XML exception " + c.toString());
        }
        return top;
    }
}

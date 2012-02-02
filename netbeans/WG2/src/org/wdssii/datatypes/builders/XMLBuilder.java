package org.wdssii.datatypes.builders;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.builders.xml.ContoursXML;
import org.wdssii.datatypes.builders.xml.DataTableXML;
import org.wdssii.datatypes.builders.xml.DataTypeXML;
import org.wdssii.index.IndexRecord;
import org.wdssii.xml.Tag_datatype;

/**
 * 
 * Reads a local/remote xml file (gzipped of not) and builds a DataType
 * 
 * @author Robert Toomey
 *
 */
public class XMLBuilder extends Builder {

    private static Logger log = LoggerFactory.getLogger(XMLBuilder.class);

    public static class XMLFileInfo extends BuilderFileInfo {
    }
   
    /** Simple factory.  Since legacy tags don't match the class DataType,
     * we can't do reflection here.  FIXME: more advanced? */
    public static DataTypeXML getDataTypeXMLFor(String name) {
        DataTypeXML xml = null;
        if (name.equals("datatable")) {
            xml = new DataTableXML();
        } else if (name.equals("contours")) {
            xml = new ContoursXML();
        }
        return xml;
    }
    
    public XMLBuilder() {
        super("xml");
    }

    @Override
    public DataType createDataType(IndexRecord rec, WdssiiJobMonitor w) {
        URL url = rec.getDataLocationURL(this);
        if (url == null) {
            return null;
        }

        return createDataTypeFromURL(url, w);
    }

    /** pass in the file name and obtain an object back. */
    public DataType createDataTypeFromURL(URL aURL, WdssiiJobMonitor m) {

        if (m != null) {
            m.beginTask("XMLBuilder", WdssiiJobMonitor.UNKNOWN);
            m.subTask("Reading " + aURL.toString());
        }

        DataType dt = createFromURLStream(aURL, m);
        return dt;

    }

    /** Use to get the attributes that describe the product, choice
     * and time of one of our wdssii format files...
     * 
     * @param path 
     */
    public static XMLFileInfo getBuilderFileInfo(URL aURL) {
        XMLFileInfo info = new XMLFileInfo();
        DataType dt = null;
        InputStream is = null;
        try {
            is = aURL.openStream();
            if (aURL.toString().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader parser = factory.createXMLStreamReader(is);

            // Get the very first start tag, this is the name of the
            // XML class
            boolean done = false;
            while (!done && parser.hasNext()) {
                int event = parser.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        String name = parser.getLocalName();
                        DataTypeXML txml = getDataTypeXMLFor(name);
                        if (txml != null) {
                            // DataTableXML --> DataTable
                            info.DataType = txml.getClass().getSimpleName().replaceAll("XML", "");

                            // Parse DataType tag, stop after STREF and ATTR
                            Tag_datatype tag = new Tag_datatype();
                            tag.processOneAndStop(parser);
                            info.TypeName = tag.name;
                            info.Time = tag.stref.time;
                            String st = tag.attrValues.get("SubType");
                            if (st != null) {
                                info.Choice = st;
                            }
                            info.success = true;
                            done = true;
                            break;
                        }
                    }
                    default:
                        break;
                }
            }
            parser.close();
        } catch (FileNotFoundException e) {
            System.out.println("Got file not found in W2algsBuilder" + e.toString());
        } catch (IOException e) {
            System.out.println("Got IOException in W2algsBuilder " + e.toString());
        } catch (XMLStreamException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e2) {
                    // ok
                }
            }
        }
        return info;
    }

    /** Wdssii XML can create directly from xml stream of URL.
     */
    private DataType createFromURLStream(URL aURL, WdssiiJobMonitor w) {

        DataType dt = null;
        //  File file = new File(path);
        InputStream is = null;
        try {
            // is = new FileInputStream(file);
            //  if (file.getAbsolutePath().endsWith(".gz")) {
            //     is = new GZIPInputStream(is);
            //  }
            is = aURL.openStream();
            if (aURL.toString().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            // Experimenting:
            // Using Stax.  Recommended over SAX and DOM. Will see how it
            // performs. Might be especially useful with CONUS
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader parser = factory.createXMLStreamReader(is);

            // get first 'real' tag (this could be a util)
            boolean done = false;
            while (!done && parser.hasNext()) {
                // Find the first tag matching the types we know:
                // 'datatable'
                // 'contours'
                // 'radialset' (not used)
                int event = parser.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        String name = parser.getLocalName();
                        DataTypeXML xml = getDataTypeXMLFor(name);
                        if (xml != null) {
                            // DataTableXML --> DataTable
                            //xml.getClass().getSimpleName().replaceAll("XML", "");
                            dt = xml.createFromXML(parser);
                            xml = null;
                            done = true;
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
            parser.close();
        } catch (FileNotFoundException e) {
            System.out.println("Got file not found in W2algsBuilder" + e.toString());
        } catch (IOException e) {
            System.out.println("Got IOException in W2algsBuilder " + e.toString());
        } catch (XMLStreamException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e2) {
                    // ok
                }
            }
        }
        return dt;
    }

    /** pass in the file name and obtain an object back. */
    public String resolveToLocalFile(String path) {
        int colon = path.indexOf(':');
        if (colon < 0 || colon == 1) { // colon will be 1 in Windows in the case of drive letters
            return path;
        }
        // Read temp file, return temp file name...
        //return readRemoteNetcdfFile(path);
        return path;
    }

    @Override
    public URL createURLForRecord(IndexRecord rec, String[] params) {
        // Params 0 are of this form for a regular index:
        // 0 - builder name 'W2ALGS'
        // 1 - 'GzippedFile' or some other storage type
        // 2 - Base path such as "http://www/warnings"
        // 3 - 'xmldata' formatter_name
        // 4 - short file such as '1999_ktlx.netcdf.gz'
        String path = params[2] + "/" + params[4];
        URL url = null;
        try {
            url = new URL(path);
        } catch (MalformedURLException e) {
        }
        return url;
    }
}

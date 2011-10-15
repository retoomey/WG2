package org.wdssii.datatypes.builders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.builders.xml.ContoursXML;
import org.wdssii.datatypes.builders.xml.DataTableXML;
import org.wdssii.index.IndexRecord;

/** Builder for the W2ALG products.
 * 
 * FIXME: Only thing I'm reading with this at the moment is Mesonet data.
 * Will need tons more work, just backing up here.
 * 
 * @author Robert Toomey
 *
 */
public class XMLBuilder extends Builder {

    private static Log log = LogFactory.getLog(XMLBuilder.class);

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
              m.subTask("Reading "+aURL.toString());
        }

        DataType dt = createFromURLStream(aURL, m);
        return dt;

    }

    /** Read remote data file 
    private DataType readRemoteFile(String path, String suffix) {
    File localFile = null;
    DataType dt = null;
    
    try {
    log.info("***FILE READING FROM " + path);
    // read from remote file and store in temporary file
    URL url = new URL(path);
    localFile = File.createTempFile("wdssiijava", ".gz");
    FileOutputStream fos = new FileOutputStream(localFile);
    InputStream is = url.openStream();
    byte[] buffer = new byte[1024 * 1024];
    int len = 0;
    while ((len = is.read(buffer)) > 0) {
    fos.write(buffer, 0, len);
    }
    fos.close();
    dt = readLocalFile(localFile.getAbsolutePath());
    } catch (Exception e) {
    //throw new FormatException("Can not read remote file: " + path);	
    log.error("Exception reading file: " + e.toString());
    } finally {
    if (localFile != null) {
    //localFile.delete();
    log.info("Created localfile: " + localFile.getAbsolutePath());
    }
    }
    return dt;
    }
     * */
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
            while (parser.hasNext()) {
                // Find the first tag matching the types we know:
                // 'datatable'
                // 'contours'
                // 'radialset' (not used)
                int event = parser.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        // Would be nice to use reflection here for future and avoid switching
                        // FIXME: ?
                        String name = parser.getLocalName();
                        if (name.equals("datatable")) {
                            log.info("****Creating DataTable from XML ");
                            DataTableXML xml = new DataTableXML();
                            dt = xml.createFromXML(parser);
                            xml = null;
                        } else if (name.equals("contours")) {
                            log.info("*****Create Contours from XML ");
                            ContoursXML xml = new ContoursXML();
                            dt = xml.createFromXML(parser);
                            xml = null;
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
            parser.close();

            /*
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(is, this);  // FIXME: probably better to have seperate object
            
            // Read the stream as an XML document?
             */
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

    private DataType readLocalFile(String path) {

        DataType dt = null;
        File file = new File(path);
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            if (file.getAbsolutePath().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }

            // Experimenting:
            // Using Stax.  Recommended over SAX and DOM. Will see how it
            // performs. Might be especially useful with CONUS
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader parser = factory.createXMLStreamReader(is);

            // get first 'real' tag (this could be a util)
            while (parser.hasNext()) {
                // Find the first tag matching the types we know:
                // 'datatable'
                // 'contours'
                // 'radialset' (not used)
                int event = parser.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        // Would be nice to use reflection here for future and avoid switching
                        // FIXME: ?
                        String name = parser.getLocalName();
                        if (name.equals("datatable")) {
                            log.info("****Creating DataTable from XML ");
                            DataTableXML xml = new DataTableXML();
                            dt = xml.createFromXML(parser);
                            xml = null;
                        } else if (name.equals("contours")) {
                            log.info("*****Create Contours from XML ");
                            ContoursXML xml = new ContoursXML();
                            dt = xml.createFromXML(parser);
                            xml = null;
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
            parser.close();

            /*
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(is, this);  // FIXME: probably better to have seperate object
            
            // Read the stream as an XML document?
             */
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

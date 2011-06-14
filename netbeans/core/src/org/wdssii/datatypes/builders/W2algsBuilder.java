package org.wdssii.datatypes.builders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.builders.xml.ContoursXML;
import org.wdssii.datatypes.builders.xml.DataTableXML;
import org.wdssii.index.IndexRecord;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Builder for the W2ALG products.
 * 
 * FIXME: Only thing I'm reading with this at the moment is Mesonet data.
 * Will need tons more work, just backing up here.
 * 
 * @author Robert Toomey
 *
 */
public class W2algsBuilder extends DefaultHandler implements Builder {

    private static Log log = LogFactory.getLog(W2algsBuilder.class);

    /** Flag if we're using a temp file for data */
    //private boolean myUsingTempFile = false;
    /** The params from the IndexRecord */
    public static enum BuilderParams {

        BUILDER, // W2ALGS
        STORAGE_TYPE, // GZippedFile
        PATH_NAME, // http://test.protect.nssl:8080/mesonet
        FORMATTER_NAME, // xmldata
        FILE_OR_LB_MESSAGE_ID		// mesonet2010...xml.gz
    };

    @Override
    public DataType createObject(IndexRecord rec) {
        DataType dt = null;

        log.info("createObject called");
        String[] params = rec.getParams();
        for (String s : params) {
            log.info("Record Param is " + s);
        }

        // FIXME: only doing Mesonet at the moment.
        if (params.length >= 5) {
            String path = params[BuilderParams.PATH_NAME.ordinal()] + "/" + params[BuilderParams.FILE_OR_LB_MESSAGE_ID.ordinal()];
            log.info("Path is " + path);
            String file = params[BuilderParams.FILE_OR_LB_MESSAGE_ID.ordinal()];
            dt = readRemoteFile(path, file);
        }
        return dt;
    }

    /** Read remote data file */
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

    // *********************** XML parsing
    @Override
    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes atts) throws SAXException {
        if (qualifiedName != null) {
            System.out.println("XML PARSE GOT " + qualifiedName);
        }
    }

    @Override
    public DataRequest createObjectBackground(IndexRecord rec) {
        System.out.println("MAJOR ERROR: W2ALGS BUILDER IS BROKEN AT THE MOMENT");
        return null;
    }
}

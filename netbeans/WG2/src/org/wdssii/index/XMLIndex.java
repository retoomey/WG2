package org.wdssii.index;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.DataUnavailableException;

/**
 * @author jianting.zhang
 * @author lakshman
 * 
 * XMLIndex can read a local or remote file using the URL format
 * 
 */
public class XMLIndex extends Index {

    private static Logger log = LoggerFactory.getLogger(XMLIndex.class);
    private boolean initComplete = false;

    @Override
    public Index newInstance(URL path, URL full, TreeMap<String, String> params, Set<IndexRecordListener> listeners)
            throws DataUnavailableException {
        return new XMLIndex(path, listeners);
    }

    /** meant for prototype factory use only. */
    public XMLIndex() {
        super(null, null);
    }

    @Override
    public void update() {
        if (!initComplete) {
            return;
        }
        if (!isGUIMode()) {  // Not a big deal for GUI
            throw new UnsupportedOperationException("Not a real-time index");
        }
    }

    public XMLIndex(URL aURL, Set<IndexRecordListener> listeners)
            throws DataUnavailableException {

        super(Index.getParent(aURL), listeners);

        try {

            if (log.isInfoEnabled()) {
                log.info("Reading records from " + aURL
                        + " indexLocation=" + getIndexLocation());
            }

            // Note by using URL, xml can actually be a remote file now
            // http://tensor.protect.nssl/somedata/data.xml&protocol=xml
            URLConnection urlConnection = aURL.openConnection();
            InputStream is = urlConnection.getInputStream();
            if (aURL.toString().contains(".gz")) {  // simple hack
                is = new GZIPInputStream(is);
            }

            // parse it
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(is, new SAXIndexHandler(this));

            // We'll assume if we got this far we read everything ok
            lastReadSuccess = true;
        } catch (Exception e) {
            log.error("Failed to load XML format index from " + aURL, e);
            throw new DataUnavailableException(e);
        }
        // avoid update() being called before we are complete
        initComplete = true;
    }
}

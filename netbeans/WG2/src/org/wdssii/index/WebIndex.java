package org.wdssii.index;

import java.net.URL;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.DataUnavailableException;

/**
 * Index that handles the 'webindex' protocol
 * 
 * @author lakshman
 * 
 */
public class WebIndex extends Index {

    private static Logger log = LoggerFactory.getLogger(WebIndex.class);
    private boolean initComplete = false;
    private final URL indexServicePath;
    private final URL indexDataPath;
    private final SAXParser saxParser;
    private int lastRead = -1;

    /** meant for prototype factory use only. */
    public WebIndex() {
        super(null, null);
        indexServicePath = null;
        indexDataPath = null;
        saxParser = null;
    }

    @Override
    public void update() {
        if (!initComplete) {
            return;
        }
        // get new records
        lastReadSuccess = false;
        try {
            URL url = Index.appendQuery(indexServicePath, "lastRead=" + lastRead);
            log.info(url.toString());
            saxParser.parse(url.openStream(), new SAXIndexHandler(this) {

                @Override
                protected void handleRecordsAttribute(String name, String value) {
                    if (name.equals("lastRead")) {
                        lastRead = Integer.parseInt(value);
                    }
                }
            });
            // We'll assume if we got this far without exception that we were successful
            lastReadSuccess = true;
        } catch (Exception e) {
            log.error("Failed to update from " + indexServicePath.toExternalForm(), e);
        }
    }

    /**
     * 
     * @param path   e.g:  http://nmqwd14:8080/?source=KLTX
     * @param listeners
     * @throws DataUnavailableException
     * 
     * EXAMPLES:
     * http://nmqwd3.protect.nssl:8080/webindex/getxml.do?source=KCCX&protocol=webindex&lastRead=5586
     * http://nmqwd3.protect.nssl:8080/KCCX/Reflectivity/01.50/20101008-204958.netcdf.gz
     * baseURL http://nmqwd3.protect.nssl:8080/
     * 
     */
    public WebIndex(URL base, URL fullPath, Set<IndexRecordListener> listeners)
            throws DataUnavailableException {
        super(base, listeners);

        try {
            this.indexServicePath = Index.appendToURL(fullPath, "webindex/getxml.do", true);
            this.indexDataPath = new URL(getIndexLocation());
            if (log.isInfoEnabled()) {
                log.info("Getting records from indexServicePath=" + indexServicePath + " and data from indexDataPath=" + indexDataPath);
            }

            saxParser = SAXParserFactory.newInstance().newSAXParser();

            if (listeners.size() == 0) {
                // no listeners; just get the last record in index ...
                lastRead = -2;
            }

            // avoid update() being called before we are complete
            initComplete = true;
            update();

        } catch (Exception e) {
            log.error("Failed to load index from " + fullPath, e);
            throw new DataUnavailableException(e);
        }
    }

    /**
     * @param listeners
     * @throws DataUnavailableException
     */
    @Override
    public Index newInstance(URL aURL, URL fullURL, TreeMap<String, String> params, Set<IndexRecordListener> listeners)
            throws DataUnavailableException {

        // Try to add the source to the URL path..this is the base path for the IndexRecord, all
        // requests for data will append this.
        // http://tensor.protect.nssl:8080/ ---->
        // http://tensor.protect.nssl:8080/KTLX/
        try {
            URL urlWithSource = new URL(aURL.toString() + params.get("source"));
            aURL = urlWithSource;
        } catch (Exception e) {
            log.error("Webindex URL failed " + aURL);
        }

        return new WebIndex(aURL, fullURL, listeners);
    }

    @Override
    public boolean checkURL(URL url, URL fullurl, TreeMap<String, String> paramMap) {
        return true;
    }
}

package org.wdssii.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.DataUnavailableException;
import org.wdssii.index.fam.FamIndexHelper;

/**
 * Index that reads a code_index.fam listing
 * 
 * 
 * FIXME: FamIndex broken here because of change to pure URL format..
 * Need to fix/test this with a FamIndex....
 * 
 * @author lakshman
 * 
 */
public class FamIndex extends Index {

    private static Logger log = LoggerFactory.getLogger(FamIndex.class);
    private boolean initComplete = false;
    private final File indexDir;
    private final FamIndexHelper helper;
    private final SAXParser saxParser;

    /** meant for prototype factory use only. */
    public FamIndex() {
        super(null, null);
        indexDir = null;
        helper = null;
        saxParser = null;
    }

    @Override
    public void update() {
        if (!initComplete) {
            return;
        }
        lastReadSuccess = false;
        File[] files = helper.getNewFiles();
        boolean allRecordsOK = true;
        for (File file : files) {
            allRecordsOK &= addRecord(file);
        }
        // We'll assume if we get this far and all the records read in we're OK
        lastReadSuccess = allRecordsOK;
    }

    public FamIndex(URL aURL, Set<IndexRecordListener> listeners)
            throws DataUnavailableException {

        super(aURL, listeners);
        saxParser = null;
        indexDir = null;
        helper = null;
        /*
        lastReadSuccess = false;
        this.indexDir = aURL.toString();
        try {
        if (log.isInfoEnabled()) {
        log.info("Reading records from " + path.getAbsolutePath()
        + " indexLocation=" + getIndexLocation());
        }
        helper = FamIndexHelperFactory.newHelper();
        saxParser = SAXParserFactory.newInstance().newSAXParser();
        // get files in directory
        File[] files = helper.getInitialFiles(indexDir.getAbsolutePath());
        boolean allRecordsOK = true;
        for (File file : files) {
        allRecordsOK &= addRecord(file);
        }
        
        // We'll assume if we get this far and all the records read in we're OK
        lastReadSuccess = allRecordsOK;
        } catch (Exception e) {
        log.error("Failed to load index from " + path, e);
        throw new DataUnavailableException(e);
        }
        // avoid update() being called before we are complete
        initComplete = true;
         */
    }

    private boolean addRecord(File file) {
        InputStream is = null;
        boolean success = false;
        try {
            is = new FileInputStream(file);
            if (file.getAbsolutePath().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            saxParser.parse(is, new SAXIndexHandler(this));

            success = true;
        } catch (Exception e) {
            log.warn("Unable to read " + file, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e2) {
                    // ok
                }
            }
        }
        return success;
    }

    @Override
    public Index newInstance(URL aUrl, URL fullUrl, TreeMap<String, String> params, Set<IndexRecordListener> listeners)
            throws DataUnavailableException {

        // broke it
        return new FamIndex(fullUrl, listeners);
    }
}

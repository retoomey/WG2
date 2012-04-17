package org.wdssii.index;

import java.net.URL;
import java.util.Date;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.DataUnavailableException;
import org.wdssii.xml.Tag_codeindex;
import org.wdssii.xml.index.Tag_item;

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

    /**
     * meant for prototype factory use only.
     */
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

            Tag_codeindex t = new Tag_codeindex();
            boolean valid = false;
            try {
                t.processAsRoot(aURL);
                if (t.wasRead()) {  // If we found a <codeindex> tag, good enough we think
                    valid = true;
                }
            } catch (Exception c) {
                valid = false;
            }
            if (valid) {

                // Try to use my new stax parser...since it doesn't create full
                // document, should be faster/less memory.
                // we 'could' create a Tag callback mechanism to avoid even
                // creating tag objects...might be faster.
                for (Tag_item i : t.items) {
                    String[] paramList = new String[]{i.params.getText()};
                    String[] changes = new String[]{null};
                    Date d;
                    if (i.time != null){
                       d = i.time.date;  
                    }else{
                        d = new Date();
                    }
                    IndexRecord rec = IndexRecord.createIndexRecord(d, paramList, changes, i.selections.getText(), getIndexLocation());
                    this.addRecord(rec);
                }
                // We'll assume if we got this far we read everything ok
                lastReadSuccess = true;
            }

        } catch (Exception e) {
            log.error("Failed to load XML format index from " + aURL, e);
            throw new DataUnavailableException(e);

        }
        // avoid update() being called before we are complete
        initComplete = true;
    }

    @Override
    public boolean checkURL(URL url, URL fullurl, TreeMap<String, String> paramMap) {
        boolean valid = false;
        Tag_codeindex t = new Tag_codeindex();
        t.setProcessChildren(false); // don't process <item>, etc...
        try {
            t.processAsRoot(url);
            if (t.wasRead()) {  // If we found a <codeindex> tag, good enough we think
                valid = true;
            }
        } catch (Exception c) {
            valid = false;
        }
        return valid;
    }
}

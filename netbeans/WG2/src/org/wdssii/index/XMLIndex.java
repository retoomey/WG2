package org.wdssii.index;

import java.net.URL;
import java.util.*;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.StringUtil;
import org.wdssii.xml.Tag;
import org.wdssii.xml.index.Tag_codeindex;
import org.wdssii.xml.index.Tag_item;
import org.wdssii.xml.index.Tag_params;

/**
 * @author jianting.zhang
 * @author lakshman
 * @author Robert Toomey
 *
 * XMLIndex can read a local or remote file using the URL format
 *
 */
public class XMLIndex extends Index {

    private final static Logger LOG = LoggerFactory.getLogger(XMLIndex.class);
    /**
     * The XML URL for directly reading items
     */
    private URL myXMLURL;

    @Override
    public Index newInstance(URL path, URL full, TreeMap<String, String> params, Set<IndexRecordListener> listeners) {
        return new XMLIndex(Index.getParent(path), path, listeners);
    }

    /**
     * meant for prototype factory use only.
     */
    public XMLIndex() {
        super(null, null);
    }

    /**
     * The direct URL for reading <item> tags
     */
    public URL getXMLReadURL() {
        return myXMLURL;
    }

    @Override
    public void update() {
    }

    public XMLIndex(URL baseURL, URL xmlURL, Set<IndexRecordListener> listeners) {

        super(baseURL, listeners);
        myXMLURL = xmlURL;
    }

    /**
     * checkURL checks the given url and protocol to see if we can handle this
     * data or not. It should be lightweight and do a minimum amount of work
     * (for example, read a single entry)
     *
     * @param protocol
     * @param url
     * @param fullurl
     * @param paramMap
     * @return
     */
    @Override
    public boolean checkURL(String protocol, URL url, URL fullurl, TreeMap<String, String> paramMap) {
        boolean valid = false;
        boolean tryIt = false;

        // If it ends in '.xml' we will try to load it....
        // If protocol is xml we will try to load it....
        if (fullurl != null) {
            if (fullurl.toString().toLowerCase().endsWith(".xml")) {
                tryIt = true;
            }
        }
        if ((protocol != null) && (protocol.equalsIgnoreCase("xml"))) {
            tryIt = true;
        }

        if (tryIt) {

            // Check top of xml at url (web or local)
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
        }
        LOG.debug("XMLINDEX HANDLE " + url + "," + valid);
        return valid;
    }

    /**
     * Get the items for a given Tag. Webindex uses a Tag_records, for example
     * while xml files use Tag_codeindex by default. This is mostly a hack to
     * get around legacy code that didn't use the same tag name, lol.
     *
     * @param t
     * @return
     */
    protected List<Tag_item> getItems(Tag t) {
        if (t instanceof Tag_codeindex) {
            Tag_codeindex c = (Tag_codeindex) (t);
            return c.items;
        }
        return null;
    }

    /**
     * Load the initial records (all current)
     */
    @Override
    public void loadInitialRecords() {
        Tag_codeindex root = new Tag_codeindex();
        processRoot(root);
    }

    /**
     * Process a root tag's items. Returns a count of read records, or -1 on
     * failure to read at all.
     *
     */
    public int processRoot(Tag root) {
        boolean valid = false;
        int counter = 0;
        URL b = this.getXMLReadURL();
        try {

            if (LOG.isInfoEnabled()) {
                LOG.info("XML reading IndexRecords from " + b);
            }

            try {
                root.processAsRoot(b);
                if (root.wasRead()) {  // If we found a <codeindex> tag, good enough we think
                    valid = true;
                }
            } catch (Exception c) {
                valid = false;
            }
            if (valid) {

                this.processItems(getItems(root));
                // We'll assume if we got this far we read everything ok
                lastReadSuccess = true;
            }

        } catch (Exception e) {
            LOG.error("Failed to load XML format index from " + b+ " "+e.toString());
        }
        if (!valid) {
            counter = -1;
        } // error
        return counter;
    }

    /**
     * Process each 'item' xml tag and create a IndexRecord from it if possible
     */
    public int processItems(List<Tag_item> list) {

        int counter = 0;
        int skipped = 0;
        if (list != null) {
            for (Tag_item i : list) {

                if (processItem(i)) {
                    counter++;
                } else {
                    skipped++;
                }
            }
        }
        LOG.debug("Total records(skipped) read " + counter + "(" + skipped + ")");
        return counter;
    }

    public boolean processItem(Tag_item item) {
        // Params are in for format:
        // buildername {list of params for builder}
        ArrayList<Tag_params> params = item.paramss;
        if (params == null) {
            return false;
        }
        if (params.size() < 1) {
            LOG.error("Missing params in xml item, ignoring record");
            return false;
        }
        // FIXME: by doing array we slow down our reading
        String rawparams = item.paramss.get(0).getText();
        List<String> p = StringUtil.splitOnFirst(rawparams, ' ');
        String builder = p.get(0);
        String builderParams = p.get(1);

        // Get the date from the <time> tag
        Date d;
        if (item.time != null) {
            d = item.time.date;
        } else {
            d = new Date();
        }

        // a common thing in old index files, we just ignore it
        if (builder.equals("Event")) {
            return false;
        }

        IndexRecord rec = IndexRecord.createIndexRecord(d, builder, builderParams, item.selections.getText(), getIndexLocation());
        if (rec != null) {
            this.addRecord(rec);
            return true;
        } else {
            return false;
        }
    }
}

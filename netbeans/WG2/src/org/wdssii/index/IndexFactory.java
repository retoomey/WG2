package org.wdssii.index;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.DataUnavailableException;
import org.wdssii.core.PrototypeFactory;

/**
 * Creates index objects from Index URLs We use the 'p' or 'protocol' get
 * parameter in the URL to determine which index handles it.
 *
 * Format examples: http://tensor.protect.nssl/data/code_index.xml?p=xml
 * file://E:/data/code_index.xml?p=xml
 *
 * @author Lakshman
 * @version $Id: IndexFactory.java,v 1.3 2009/06/02 20:18:30 lakshman Exp $
 * @see IndexRecord
 */
public class IndexFactory {

    private final static Logger LOG = LoggerFactory.getLogger(IndexFactory.class);
    private static final PrototypeFactory<Index> myFactory;

    /**
     * Create the factory from Index.xml in the xml, OR use a stock set of built
     * in defaults. This rarely changes, so this allows overriding without
     * breaking if w2config is missing.
     */
    static {
        myFactory = new PrototypeFactory<Index>(
                "java/Index.xml");
        myFactory.addDefault("xml", "org.wdssii.index.XMLIndex");
        myFactory.addDefault("fam", "org.wdssii.index.FamIndex");
        myFactory.addDefault("webindex", "org.wdssii.index.WebIndex");
        //    myFactory.addDefault("test", "org.wdssii.index.TestIndex");
    }
    private static List<Index> toUpdate = new ArrayList<Index>();
    // FIXME: we're gonna have to steal this and make it controllable (for the GUI)
    private static TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            for (Index index : toUpdate) {
                index.update();
            }
        }
    };

    static {
        new Timer().schedule(timerTask, 1000 * 30, 1000 * 30); // every 30
        // seconds
    }

    /**
     * Creates an index. Add listeners using addIndexRecordListener
     */
    public static Index createIndex(String url)
            throws IllegalArgumentException, DataUnavailableException {
        return createIndex(url, new HashSet<IndexRecordListener>());
    }

    /**
     * convenience function when only one listener needs to be provided.
     */
    public static Index createIndex(String url, IndexRecordListener listener)
            throws IllegalArgumentException, DataUnavailableException {
        Set<IndexRecordListener> listeners = new HashSet<IndexRecordListener>();
        listeners.add(listener);
        return createIndex(url, listeners);
    }

    /**
     * Find the Index that can handle this URL
     */
    public static Index findIndexForURL(URL aURL, boolean createNew) {
        Index anIndex = null;
        try {
            // Get params off the URL..
            TreeMap<String, String> paramMap = new TreeMap<String, String>();
            URL baseURL = Index.getParams(aURL, paramMap);

            // Get the protocol param which is special
            String protocol = paramMap.get("p");
            if (protocol == null) {
                protocol = paramMap.get("protocol");
            }

            // Find first index that can handle this URL
            String name = null;
            Iterator<Entry<String, Index>> i = myFactory.iterator();
            while (i.hasNext()) {
                Entry<String, Index> item = i.next();
                Index index = item.getValue();
                boolean valid = index.checkURL(protocol, baseURL, aURL, paramMap);
                if (valid) {
                    anIndex = index;
                    name = item.getKey();
                    break;
                }
            }
            if (anIndex == null) {
                LOG.error("Don't know how to read data from this URL " + aURL);
            } else {
                LOG.info("Index " + name + " will handle " + aURL);
            }

            // Create a new index from the prototype. (Basically the index in our
            // map is just an empty factory)
            if ((anIndex != null) && createNew) {
                anIndex = anIndex.newInstance(baseURL, aURL, paramMap, null);
            }

        } catch (Exception e) {
            // any exception warn and return gracefully
            LOG.error("Error finding index type for URL:" + e.toString());
        }
        return anIndex;
    }

    /**
     * Return 'true' if this URL can be read by an index
     */
    public static boolean checkURLForIndex(URL aURL) {
        Index anIndex = findIndexForURL(aURL, false); // don't create just check
        return (anIndex != null);
    }

    /**
     * Creates an index. Existing records are supplied to the list of listeners
     */
    public static Index createIndex(String url,
            Set<IndexRecordListener> listeners)
            throws IllegalArgumentException, DataUnavailableException {

        // Should we create URL here, or higher up?
        URL aURL;
        try {
            aURL = new URL(url);
        } catch (MalformedURLException e) {
            LOG.error("URL Malformed, can't create an index: " + e);
            return null;
        }
        // Create a new index
        Index newIndex = findIndexForURL(aURL, true);
        if (newIndex != null) {
            newIndex.addRecordListeners(listeners);
            // Load the initial records...or wait until connection??
            newIndex.loadInitialRecords();
            toUpdate.add(newIndex);
        }
        return newIndex;
    }
}

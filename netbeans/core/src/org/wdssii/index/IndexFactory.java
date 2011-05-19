package org.wdssii.index;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.DataUnavailableException;
import org.wdssii.core.PrototypeFactory;

/**
 * Creates index objects from Index URLs
 * We use the 'p' or 'protocol' get parameter in the URL to determine
 * which index handles it.
 * 
 * Format examples:
 * http://tensor.protect.nssl/data/code_index.xml?p=xml
 * file://E:/data/code_index.xml?p=xml
 * 
 * @author Lakshman
 * @version $Id: IndexFactory.java,v 1.3 2009/06/02 20:18:30 lakshman Exp $
 * @see IndexRecord
 */
public abstract class IndexFactory {

    private static Log log = LogFactory.getLog(IndexFactory.class);
    private static final PrototypeFactory<Index> myFactory;

    /** Create the factory from Index.xml in the xml, OR use
     * a stock set of built in defaults.  This rarely changes, so this
     * allows overriding without breaking if w2config is missing.
     */
    static {
        myFactory = new PrototypeFactory<Index>(
                "java/Index.xml");
        myFactory.addDefault("xml", "org.wdssii.index.XMLIndex");
        myFactory.addDefault("fam", "org.wdssii.index.FamIndex");
        myFactory.addDefault("webindex", "org.wdssii.index.webindex");
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

    /** Creates an index. Add listeners using addIndexRecordListener */
    public static Index createIndex(String url)
            throws IllegalArgumentException, DataUnavailableException {
        return createIndex(url, new HashSet<IndexRecordListener>());
    }

    /** convenience function when only one listener needs to be provided. */
    public static Index createIndex(String url, IndexRecordListener listener)
            throws IllegalArgumentException, DataUnavailableException {
        Set<IndexRecordListener> listeners = new HashSet<IndexRecordListener>();
        listeners.add(listener);
        return createIndex(url, listeners);
    }

    /** Creates an index. Existing records are supplied to the list of listeners */
    public static Index createIndex(String url,
            Set<IndexRecordListener> listeners)
            throws IllegalArgumentException, DataUnavailableException {

        // Should we create URL here, or higher up?
        URL aURL = null;
        try {
            aURL = new URL(url);
        } catch (MalformedURLException e) {
            log.error("URL Malformed, can't create an index: " + e);
            return null;
        }

        // Get params off the URL..
        TreeMap<String, String> paramMap = new TreeMap<String, String>();
        URL baseURL = Index.getParams(aURL, paramMap);

        // Find the protocol
        String protocol = paramMap.get("p");
        if (protocol == null) {
            protocol = paramMap.get("protocol");
        }
        if (protocol == null) {
            log.error("Missing protocol in URL.  Need something like ?p=xml or ?protocol=webindex in URL");
            // FIXME: might 'guess' protocol... stuff ending in ".xml" is probably xml...
            //return null;
            protocol = "xml"; // hack to see something...
        }
        log.info("URL protocol is:" + protocol);

        Index prototype = myFactory.getPrototypeMaster(protocol);
        if (prototype == null) {
            String error = ("No protocol named " + protocol);
            log.error(error);
            throw new IllegalArgumentException(error);
        }

        Index index = prototype.newInstance(baseURL, aURL, paramMap, listeners);
        toUpdate.add(index);
        return index;
    }
}

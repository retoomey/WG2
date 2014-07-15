package org.wdssii.index;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * the Index class connects to a data source and provides access to real-time
 * products as they come in.
 *
 * @author Lakshman
 * @see IndexRecord
 * @see IndexRecordListener
 * @see HistoricalIndex
 *
 */
public abstract class Index {

    /**
     * Create a brand-new Index with these listeners attached.
     */
    public abstract Index newInstance(URL url, URL fullurl, TreeMap<String, String> paramMap, Set<IndexRecordListener> listeners);

    /**
     * Check URL for reading
     */
    public abstract boolean checkURL(String protocol, URL url, URL fullurl, TreeMap<String, String> paramMap);

    /**
     * The update() method will be called periodically. The implementation is
     * allowed to ignore update() calls that happen at inconvenient times.
     */
    public abstract void update();
    /**
     * Collection of listeners for responding to records
     */
    private Set<IndexRecordListener> listeners = new TreeSet<IndexRecordListener>();
    /**
     * Last file/access/read of index was good
     */
    protected boolean lastReadSuccess = false;
    /**
     * Set to true if index is created by GUI
     */
    private static boolean GUIMode = false;
    /**
     * Location of the index
     */
    private URL myIndexBase = null;

    /**
     * @return is this program interactive?
     */
    public static boolean isGUIMode() {
        return GUIMode;
    }

    /**
     * Called by GUI to tell indexes we're running for a display
     *
     * @param flag Is this code within the GUI?
     */
    public static void setGUIMode(boolean flag) {
        GUIMode = flag;
    }

    /**
     * @return true if the last update or load of data was successful The GUI
     * uses this to mark an index as having a problem or not
     */
    public boolean getLastReadSuccess() {
        return lastReadSuccess;
    }

    /**
     * the directory in which this index.xml or index.lb is located
     */
    public String getIndexLocation() {
        if (myIndexBase != null) {
            return myIndexBase.toString();
        } else {
            return "";
        }
    }

    /**
     * The index base is used as a 'base' for getting a file by location. A
     * 'base' could be something like http://www.myserver.com/ktlx where files
     * are http://www.myserver.com/ktlx/reflectivity/file1.tar.gz
     *
     * @return
     */
    public URL getIndexBase() {
        return myIndexBase;
    }

    /**
     * Create index with a set of RecordListeners
     */
    public Index(URL indexLocation, Set<IndexRecordListener> listeners) {
        this.myIndexBase = indexLocation;

        if (listeners != null) {
            this.listeners = listeners;
        }
    }

    /**
     * Register a listener to be notified about new records in this index.
     */
    public void addRecordListener(IndexRecordListener listener) {
        listeners.add(listener);
    }

    public void addRecordListeners(Set<IndexRecordListener> listeners) {
        if (listeners != null) {
            this.listeners = listeners;
        }
    }

    /**
     * Called by subclasses to add record
     */
    public void addRecord(IndexRecord rec) {

        // Let all listeners handle the new record
        for (IndexRecordListener listener : listeners) {
            listener.handleRecord(rec);
        }
    }

    // URL utilities for all index use -----------------------------------------------------
    /**
     * Get the parent of the given URL into another URL:
     * http://tensor.protect.nssl/test/data/index.html --->
     * http://tensor.protect.nssl/test/data/
     *
     * For a DIRECTORY (extra / at end):
     * http://tensor.protect.nssl/test/data/directory/ --->
     * http://tensor.protect.nssl/test/data/ Assumes no URL parameters..
     * "http:///asdfsdfd?stuff=test
     *
     * @param incoming a URL to get the parent of
     *
     * @return base URL
     */
    public static URL getParent(URL incoming) {

        URL output = null;
        if (incoming != null) {
            String path = incoming.toExternalForm();
            if (path != null) {
                int lastSlashPos = path.lastIndexOf('/');

                // Remove solo "/" at end for directory and try again...
                if (lastSlashPos == path.length() - 1) {
                    path = path.substring(0, path.length() - 1);
                    lastSlashPos = path.lastIndexOf('/');
                }

                // Remove the /stuff part... to /
                if (lastSlashPos >= 0) {
                    String baseString = path.substring(0, lastSlashPos + 1);
                    try {
                        output = new URL(baseString);
                    } catch (MalformedURLException e) {
                        // just leave it null
                        output = null;
                    }
                }
            }
        }
        return output;
    }

    /**
     * Take a URL with a GET params list and fill a map with those values,
     * return the URL without the params..
     *
     * @param aUrl incoming url such as 'http://stuff?test=1
     * @param paramMap for output map <test, 1>
     * @return url without params 'http://stuff"
     */
    public static URL getParams(URL aUrl, TreeMap<String, String> paramMap) {
        URL baseURL = null;
        if (aUrl != null) {
            String urlString = aUrl.toString();
            String[] urlAndParams = urlString.split("\\?");
            if (urlAndParams.length == 2) {  // If we have "http://adfasdf/asdfdsf/?otherstuff
                try {
                    baseURL = new URL(urlAndParams[0]);
                } catch (MalformedURLException e) {
                    // ignore it, we'll return null...
                    baseURL = null;
                }

                // Get each pair of values....
                String pairs[] = urlAndParams[1].split("&");
                for (String pair : pairs) {
                    String set[] = pair.split("=");
                    if (set.length == 2) {
                        paramMap.put(set[0], set[1]);  // Store "key = value"
                    }
                }

            } else {  // No params, just return the input...
                baseURL = aUrl;
            }
        }
        return baseURL;
    }

    /**
     * Create a new URL by taking a base and adding extra stuff to the end of
     * it. The query is kept in place. This method will take a URL like:
     * http://tensor.protect.nssl:8080/?test=test and a string like "webindex"
     * and create a URL: http://tensor.protect.nssl:8080/webindex?test=test
     *
     * @param base the base URL, including a possible query part
     * @param extra the stuff to add on the end
     * @param keepQuery true to keep query part of URL
     * @return
     */
    public static URL appendToURL(URL base, String extra, boolean wantQuery) {
        boolean keepQuery = false;
        String q = null;
        if (wantQuery) {
            q = base.getQuery();
            if ((q != null) && (q.length() != 0)) {
                keepQuery = true;
            }
        }
        URL newURL = base;
        try {
            newURL = new URL(base, keepQuery ? (extra + '?' + q) : extra);
        } catch (MalformedURLException e) {
            // LOG.error("Couldn't append string to URL "+base+" + "+extra);
        }

        return newURL;
    }

    public static URL appendQuery(URL servicePath, String query) throws MalformedURLException {
        String currentPath = servicePath.getPath();
        String currentQuery = servicePath.getQuery();
        String newQuery = (currentQuery.length() == 0) ? query : (currentQuery + '&' + query);
        return new URL(servicePath, currentPath + "?" + newQuery);
    }

    /**
     * Load the initial records of the index
     */
    public abstract void loadInitialRecords();

    /**
     * Is this URL a local file?
     */
    public static boolean isLocalFile(URL url) {
        String scheme = url.getProtocol();
        return "file".equalsIgnoreCase(scheme) && !hasHost(url);
    }

    /**
     * Does this URL have a host
     */
    public static boolean hasHost(URL url) {
        String host = url.getHost();
        return host != null && !"".equals(host);
    }

    /**
     * Called when about to dispose
     */
    public void aboutToDispose() {
    }
}

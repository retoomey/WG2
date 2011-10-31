package org.wdssii.core;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This configuration class finds and uses the pertinent configuration
 * information from the WDSS2/w2config directory.
 * 
 * Updated to handle URLS and patterns
 * Configuration format is of the form of URL strings with a replacement
 * of the string "{1}", etc. with passed in fields.
 * "http://www.google.com/{1}.xml" with "icons/iconfile1" gives us
 * "http://www.google.com/icons/iconfile1.xml" as a possible location.
 * 
 * We can pull from a cvs repository directly on the fly with a pattern such
 * as:
 * "http://tensor.protect.nssl/cgi-bin/viewcvs.cgi/cvs/w2/w2config/{1}?view=co"
 * 
 * @author Lakshman
 */
public class W2Config {

    private static final Log log = LogFactory.getLog(W2Config.class);
    /** List of configuration strings.  These get converted to URLS
    after macro substitution of characters.
     */
    private static List<String> configPatterns;

    static {
        // For testing pulling over web w2config only....
        boolean useLocal = true;
        boolean useWeb = true;
        /*
         * An environment variable W2_CONFIG_LOCATION $HOME $HOME/w2config,
         * $HOME/WDSS2/w2/w2config, $HOME/WDSS2/w2config, etc. /etc/w2config
         */
        configPatterns = new ArrayList<String>();
        log.debug("Looking for your w2config locations.");
        if (useLocal) {
            String s = System.getenv("W2_CONFIG_LOCATION");
            if (s != null) {
                String[] locations = s.split(":");
                for (String location : locations) {
                    addDir(location);
                }
            }
            s = System.getProperty("user.home");
            if (addDir(s)) {
                addDir(s + "/w2config");
                addDir(s + "/WDSS2/w2config");
                addDir(s + "/WDSS2/src/w2/w2config");
            }

            // This is for finding the checked in w2config within my compiled source tree..
            // Not sure if there's an easier way to do this or not
            try {
                URL bUrl = ClassLoader.getSystemResource("org/wdssii");

                // ---> filepath/WG2/build/classes/org/wdssii...
                String bUrls = bUrl.toString();
                bUrls = bUrls.replaceFirst("/build/classes/org/wdssii", "/w2config");
                bUrl = new URL(bUrls);
                // if this is a good url, it's working...so add it..
                addPattern(bUrl.toString()+"/{1}");
            } catch (Exception ee) {
                // oh well don't care...we tried
            }

            addDir("/etc/w2config");
        }
        if (useWeb) {
            // Attempt to get data from web located w2config locations.  We have
            // a cvs repository, also added stuff to the google project page.
            addPattern("http://tensor.protect.nssl/cgi-bin/viewcvs.cgi/cvs/w2/w2config/{1}?view=co");
            addPattern("http://wg2.googlecode.com/hg/netbeans/WG2/w2config/{1}");
        }
    }

    /** Add directory as a local directory, iff that directory exists
     * on the local disk.  The final pattern is of the form "dir+"/{1}"
     * @param dir
     * @return 
     */
    private static boolean addDir(String dir) {
        if (dir != null) {
            File aFile = new File(dir);
            if (aFile.exists()) {
                try {
                    // We need the URL for the file...
                    // Doing it this way encodes spaces as %20, which is
                    // proper URL format.
                    URL u = aFile.toURI().toURL();
                    String path = u.toExternalForm();
                    if (path.endsWith("/")) {
                        path = path + "{1}";
                    } else {
                        path = path + "/{1}";
                    }
                    addPattern(path);
                    log.debug("Added w2config pattern: " + path);
                    return true;
                } catch (MalformedURLException ex) {
                    // This shouldn't happen if the file exists....
                }
            }
            log.info("Ignoring " + dir + " -- not there");
        }
        return false;
    }

    private static void addPattern(String pattern) {
        configPatterns.add(pattern);
    }

    /**
     * will search the config directories for the first URL that matches the
     * given substitution. can return null.
     * 
     * For example, passing colormaps/Reflectivity, you will get a File
     * corresponding to /etc/w2config/colormaps/Reflectivity
     */
    public static URL getURL(String s) {
        URL aURL = null;
        for (String p : configPatterns) {
            String current = p;
            current = current.replaceFirst("\\{1\\}", s);
            aURL = tryReadingURL(current);
            if (aURL != null) {
                return aURL;
            }
        }
        return aURL;
    }

    /** Try reading URL at given location.  For HTTP we read the header
     * only and check for OK response.  We assume this means it's good and
     * return this URL for the display to use.  Of course it could fail later,
     * but that's not our job to check.
     * 
     * @param s
     * @return 
     */
    private static URL tryReadingURL(String s) {
        URL aURL = null;
        try {
            aURL = new URL(s);
            try {

                if (isLocalFile(aURL)) {
                    // If it's a local URL, then check for existance and read
                    // of the file.

                    // URL will have spaces encoded as %20, which will fail in windows
                    // This will decode %20 into actual spaces for the filename
                    URI uri = new URI(aURL.toString());
                    File aFile = new File(uri.getPath());
                    if (!(aFile.exists() && aFile.canRead())) {
                        aURL = null;
                    }
                } else {
                    // If it's a web URL, then check for non-404 (missing)
                    // by reading just the html HEAD (cheap)
                    URLConnection c = aURL.openConnection();
                    if (c instanceof HttpURLConnection) {
                        HttpURLConnection h = (HttpURLConnection) (c);
                        // Don't read all the data at the URL...
                        h.setRequestMethod("HEAD");
                        int code = h.getResponseCode();
                        if (code != HttpURLConnection.HTTP_OK) {
                            aURL = null;
                        }
                    }
                }
            } catch (IOException ex) {
                aURL = null;
            } catch (URISyntaxException ex) {
                aURL = null;
            }
        } catch (MalformedURLException ex) {
            aURL = null;
        }
        return aURL;
    }

    public static boolean isLocalFile(URL url) {
        String scheme = url.getProtocol();
        return "file".equalsIgnoreCase(scheme) && !hasHost(url);
    }

    public static boolean hasHost(URL url) {
        String host = url.getHost();
        return host != null && !"".equals(host);
    }

    private static String getAllPatterns() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < configPatterns.size(); ++i) {
            sb.append(configPatterns.get(i));
            if (i != configPatterns.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Parse and return the XML Dom element corresponding to a partial file
     * name.  (SAX)
     * 
     * For example, passing colormaps/Reflectivity, you may get the DOM element
     * from /etc/w2config/colormaps/Reflectivity
     * @deprecated
     */
    public static Element getElement(String filename)
            throws ConfigurationException {

        // First try with ".xml" on the end of it...
        URL u;
        u = getURL(filename + ".xml");
        if (u == null) {
            u = getURL(filename);
        }
        if (u == null) {
            return null;
        }
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = parser.parse(u.openStream());
            return doc.getDocumentElement();
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }
}

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
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This configuration class finds and uses the pertinent configuration
 * information from the WDSS2/w2config directory.
 *
 * Updated to handle URLS and patterns Configuration format is of the form of
 * URL strings with a replacement of the string "{1}", etc. with passed in
 * fields. "http://www.google.com/{1}.xml" with "icons/iconfile1" gives us
 * "http://www.google.com/icons/iconfile1.xml" as a possible location.
 *
 * We can pull from a cvs repository directly on the fly with a pattern such as:
 * "http://tensor.protect.nssl/cgi-bin/viewcvs.cgi/cvs/w2/w2config/{1}?view=co"
 *
 * @author Lakshman
 */
public class W2Config {

    private final static Logger LOG = LoggerFactory.getLogger(W2Config.class);
    /**
     * List of configuration strings. These get converted to URLS after macro
     * substitution of characters.
     */
    private static List<String> configPatterns;
    /**
     * The timeout for trying to pull a URL off a remote web-server
     */
    private static final float WEB_TIMEOUT_SECS = .5f;

    static {
        // For testing pulling over web w2config only....
        boolean useLocal = true;
        boolean useWeb = true;
        /*
         * An environment variable W2_CONFIG_LOCATION $HOME $HOME/w2config,
         * $HOME/WDSS2/w2/w2config, $HOME/WDSS2/w2config, etc. /etc/w2config
         */
        configPatterns = new ArrayList<String>();
        LOG.debug("Looking for your w2config locations.");
        if (useLocal) {
            String s = System.getenv("W2_CONFIG_LOCATION");
            if (s != null) {
                String[] locations = s.split(":");
                for (String location : locations) {
                    addDir(location);
                }
            }

            // Use the 'home' directory of the user
            s = System.getProperty("user.home");
            if (addDir(s)) {
                addDir(s + "/w2config");
                addDir(s + "/WDSS2/w2config");
                addDir(s + "/WDSS2/src/w2/w2config");
            }

            // Use the 'running' directory of the program/jar
            s = System.getProperty("user.dir");
            if (addDir(s)) {
                addDir(s + "/w2config");
            }

            // This is for finding the checked in w2config within my compiled source tree..
            // Not sure if there's an easier way to do this or not
            try {
                URL bUrl = ClassLoader.getSystemResource("org/wdssii");

                // ---> filepath/WG2/build/classes/org/wdssii...
                String bUrls = bUrl.toString();

                // For the moment ignore stuff within jar, since it
                // requires a different connection I think
                if (!bUrls.startsWith("jar")) {
                    bUrls = bUrls.replaceFirst("/build/classes/org/wdssii", "/w2config");   // Netbeans checkout path
                    bUrls = bUrls.replaceFirst("/bin/org/wdssii", "/netbeans/WG2/w2config"); // Eclipse checkout path
                    bUrl = new URL(bUrls);
                    // if this is a good url, it's working...so add it..
                    addPattern(bUrl.toString() + "/{1}");
                }
            } catch (Exception ee) {
                // oh well don't care...we tried
            }

            addDir("/etc/w2config");
        }
        if (useWeb) {
            // Attempt to get data from web located w2config locations.  We have
            // a cvs repository, also added stuff to the google project page.
        }
    }

    /**
     * Add directory as a local directory, iff that directory exists on the
     * local disk. The final pattern is of the form "dir+"/{1}"
     *
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
                    return true;
                } catch (MalformedURLException ex) {
                    // This shouldn't happen if the file exists....
                }
            }
            LOG.info("Ignoring " + dir + " -- not there");
        }
        return false;
    }

    private static void addPattern(String pattern) {
        configPatterns.add(pattern);
        LOG.debug("Added w2config pattern: " + pattern);
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
            	LOG.error("Load URL non null match '"+p+"' --> "+aURL);

                return aURL;
            }
        }
        aURL = tryReadingURL("file:"+s); // absolute paths"

        return aURL;
    }

    /**
     * Try reading URL at given location. For HTTP we read the header only and
     * check for OK response. We assume this means it's good and return this URL
     * for the display to use. Of course it could fail later, but that's not our
     * job to check.
     *
     * @param s
     * @return
     */
    private static URL tryReadingURL(String s) {
        URL aURL;
        LOG.error("READ URL "+s);
        try {
            aURL = new URL(s);
            try {

                if (isLocalFile(aURL)) {
                    // If it's a local URL, then check for existence and read
                    // of the file.

                    // URL will have spaces encoded as %20, which will fail in windows
                    // This will decode %20 into actual spaces for the filename
                    URI uri = new URI(aURL.toString());
                    String uripath = uri.getPath();
                    if (uripath == null) { return null; }
                    File aFile = new File(uripath);
                    if (!(aFile.exists() && aFile.canRead())) {
                        aURL = null;
                    }
                } else {
                    // If it's a web URL, then check for non-404 (missing)
                    // by reading just the html HEAD (cheap)
                    URLConnection c = aURL.openConnection();
                    if (c instanceof HttpURLConnection) {
                        HttpURLConnection h = (HttpURLConnection) (c);
                        // Set the timeout, don't want the display hanging
                        // trying to get a remote file.  Humm.  How to handle
                        // this properly?  Dialog?  FIXME?
                        h.setConnectTimeout((int) (WEB_TIMEOUT_SECS * 1000.0f));
                        h.setAllowUserInteraction(false);
                        h.setDoInput(false);
                        h.setDoOutput(false);

                        // ISPs and others redirect common errors to a 
                        // custom html page, which we would confuse as a valid
                        // data file.
                        h.setInstanceFollowRedirects(false);

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
        // LOG.debug("Tried URL " + s + " and got " + aURL);
        return aURL;
    }

    /**
     * Our version of openConnection with extra settings to try to avoid hanging
     */
    public static URLConnection open(URL aURL) throws IOException {
        // If it's a web URL, then check for non-404 (missing)
        // by reading just the html HEAD (cheap)
        URLConnection c = aURL.openConnection();
        if (c instanceof HttpURLConnection) {
            HttpURLConnection h = (HttpURLConnection) (c);
            // Set the timeout, don't want the display hanging
            // trying to get a remote file.  Humm.  How to handle
            // this properly?  Dialog?  FIXME?
            h.setConnectTimeout((int) (WEB_TIMEOUT_SECS * 1000.0f));
            h.setAllowUserInteraction(false);
            // ISPs and others redirect common errors to a 
            // custom html page, which we would confuse as a valid
            // data file.
            h.setInstanceFollowRedirects(false);

            // Don't read all the data at the URL...
            //  h.setRequestMethod("HEAD");
            // int code = h.getResponseCode();
            //if (code != HttpURLConnection.HTTP_OK) {
            //aURL = null;
            // }
        }
        return c;
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
     * name. (SAX)
     *
     * For example, passing colormaps/Reflectivity, you may get the DOM element
     * from /etc/w2config/colormaps/Reflectivity
     *
     * @deprecated
     */
    public static Element getElement(String filename) {

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
            LOG.error("Unable to parse element from " + filename);
        }
        return null;
    }

    /**
     * Get the preferred directory for save location... This will try to find a
     * directory in path, if not it will try to create a directory with this
     * name within the base w2config folder
     */
    public static URL getPreferredDir(String base) {

        URL preferredDir = null;
        // Get the preferred directory for symbology?
        try {
            preferredDir = W2Config.getURL(base + "/");
            if (preferredDir == null) {
                // Let's try to make one inside a w2config folder...
                // We 'should' have at least have our w2config folder from
                // the distribution...
                preferredDir = W2Config.getURL("w2config");
                if (preferredDir != null) {
                    // Ok, we have a w2config.  We'll try to make a directory
                    String output = preferredDir.getFile() + "/" + base;
                    File dir = new File(output);
                    if (!dir.exists()) {
                        if (dir.mkdir()) {
                            LOG.info("Created directory "+dir.getAbsolutePath());
                            preferredDir = dir.toURI().toURL();
                        } else {
                            LOG.error("Couldn't create directory '" + base + "' within '" + preferredDir.getFile() + "',maybe permission settings?");
                        }
                    } else {
                        preferredDir = dir.toURI().toURL();
                    }
                } else {
                    LOG.error("Couldn't find ANY base w2config folder, if you downloaded the distribution, there should be at least a w2config folder in there");
                    LOG.error("You might need to reinstall.");
                }
            }
        } catch (Exception e) {
            // Well..we might be SOL here...dialog and quit?
            LOG.error("Exception looking for required directory '"+base+"' "+e.toString());
        }
        return preferredDir;
    }
}

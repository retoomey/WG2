package org.wdssii.core;

import java.io.File;
import java.io.InputStream;
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
 * FIXME: gonna have to merge this with my RCP package stuff for it to work
 * correctly -- Robert
 * 
 * @author Lakshman
 */
public class W2Config {

    private static Log log = LogFactory.getLog(W2Config.class);
    private static List<String> configDirectories = loadConfigDirectories();

    /** Load an input stream from a path relative to root of the project
     * For example, passing in "icons/test.png" will
     * 1.  Find it in the top directory of project: (well find it in classpath)
     * 	C:/mysourcecode/WJ, C:/mysourcecode/WJ/icons
     * --> "icons/test.png"
     * 2.  Find it inside the jar of the plugin.
     * C:/WJEXPORT/launch.exe, C:/WJEXPORT/plugins/ourplug.jar (icons at top level of jar)
     * --> "icons/test.png"
     * 
     */
    public static InputStream streamFromFile(String relativePath) {
        // We want the 'root' of the plugin or project directory, without a '/'
        // java appends the package path "org/test/etc/relativePath"
        InputStream s = W2Config.class.getResourceAsStream("/" + relativePath);

        return s;
    }

    private static boolean addDir(String dir, List<String> configDirectories) {
        if (dir != null) {
            if (new File(dir).exists()) {
                configDirectories.add(dir);
                log.debug("Will search w2config directory: " + dir);
                return true;
            }
            log.info("Ignoring " + dir + " -- not there");
        }
        return false;
    }

    private static List<String> loadConfigDirectories() {
        /*
         * An environment variable W2_CONFIG_LOCATION $HOME $HOME/w2config,
         * $HOME/WDSS2/w2/w2config, $HOME/WDSS2/w2config, etc. /etc/w2config
         */
        List<String> configDirectories = new ArrayList<String>();
        log.debug("Looking for your w2config directories.");
        String s = System.getenv("W2_CONFIG_LOCATION");
        if (s != null) {
            String[] locations = s.split(":");
            for (String location : locations) {
                addDir(location, configDirectories);
            }
        }
        s = System.getProperty("user.home");
        if (addDir(s, configDirectories)) {
            addDir(s + "/w2config", configDirectories);
            addDir(s + "/WDSS2/w2config", configDirectories);
            addDir(s + "/WDSS2/src/w2/w2config", configDirectories);
        }
        addDir("/etc/w2config", configDirectories);
        return configDirectories;
    }

    /**
     * will search the config directories for the first file that matches the
     * given filename. can return null.
     * 
     * For example, passing colormaps/Reflectivity, you will get a File
     * corresponding to /etc/w2config/colormaps/Reflectivity
     */
    public static File getFile(String filename) throws ConfigurationException {
        for (String configDir : configDirectories) {
            String s = configDir + "/" + filename;
            File f = new File(s);
            if (f.exists()) {
                if (log.isInfoEnabled()) {
                    log.info("Using config file " + s);
                }
                return f;
            }
        }
        String error = "Could not find config file " + filename + " in " + allconfigdirs();
        log.error(error);
        throw new ConfigurationException(error);
    }

    private static String allconfigdirs() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < configDirectories.size(); ++i) {
            sb.append(configDirectories.get(i));
            if (i != configDirectories.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Parse and return the XML Dom element corresponding to a partial file
     * name.
     * 
     * For example, passing colormaps/Reflectivity, you may get the DOM element
     * from /etc/w2config/colormaps/Reflectivity
     */
    public static Element getFileElement(String filename)
            throws ConfigurationException {
        File f = getFile(filename);
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = parser.parse(f);
            return doc.getDocumentElement();
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }
}

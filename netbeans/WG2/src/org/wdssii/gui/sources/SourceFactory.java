package org.wdssii.gui.sources;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.GUIPlugInPanel;
import org.wdssii.gui.views.SourcesURLLoadDialog;

/**
 * Any source should implement a SourceFactory for creating it
 *
 * @author Robert Toomey
 */
public abstract class SourceFactory {

	private static Logger log = LoggerFactory.getLogger(SourceFactory.class);
	private final static ArrayList<String> sourceNames = new ArrayList<String>();
	private final static ArrayList<SourceFactory> factories;

	static {
		// Introduce all the sources we know about....
		// Could get these dynamically or from an xml file
		sourceNames.add("org.wdssii.gui.sources.IndexSource");
                sourceNames.add("org.wdssii.gui.sources.WMSSource");
		factories = createFactories();
	}

	/** Create a small object for creating each Source on start up */
	public static ArrayList<SourceFactory> createFactories(){

		ArrayList<SourceFactory> f = new ArrayList<SourceFactory>();
		for (String t : sourceNames) {
			final String s = t + "$Factory";
			Class<?> aClass;
			try {
				aClass = Class.forName(s);
				SourceFactory factory = (SourceFactory) aClass.newInstance();
				f.add(factory);
				log.info("Created SourceFactory "+s);
			} catch (Exception e) {
				// ? just warn
				log.error("Error during source factory " + e.toString());
			}
		}
		return f;
	}

	/**
	 * Return true if we can handle this file, used by file
	 * dialogs
	 */
	public static boolean canAnyHandleFileType(File aFile) {

		for (SourceFactory f : factories) {
			boolean canHandle =  f.canHandleFileType(aFile);
			if (canHandle){return true; }
		}
		return false;
	}

	/**
	 * Return all file descriptions that we can load, used by
	 * file dialogs
	 */
	public static Set<String> getAllHandledFileDescriptions() {

		TreeSet<String> fileEndings = new TreeSet<String>();

		for (SourceFactory f : factories) {
			Set<String> s = f.getHandledFileDescriptions();
			fileEndings.addAll(s);
		}
		return fileEndings;
	}

	/** Get the factory for this URL. Note that we are letting the
	 * first SourceFactory that says yes handle the URL, so it is
	 * important that the factory do more than just check the file
	 * extension such as '.xml'  (a CFRadialSource might load a netcdf file,
	 * but WDSSII also can read netcdf)
	 * @param aURL
	 * @return factory for this URL, if any
	 */
	public static SourceFactory getFactoryForURL(URL aURL){
		log.debug("Source factory getting factory for "+aURL);
		SourceFactory worker = null;
		for (SourceFactory f : factories) {
	              if (f.canCreateFromURL(aURL)){;
		         worker = f;
			 break;
		      }
		}
		return worker;
	}

	// Individual source methods

	/** Can this source handle this file type? */
	public abstract boolean canHandleFileType(File f);

	/** Get the file descriptor for open file dialog */
	public abstract Set<String> getHandledFileDescriptions();

	/** Can this source create from THIS URL?  It should sample it, but
	 not fully load it */
        public abstract boolean canCreateFromURL(URL aURL);

	/** Create the GUI plug in for extra loading params.  These differ
	 from the SourceGUI in that they are used before loading the source */
	public abstract GUIPlugInPanel createParamsGUI(SourcesURLLoadDialog d);

	/** Get the description for the source dialog */
	public abstract String getDialogDescription();
}
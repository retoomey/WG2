package org.wdssii.gui;

import org.wdssii.gui.views.infonode.Infonode;
import org.wdssii.gui.views.infonode.InfonodeViews;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.commands.SourceAddCommand.IndexSourceAddParams;
import org.wdssii.gui.commands.SourceAddCommand.SourceAddParams;
import org.wdssii.gui.features.EarthBallFeature;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.MapFeature;
import org.wdssii.gui.features.MapGUI;
import org.wdssii.gui.sources.IndexSource;
import org.wdssii.gui.views.ViewManager;
import org.wdssii.gui.views.WindowManager;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.wdssii.core.CommandManager;
import org.wdssii.core.W2Config;
import org.wdssii.core.WDSSII;
import org.wdssii.core.WdssiiJob;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.storage.DataManager;

/**
 * 
 * The Application...
 *
 * @author Robert Toomey
 */
public class Application {

	private final static Logger LOG;// = LoggerFactory.getLogger(Application.class);
	public final static String NAME = "WDSSII GUI";
	public final static int MAJOR_VERSION = 2;
	public final static int MINOR_VERSION = 1;
	public static final boolean USE_HEAVYWEIGHT_GL = false;

	public final static String logmessage;

	static {
		// Initialize our logging to Sf4j. Explicitly calling classes by full name
		// just because all the similiar names is slightly confusing. We create
		// a simple wrapper to all logging to allow us to patch into various logging
		// systems. Which is what sl4fj does as well, but we need to remove the
		// dependency on sl4fj imports in our code as well for systems where it doesn't
		// play well.
		org.wdssii.log.sl4fj.Slf4jLoggerFactory ourLogging = new org.wdssii.log.sl4fj.Slf4jLoggerFactory();
		org.wdssii.log.LoggerFactory.setLoggerFactory(ourLogging);
		logmessage = ourLogging.firstMessage;

		LOG = LoggerFactory.getLogger(Application.class);
	}

	public void start() {

		GUISingletonManager.setup();

		DataManager.getInstance();

		// Create the WDSSII low-level core for products
		WDSSII.getInstance();

		// Add the netbeans job creator
		WdssiiJob.introduce(new JobSwingFactory());

		// Defaults to UIManager
		// Don't allow double click to work in file chooser
		UIManager.put("FileChooser.readOnly", Boolean.TRUE);

		org.wdssii.xml.PointColorMap.loadStockMaps();
		// System.exit(1);
		// DockWindows are in the swing thread
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				int oldOne = 1;
				if (oldOne == 1) { // Old one we're replacing
					InfonodeViews d = new InfonodeViews();
					ViewManager.init(d);
				} else if (oldOne == 0) { // New one...
					Infonode d = new Infonode();
					WindowManager.init(d);
				}

				// Lazy load examples up
				boolean loadExamples = true;
				if (loadExamples) {

					Feature earthBall = new EarthBallFeature(FeatureList.theFeatures);
					FeatureList.theFeatures.addFeature(earthBall);
					
					URL mapURL = W2Config.getURL("maps/shapefiles/usa/ok/okcnty.shp");
					if (mapURL != null) {
						String filename = mapURL.getPath();
						Feature testOne = new MapFeature(FeatureList.theFeatures, filename);
						FeatureList.theFeatures.addFeature(testOne);
					}

					URL aURL = W2Config.getURL("data/KTLX_05031999/code_index.xml");
					if (aURL != null) {
						SourceAddParams params = new IndexSourceAddParams("KTLX-MAY-1999", aURL, false, true,
								IndexSource.HISTORY_ARCHIVE);
						SourceAddCommand c = new SourceAddCommand(params);
						c.setConfirmReport(false, false, null);
						CommandManager.getInstance().executeCommand(c, false);
					}
				}
			}
		});
	}

	public static void main(String[] args) {

		final String arch = System.getProperty("os.arch");
		final String name = System.getProperty("os.name");
		final String bits = System.getProperty("sun.arch.data.model");
		final String userdir = System.getProperty("user.dir");

		LOG.info("WDSSII GUI VERSION 2.0 [{}, {}, ({} bit)]", new Object[] { name, arch, bits });

		if (bits.equals("32")) {
			LOG.error("Sorry, currently no 32 bit support.\n  You really want to run this on a 64 bit OS");
			LOG.error("You may have 64 and 32 bit Java and be running the 32 version in your path");
			System.exit(0);
		}
		LOG.info("JAVA VERSION {}", System.getProperty("java.specification.version"));
		LOG.info("USER DIRECTORY {}", userdir);
		if (logmessage != null) {
			LOG.info(logmessage);
		}

		// initialize geotools to whatever WDSSII logging is bound too.
		try {
			org.geotools.util.logging.Logging.GEOTOOLS
					.setLoggerFactory("org.geotools.util.logging.WdssiiLoggerFactory");
			// "org.geotools.util.logging.Slf4jLoggerFactory");
		} catch (Exception e) {
			LOG.error("Couldn't bind GEOTOOLS logger system to ours " + e.toString());
		}

		// Use the user directory (where we are running) to dynamically
		// add the OS information to the path. This is where all of our
		// native libraries will be found

		addNativeLibrariesOrDie(userdir);

		Application a = new Application();
		a.start();
	}

	public static void addNativeLibrariesOrDie(String rootdir) {
		// FIXME: move/cleanup native locations?
		String arch = System.getProperty("os.arch");

		// Newer machines, mac in particular returning
		// x86_86 instead of amd64
		if (arch.equalsIgnoreCase("x86_64")) {
			arch = "amd64";
		}

		rootdir += "/release/modules/lib/" + arch;
		LOG.info("Native library directory is: " + rootdir);
		try {
			addLibraryPath(rootdir);
		} catch (Exception ex) {
			LOG.error("Couldn't add native library path dynamically");
			System.exit(0);
		}
	}

	/**
	 * Adds the specified path to the java library path
	 *
	 * @param pathToAdd the path to add
	 * @throws Exception
	 */
	public static void addLibraryPath(String pathToAdd) throws Exception {
		final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
		usrPathsField.setAccessible(true);

		// get array of paths
		final String[] paths = (String[]) usrPathsField.get(null);

		// check if the path to add is already present
		for (String path : paths) {
			if (path.equals(pathToAdd)) {
				return;
			}
		}

		// add the new path
		final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
		newPaths[newPaths.length - 1] = pathToAdd;
		usrPathsField.set(null, newPaths);
	}
}

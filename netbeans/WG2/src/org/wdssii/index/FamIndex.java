package org.wdssii.index;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.DataUnavailableException;
import org.wdssii.index.fam.FamIndexHelper;
import org.wdssii.xml.index.Tag_item;

/**
 * Index that reads a code_index.fam listing
 *
 * @author lakshman
 * @author Robert Toomey
 *
 */
public class FamIndex extends XMLIndex {

	private static Logger log = LoggerFactory.getLogger(FamIndex.class);
	private boolean initComplete = false;
	private final File indexDir;
	private final FamIndexHelper helper;
	/**
	 * The JNotify id of our directory watcher
	 */
	private int myJNotifyWatchID;
	/**
	 * Do we have a watcher?
	 */
	private boolean myJNotifyConnected = false;

	/**
	 * meant for prototype factory use only.
	 */
	public FamIndex() {
		super(null, null, null);
		indexDir = null;
		helper = null;
	}

	@Override
	public void update() {
	}

	public FamIndex(URL aURL, Set<IndexRecordListener> listeners)
		throws DataUnavailableException {

		super(Index.getParent(aURL), Index.getParent(aURL), listeners);
		log.error("*****************FAM IN IS " + aURL.toString());
		log.error("****PARENT IS " + Index.getParent(aURL));
		helper = null;
		// We can link to any local directory.
		// FIXME: could filter directories that don't end in '.fam', but why bother?
		boolean canHandle = false;
		File temp = null;
		if (Index.isLocalFile(aURL)) {
			String fs = aURL.getFile();
			if (!fs.isEmpty()) {
				File f = new File(fs);
				if (f.isDirectory()) {
					temp = f;
				}
			}
		}
		indexDir = temp;
	}

	@Override
	public Index newInstance(URL aUrl, URL fullUrl, TreeMap<String, String> params, Set<IndexRecordListener> listeners)
		throws DataUnavailableException {

		// broke it
		return new FamIndex(aUrl, listeners);
	}

	@Override
	public boolean checkURL(String protocol, URL url, URL fullurl, TreeMap<String, String> paramMap) {

		// We can link to any local directory.
		// FIXME: could filter directories that don't end in '.fam', but why bother?
		boolean canHandle = false;
		if (Index.isLocalFile(url)) {
			String fs = url.getFile();
			if (!fs.isEmpty()) {
				File f = new File(fs);
				if (f.isDirectory()) {
					canHandle = true;
				}
			}
		}
		log.debug("FamIndex HANDLE " + url + "," + canHandle);
		return canHandle;
	}

	private static class test implements JNotifyListener {

		@Override
		public void fileCreated(int i, String string, String string1) {
			log.debug("FileCreated " + i + ", " + string + ", " + string1);
		}

		@Override
		public void fileDeleted(int i, String string, String string1) {
			log.debug("FileDeleted " + i + ", " + string + ", " + string1);
		}

		@Override
		public void fileModified(int i, String string, String string1) {
			log.debug("FileModified " + i + ", " + string + ", " + string1);
		}

		@Override
		public void fileRenamed(int i, String string, String string1, String string2) {
			log.debug("FileRenamed " + i + ", " + string + ", " + string1);
		}
	}

	public class FmlFilesOnlyFilter implements FilenameFilter {

		@Override
		public boolean accept(File dirParameterIgnored, String baseName) {
			if (baseName.length() < 2 || baseName.charAt(0) == '.') {
				return false; // hidden file
			}
			return baseName.toLowerCase().endsWith(".fml");
		}
	}

	@Override
	public void loadInitialRecords() {
		//	throw new UnsupportedOperationException("Not supported yet.");
		// The 
		log.debug("FAM WAS CALLED LOAD INITIAL RECORDS>>>>>********");
		if (indexDir == null) {
			log.warn("no directory to load fml files or to watch");
			return;
		}

		connectJNotify();
		// It's possible JNotify will send messages while we are handling the initial files...
		// so we'll have to synchronize properly for that case...
		File[] files = indexDir.listFiles(new FmlFilesOnlyFilter());
		Arrays.sort(files);
		int counter = 0;
		for (File f : files) {
			Tag_item t = new Tag_item();
			t.processAsRoot(f);
			if (t.wasRead()) {
				if (processItem(t)) {
					counter++;
				}
			}
		}
	}

	public void connectJNotify() {
		// Just find any ".fml" files, sort and add records.
		try {
			// JNotify maps IN_CREATE and IN_MOVED_TO to these constants
			final int mask = JNotify.FILE_CREATED | JNotify.FILE_RENAMED;
			myJNotifyWatchID = JNotify.addWatch(indexDir.getAbsolutePath(), mask, false, new test());
			log.info("JNotify watch added for " + indexDir + " (" + myJNotifyWatchID + ")");
			myJNotifyConnected = true;
		} catch (JNotifyException e) {
			log.error("JNotify error connecting to " + indexDir + ", " + e.toString());
		}

	}

	public void disconnectJNotify() {
		if (myJNotifyConnected) {
			try {
				JNotify.removeWatch(myJNotifyWatchID);
				myJNotifyConnected = false;
				log.info("JNotify watch removed for " + indexDir + " (" + myJNotifyWatchID + ")");
			} catch (JNotifyException e) {
				log.error("JNotify error disconnecting from " + indexDir + ", " + e.toString());
			} finally {
				myJNotifyConnected = false;
				myJNotifyWatchID = -1;
			}
		}
	}

	/**
	 * We need to remove any JNotify listeners
	 */
	@Override
	public void aboutToDispose() {
		disconnectJNotify();
	}
}

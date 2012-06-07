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
	 * meant for prototype factory use only.
	 */
	public FamIndex() {
		super(null, null, null);
		indexDir = null;
		helper = null;
	}

	@Override
	public void update() {
		/*
		 * if (!initComplete) { return; } lastReadSuccess = false;
		 * File[] files = helper.getNewFiles(); boolean allRecordsOK =
		 * true; for (File file : files) { allRecordsOK &=
		 * addRecord(file); } // We'll assume if we get this far and all
		 * the records read in we're OK lastReadSuccess = allRecordsOK;
		 *
		 */
	}

	public FamIndex(URL aURL, Set<IndexRecordListener> listeners)
		throws DataUnavailableException {

		super(aURL, aURL, listeners);
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
		/*
		 * lastReadSuccess = false; this.indexDir = aURL.toString(); try
		 * { if (log.isInfoEnabled()) { log.info("Reading records from "
		 * + path.getAbsolutePath() + " indexLocation=" +
		 * getIndexLocation()); } helper =
		 * FamIndexHelperFactory.newHelper(); saxParser =
		 * SAXParserFactory.newInstance().newSAXParser(); // get files
		 * in directory File[] files =
		 * helper.getInitialFiles(indexDir.getAbsolutePath()); boolean
		 * allRecordsOK = true; for (File file : files) { allRecordsOK
		 * &= addRecord(file); }
		 *
		 * // We'll assume if we get this far and all the records read
		 * in we're OK lastReadSuccess = allRecordsOK; } catch
		 * (Exception e) { log.error("Failed to load index from " +
		 * path, e); throw new DataUnavailableException(e); } // avoid
		 * update() being called before we are complete initComplete =
		 * true;
		 */
	}

	private boolean addRecord(File file) {
		/*
		 * InputStream is = null; boolean success = false; try { is =
		 * new FileInputStream(file); if
		 * (file.getAbsolutePath().endsWith(".gz")) { is = new
		 * GZIPInputStream(is); } saxParser.parse(is, new
		 * SAXIndexHandler(this));
		 *
		 * success = true; } catch (Exception e) { log.warn("Unable to
		 * read " + file, e); } finally { if (is != null) { try {
		 * is.close(); } catch (Exception e2) { // ok } } } return
		 * success;
		 *
		 */
		return true;
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
	private int wd;

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
			return baseName.endsWith(".fml");
		}
	}

	@Override
	public void loadInitialRecords() {
		/*
		//	throw new UnsupportedOperationException("Not supported yet.");
		// The 
		log.debug("FAM WAS CALLED LOAD INITIAL RECORDS>>>>>********");
		if (indexDir == null) {
			log.warn("FAM doesn't have a directory to watch");
			return;
		}

		// Just find any ".fml" files, sort and add records.
		try {
			// JNotify maps IN_CREATE and IN_MOVED_TO to these constants
			final int mask = JNotify.FILE_CREATED | JNotify.FILE_RENAMED;
			log.info("adding watch...");
			this.wd = JNotify.addWatch(indexDir.getAbsolutePath(), mask, false, new test());
			log.info("Successfully created inotify watch for " + indexDir
				+ " wd=" + wd);
		} catch (JNotifyException e) {
			log.error("Make sure that the jnotify jar file and .so are in Tomcat's shared/lib");
			log.error(
				"Otherwise, you could try using WebIndexDirectoryListingDAO instead",
				e);
			throw new UnsupportedOperationException(e);
		}
		// existing files
		//File[] files = new File(indexDir).listFiles(filenamePattern);
		log.info("snagging files...");
		File[] files = indexDir.listFiles(new FmlFilesOnlyFilter());
		Arrays.sort(files);
		//return files;
		* */
	}
}

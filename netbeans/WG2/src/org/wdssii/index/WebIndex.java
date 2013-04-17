package org.wdssii.index;

import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.DataUnavailableException;
import org.wdssii.xml.Tag;
import org.wdssii.xml.index.Tag_item;
import org.wdssii.xml.index.Tag_records;

/**
 * Index that handles the 'webindex' protocol
 *
 * Webindex uses polling to pull xml records from a webserver.
 *
 * @author lakshman
 * @author Robert Toomey
 *
 */
public class WebIndex extends XMLIndex {

	private final static Logger LOG = LoggerFactory.getLogger(WebIndex.class);
	private final URL indexServicePath;
	/**
	 * LastRead value passed in for a single latest record only
	 */
	private final int LATEST_RECORD = -2;
	/**
	 * LastRead value for reading all records
	 */
	private final int READ_ALL_RECORDS = -1;
	/**
	 * A marker telling the webserver the subset of records we want, the
	 * webserver returns a new marker each time we request to let us know
	 * where to start the next time
	 */
	private int lastRead = READ_ALL_RECORDS;

	/**
	 * meant for prototype factory use only.
	 */
	public WebIndex() {
		super(null, null, null);
		indexServicePath = null;
	}

	@Override
	public void update() {
	}

	/**
	 *
	 * @param path e.g: http://nmqwd14:8080/?source=KLTX
	 * @param listeners
	 * @throws DataUnavailableException
	 *
	 * EXAMPLES:
	 * http://nmqwd3.protect.nssl:8080/webindex/getxml.do?source=KCCX&protocol=webindex&lastRead=5586
	 * http://nmqwd3.protect.nssl:8080/KCCX/Reflectivity/01.50/20101008-204958.netcdf.gz
	 * baseURL http://nmqwd3.protect.nssl:8080/
	 *
	 */
	public WebIndex(URL baseURL, URL fullPath, Set<IndexRecordListener> listeners) {
		super(baseURL, baseURL, listeners);
		this.indexServicePath = Index.appendToURL(fullPath, "webindex/getxml.do", true);
	}

	/**
	 * To get the current XML read URL, we have to append the lastRead
	 * parameter
	 */
	@Override
	public URL getXMLReadURL() {
		URL aURL = null;
		try {
			aURL = Index.appendQuery(indexServicePath, "lastRead=" + lastRead);
		} catch (Exception e) {
			LOG.error("Couldn't create url to read xml records with :(");
		}
		return aURL;
	}

	/**
	 * @param listeners
	 * @throws DataUnavailableException
	 */
	@Override
	public Index newInstance(URL aURL, URL fullURL, TreeMap<String, String> params, Set<IndexRecordListener> listeners)
		throws DataUnavailableException {

		// Try to add the source to the URL path..this is the base path for the IndexRecord, all
		// requests for data will append this.
		// http://tensor.protect.nssl:8080/ ---->
		// http://tensor.protect.nssl:8080/KTLX/
		URL baseURL = aURL;
		try {
			baseURL = new URL(aURL.toString() + params.get("source"));
		} catch (Exception e) {
			LOG.error("Webindex URL failed " + aURL);
		}

		return new WebIndex(baseURL, fullURL, listeners);
	}

	@Override
	public boolean checkURL(String protocol, URL url, URL fullurl, TreeMap<String, String> paramMap) {
		boolean valid = false;
		boolean tryIt = false;

		// We will check if it is 'xml' or null (default)
		if (protocol != null) {
			if (protocol.equalsIgnoreCase("webindex")) {

				// Check top of xml at url (web or local)
				try {
					// Just ask for one record...
					URL a = Index.appendToURL(fullurl, "webindex/getxml.do", true);
					URL b = Index.appendQuery(a, "lastRead=" + this.LATEST_RECORD);
					Tag_records t = new Tag_records();
					t.setProcessChildren(false); // don't process <item>, etc...
					t.processAsRoot(b);
					if (t.wasRead()) {  // If we found a <codeindex> tag, good enough we think
						valid = true;
					}
				} catch (Exception c) {
					valid = false;
				}
			}
		}
		LOG.error("WebIndex HANDLE " + url + "," + valid);
		return valid;
	}

	/**
	 * Get the items for a given Tag. Webindex uses a Tag_records
	 *
	 * @param t
	 * @return
	 */
	@Override
	protected List<Tag_item> getItems(Tag t) {
		if (t instanceof Tag_records) {
			Tag_records c = (Tag_records) (t);
			return c.items;
		}
		return null;
	}

	/**
	 * Load the initial records (all current)
	 */
	@Override
	public void loadInitialRecords() {
		Tag_records root = new Tag_records();
		int counter = processRoot(root);
		if (counter >= 0) {
			lastRead = root.lastRead;
		}
	}
}

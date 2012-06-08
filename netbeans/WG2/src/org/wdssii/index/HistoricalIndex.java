package org.wdssii.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.wdssii.index.IndexSubType.SubtypeType;

/**
 * A wrapper to an index that stores a certain number of records in a history
 * for browsing, either by the GUI or others
 *
 * FIXME: Could extend this to handle a collection of Index, instead of just
 * one. The advantage would be that you could control size of the group history.
 * FIXME: Total record counts should be by each datatype, not the whole index.
 * Otherwise a large number of datatypes 'pushes out' data.
 *
 * @author Robert Toomey
 *
 */
public class HistoricalIndex implements IndexRecordListener {

	/**
	 * The size of our history
	 */
	private int maxHistorySize = 0;
	/**
	 * The current count of all records we have.
	 */
	private int currentHistorySize = 0;
	/**
	 * The total history we've ever seen
	 */
	private int totalHistorySize = 0;
	/**
	 * Fire events on add/delete. Turned off when first reading index to
	 * avoid flooding
	 */
	private boolean fireEvents = false;
	/**
	 * The index we watch
	 */
	private Index index = null;
	/**
	 * Collection of listeners for responding to records
	 */
	private Set<HistoryListener> listeners = new TreeSet<HistoryListener>();
	/**
	 * Map from internal datatype key to the IndexDataType storing the
	 * records
	 */
	private TreeMap<String, IndexDataType> datatypeStringToInt = new TreeMap<String, IndexDataType>();
	/**
	 * Set of datatype names to short strings, used to save memory on keys.
	 */
	private ArrayList<String> datatypeIntToString = new ArrayList<String>();

	/**
	 * Query result to gather a subset of records. The GUI uses this to pass
	 * in parameters and get a subset of records from the selections a user
	 * makes. You can call this for any purpose though.
	 */
	public static class RecordQuery {

		/**
		 * A set of the unique subtypes found in the query
		 */
		public Set<String> uniqueSubtypes = new TreeSet<String>();
		/**
		 * A set of the unique datatypes found in the query
		 */
		public Set<String> uniqueDatatypes = new TreeSet<String>();
		/**
		 * The records matching the query
		 */
		public ArrayList<IndexRecord> matches = new ArrayList<IndexRecord>();
	}

	/**
	 * Create a historical index given a path (e.g: /tmp/code_index.xml) and
	 * a maximum history in record count.
	 */
	public HistoricalIndex(String path, int aHistorySize) {
		maxHistorySize = aHistorySize;
		index = IndexFactory.createIndex(path, this);
		fireEvents = true;
	}

	/**
	 * Get the index we wrap around
	 */
	public Index getIndex() {
		return index;
	}

	/**
	 * Get the current history size in records
	 */
	public int getCurrentHistorySize() {
		return currentHistorySize;
	}

	/**
	 * Get the maximum history size in records
	 */
	public int getMaxHistorySize() {
		return maxHistorySize;
	}

	/**
	 * Get the total history size (every record ever received
	 */
	public int getTotalHistorySize() {
		return totalHistorySize;
	}

	/**
	 * Add a datatype to index, keeping reference count. Called for each new
	 * record
	 */
	private IndexDataType addDatatype(String datatypeName) {
		IndexDataType info = datatypeStringToInt.get(datatypeName);
		if (info == null) {
			info = new IndexDataType(datatypeIntToString.size());
			datatypeIntToString.add(datatypeName);
			datatypeStringToInt.put(datatypeName, info);
		}
		return info;
	}

	/**
	 * Given a long name such as "Reflectivity", get the IndexDataType that
	 * stores all the subtypes for it
	 *
	 * @param datatypeName	DataType name such as "Reflectivity"
	 * @return	The IndexDataType for given name
	 */
	private IndexDataType getDataTypeInfo(String datatypeName) {
		return (datatypeStringToInt.get(datatypeName));
	}

	/**
	 * Register a listener to be notified about record changes in this
	 * history
	 */
	public void addHistoryListener(HistoryListener listener) {
		listeners.add(listener);
	}

	/**
	 * Handle records from the index by storing them into our history
	 */
	@Override
	public void handleRecord(IndexRecord rec) {

		// First, trim the history to make room for this record
		if (currentHistorySize + 1 > maxHistorySize) {
			removeOldestRecords();
		}
		addRecord(rec);
	}

	/**
	 * Iterator for handling IndexDataTypes
	 */
	public Iterator<Entry<String, IndexDataType>> getIndexDataTypeIterator() {
		Set<Entry<String, IndexDataType>> set = datatypeStringToInt.entrySet();
		Iterator<Entry<String, IndexDataType>> i = set.iterator();
		return i;
	}

	/**
	 * Add a new record to our history
	 */
	public void addRecord(IndexRecord rec) {

		// Add record 
		String dataType = rec.getDataType();
		IndexDataType info = addDatatype(dataType);
		info.addRecord(rec);
		currentHistorySize++;
		totalHistorySize++;

		// Let all listeners handle the new record added (algs or display autoupdate)
		if (fireEvents) {
			for (HistoryListener listener : listeners) {
				listener.recordAdded(rec);
			}
		}
	}

	/**
	 * Remove the oldest records to make room for new ones
	 */
	private void removeOldestRecords() {
		if (currentHistorySize + 1 > maxHistorySize) {

			if (currentHistorySize > 0) {

				// Find 'an' oldest record.  Note that there can be more than one record
				// with the same oldest time.  (Such as scale_1, scale_2 Windfield where subtype is a mode)
				// We just delete one of them in this case
				IndexRecord candidate = null;
				IndexDataType deleteFromDatatype = null;
				Iterator<Entry<String, IndexDataType>> iter = getIndexDataTypeIterator();
				while (iter.hasNext()) {
					Entry<String, IndexDataType> current = iter.next();
					IndexDataType datatype = current.getValue();
					IndexRecord oneOfOldest = datatype.getFirstRecordByTime();
					if (candidate == null) {
						candidate = oneOfOldest;
						deleteFromDatatype = datatype;
					} else {
						if (oneOfOldest != null) {
							if (oneOfOldest.getTime().before(candidate.getTime())) {
								candidate = oneOfOldest;
								deleteFromDatatype = datatype;
							}
						}
					}

				}

				// Now try to delete the record
				if (deleteFromDatatype != null) {
					boolean success = deleteFromDatatype.removeRecord(candidate);
					if (success) {
						currentHistorySize--;

						// Let all listeners handle the new record added (algs or display autoupdate)
						if (fireEvents) {
							for (HistoryListener listener : listeners) {
								listener.recordDeleted(candidate);
							}
						}
					}
				}
			}
		}
	}

	public Map<Date, IndexRecord> getRecordsByTypeTime(String longDt, String subtype, Date from,
		Date to) {
		IndexDataType type = getDataTypeInfo(longDt);
		if (type == null) {
			return null;
		}
		Map<Date, IndexRecord> holdme = null;
		holdme = type.getRecordsByTime(subtype, from, to);
		return holdme;
	}

	/**
	 * This gets all records for a data type. It doesn't return a Map<Date,
	 * IndexRecord> because there can be the same Date stored in multiple
	 * subtypes (such as WindField) In general, you'll want to use
	 * getRecordsByTypeTime (above) which gives you a unique Date per
	 * record, but for a single DataType/SubType.
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public ArrayList<IndexRecord> getRecordsByTime(String longDt, Date from, Date to) {

		// Buffer to store result records into
		ArrayList<IndexRecord> theResults = null;

		IndexDataType type = getDataTypeInfo(longDt);
		if (type != null) {
			theResults = new ArrayList<IndexRecord>();
			type.getRecordsByTime(from, to, theResults);
		}
		return theResults;
	}

	public IndexRecord getFirstRecordByTime(String longDt, String subtype) {
		IndexDataType type = getDataTypeInfo(longDt);
		if (type == null) {
			return null;
		}
		return (type.getFirstRecordByTime(subtype));
	}

	/**
	 * Get latest record by time for all subtypes
	 *
	 * @param longDt
	 * @return
	 */
	public IndexRecord getLastRecordByTime(String longDt) {
		IndexDataType dataType = getDataTypeInfo(longDt);
		if (dataType != null) {
			return (dataType.getLastRecordByTime());
		}
		return null;
	}

	/**
	 * Get latest time record, forcing subtype to stay the same
	 */
	public IndexRecord getLastRecordByTime(String longDt, String subtype) {
		if (subtype == null || subtype.length() == 0) {
			return null; // We have to keep current subtype for GUI
		}
		IndexDataType dataType = getDataTypeInfo(longDt);
		if (dataType != null) {
			return (dataType.getLastRecordByTime(subtype));
		}
		return null;
	}

	/**
	 * @return the index record <= the given date, trying to match subtype.
	 * Used by display to synchronize products such as
	 * Reflectivity/Velocity. You pass in "Reflectivity" "3.5" and 3 pm..and
	 * get the closest record up to and including 3 pm.
	 */
	public IndexRecord getLatestUpToDate(String longDt, String subtype, Date d) {
		IndexDataType type = getDataTypeInfo(longDt);
		if (type == null) {
			return null;
		}

		return type.getLatestUpToDate(subtype, d);
	}

	/**
	 * @param longDt the DataType name
	 * @return Number of records stored in this DataType, or -1 if no
	 * DataType exists
	 */
	public int numRecordsByTime(String longDt) {
		IndexDataType type = getDataTypeInfo(longDt);
		if (type == null) {
			return -1;
		}
		return (type.getRecordCount());
	}

	/**
	 * Get record from index given datatype, subtype and time
	 */
	public IndexRecord getRecord(String datatype, String subtype, Date time) {
		if (datatype != null) {
			IndexDataType type = getDataTypeInfo(datatype);
			if (type != null) {
				return (type.getSubtypeTime(subtype, time));
			}
		}
		return null;

	}

	/**
	 * Get the datatype list for this index
	 */
	public TreeSet<String> getDataTypes() {

		// Copy to set for backward compatibility.
		TreeSet<String> output = new TreeSet<String>();
		for (String name : datatypeIntToString) {
			output.add(name);
		}
		return output;
	}

	/**
	 * Get the subtype list for a given datatype
	 */
	public TreeSet<String> getSubTypesForDataType(String longDt) {
		IndexDataType info = datatypeStringToInt.get(longDt);
		if (info == null) {
			return null;
		}
		return (info.getSubtypes());
	}

	/**
	 * Get a list of the sorted subtypes for a datatype 'product'
	 *
	 * @param longDt Datatype to get such as "Reflectivity"
	 * @param ascending Which way to sort
	 */
	public ArrayList<String> getSortedSubTypesForDataType(
		String longDt, boolean ascending) {

		ArrayList<String> strings = new ArrayList<String>();

		// Try it from index directly
		TreeSet<String> subtypeList = getSubTypesForDataType(longDt);
		if (subtypeList != null) {
			for (String s : subtypeList) {
				strings.add(s);
			}

			// Sort the strings (though index should return them sorted already now)
			if (ascending) {
				Collections.sort(strings);
			} else {
				Collections.sort(strings, new Comparator<String>() {

					@Override
					public int compare(String o1, String o2) {
						return o2.compareTo(o1);
					}
				});
			}
		}

		return strings;
	}

	public enum Direction {

		PreviousSubType, // Move to previous time < current (if subtype numeric)
		// Move to previous subtype, find closest time <= (if subtype non-numeric)
		NextSubType, // Move to next time > current (if subtype numeric)
		// Move to next subtype, find closest time <= (if subtype non-numeric)
		PreviousTime, // Back in time, keep subtype the same	
		NextTime, // Forward in time, keep subtype the same
		LatestTime, // Latest time, keep subtype the same	
		LatestAllSubTypes;                   // Latest time record, ignore subtype
	}

	/**
	 * Navigate starting from this record.
	 *
	 * @return null if no record was found in that direction
	 */
	public IndexRecord getNextRecord(IndexRecord start, Direction direction) {

		IndexRecord next = null;
		if (start != null) {

			// Get datatype
			IndexDataType dataType = getDataTypeInfo(start.getDataType());
			if (dataType != null) {

				switch (direction) {  // FIXME: make these use real date, not timestamp (save some memory)
					case PreviousTime:	// Easy, just previous time record keeping subtype the same
						//next = dataType.getPreviousTimeRecord(start.getSubType(), start.getTimeStamp());
						next = dataType.getPreviousTimeRecord(start.getSubType(), start.getTime());
						break;
					case NextTime:		// Easy, just next time record keeping subtype the same
						next = dataType.getNextTimeRecord(start.getSubType(), start.getTime());
						break;
					case PreviousSubType: {
						SubtypeType mode = IndexSubType.getSubtypeType(start.getSubType());
						switch (mode) {
							case ELEVATION:
								next = dataType.getPreviousElevation(start.getSubType(), start.getTime());
								break;
							case MODE_SELECTION:
								next = dataType.getPreviousMode(start.getSubType(), start.getTime());
								break;
						}
					}
					break;
					case NextSubType: {
						SubtypeType mode = IndexSubType.getSubtypeType(start.getSubType());
						switch (mode) {
							case ELEVATION: {
								next = dataType.getNextElevation(start.getSubType(), start.getTime());
							}
							break;
							case MODE_SELECTION:
								next = dataType.getNextMode(start.getSubType(), start.getTime());
								break;
						}
					}
					break;
					case LatestTime: { // Public record takes full datatype
						next = getLastRecordByTime(start.getDataType(), start.getSubType());
					}
					break;
					case LatestAllSubTypes: {  // Public record takes full datatype
						next = getLastRecordByTime(start.getDataType());
					}
					break;
					default:
						break;
				}
			}
		}
		return next;
	}

	/**
	 * Get a sorted result list of index records. The upto is a set of
	 * strings telling the selection. For now, these come from the GUI from
	 * the product picker FIXME: Put some examples here in the documentation
	 */
	public RecordQuery gatherRecords(
		String[] upto, boolean ascending) {

		RecordQuery results = new RecordQuery();

		Iterator<IndexRecord> iter = null;
		Map<Date, IndexRecord> recordsMap;
		ArrayList<IndexRecord> recordsList;
		if (upto[1].compareTo("*") == 0) {
			recordsList = getRecordsByTime(upto[0], null, null);
			if (recordsList != null) {
				iter = recordsList.iterator();
			}
		} else {
			// They are sorted if only one subtype (there can't be dups)
			recordsMap = getRecordsByTypeTime(
				upto[0], upto[1], null, null);
			if (recordsMap != null) {
				iter = recordsMap.values().iterator();
			}
		}

		// Create a new string list of data items (lazy)
		// We want to 'filter' each index record and get a set of all
		// the second selections...
		int length = upto.length;
		if (iter != null) {
			while (iter.hasNext()) {
				IndexRecord aRecord = iter.next();
				String datatype = aRecord.getDataType();
				String subtype = aRecord.getSubType();
				//String timestamp = aRecord.getTimeStamp();
				//Date aTime = aRecord.getTime();

				// Match each key part to each field of selection
				boolean match = true;

				// 'Elevation' match record
				if (length > 1) {
					if (!(upto[1].equals("*"))) {
						if (!(upto[1].equals(subtype))) {
							match = false;
						}
					}
				}

				// Add to query results
				if (match) {
					results.uniqueSubtypes.add(subtype);
					results.uniqueDatatypes.add(datatype);
					results.matches.add(aRecord);
				}
			}
		}

		sortRecordsByTime(results.matches, ascending);

		if (ascending) {
			// Sort anagram groups according to size
			Collections.sort(results.matches, new Comparator<IndexRecord>() {

				@Override
				public int compare(IndexRecord o1, IndexRecord o2) {
					return o1.getTime().compareTo(o2.getTime());
				}
			});
		} else {
			// Sort anagram groups according to size
			Collections.sort(results.matches, new Comparator<IndexRecord>() {

				@Override
				public int compare(IndexRecord o1, IndexRecord o2) {
					return o2.getTime().compareTo(o1.getTime());
				}
			});
		}

		return results;
	}

	/**
	 * Sort a collection of IndexRecords by ascending or descending time
	 */
	public static void sortRecordsByTime(ArrayList<IndexRecord> matches, boolean ascending) {
		if (matches != null) {
			if (ascending) {
				// Sort anagram groups according to size
				Collections.sort(matches, new Comparator<IndexRecord>() {

					@Override
					public int compare(IndexRecord o1, IndexRecord o2) {
						return o1.getTime().compareTo(o2.getTime());
					}
				});
			} else {
				// Sort anagram groups according to size
				Collections.sort(matches, new Comparator<IndexRecord>() {

					@Override
					public int compare(IndexRecord o1, IndexRecord o2) {
						return o2.getTime().compareTo(o1.getTime());
					}
				});
			}
		}
	}

	public IndexRecord getPreviousLowestRecord(
		IndexRecord current) {
		IndexRecord newRecord = null;

		// newRecord = theIndex.getNextRecord(current, direction);
		// ----- THIS might be new direction in index...but then again, we
		// need to
		// find 'base' by comparing the subtype string. Something like that
		// should
		// be overridable by Product.

		IndexRecord iter = current;
		IndexRecord previous;
		boolean found = false;
		String subtype;

		subtype = current.getSubType();
		// System.out.println("Current subtype is "+subtype);
		if (subtype == null) {
			return null;
		}

		String currsubtype = subtype;
		String prevsubtype = null;

		// The 'base' logic. FIXME: this is flawed.
		// Hunt previous records until we get a subtype above us
		do {
			previous = getNextRecord(iter,
				Direction.PreviousSubType);
			if (previous != null) {
				prevsubtype = previous.getSubType();
				// System.out.println("Comparing "+prevsubtype+" to "+subtype);
				// System.out.format("----->%d",
				// prevsubtype.compareTo(subtype));
				if (prevsubtype.compareTo(subtype) >= 0) { // THIS SHOULD BE
					// BY PRODUCT
					break;
				}
			}

			found = true;
			if (prevsubtype != null) {
				subtype = prevsubtype;
			}
			iter = previous;
		} while (found);

		if (found && (!currsubtype.equals(subtype))) {
			newRecord = iter;
		}
		// ----- END NEW DIRECTION?

		//
		return newRecord;
	}

	/** A clean up message to send when getting rid of */
	public void aboutToDispose(){
		if (index != null){
			index.aboutToDispose();
		}
	}
}



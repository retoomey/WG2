package org.wdssii.index;

import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * the IndexSubType class handles a collection of records for a particular SubType.
 * They are stored by Date.
 * A IndexSubType belongs to a IndexDataType
 * 
 * @author Robert Toomey
 * 
 */
public class IndexSubType {

    /**
     * Key of this subtype, such as '0.50'
     */
    protected String subtypeKey;
    /** Maintain a Date --> IndexRecord lookup */
    private TreeMap<Date, IndexRecord> dateToRecord = new TreeMap<Date, IndexRecord>();

    /**
     * The type of subtype we have.  We don't store it, the static function getSubtypeType
     * takes a subtype and returns the type.  This is used to determine how the up/down subtype
     * and volumes work.
     * ELEVATION  is 00.50, etc.  We assume part of a linear time volume
     * MODE_SELECTION is say scale_0 for WindField.  We assume the subtypes are independent
     */
    public static enum SubtypeType {

        ELEVATION,
        MODE_SELECTION
    }

    /**
     * This is the test of what type the subtype is.  If it's a float like "00.50" we assume it's
     * an elevation.  Otherwise for now, it's a mode picker.
     * 
     * @param subtype the subtype key to test
     * @return a SubtypeType telling which kind of subtype this is
     */
    public static SubtypeType getSubtypeType(String subtype) {
        try {
            Float.parseFloat(subtype);
            return SubtypeType.ELEVATION;
        } catch (NumberFormatException e) {
            // Ok it's not a float...
        }
        return SubtypeType.MODE_SELECTION;
    }

    /** Create an IndexSubType with given key name */
    public IndexSubType(String key) {
        subtypeKey = key;
    }

    /** Add a record to this IndexSubType */
    protected void addRecord(IndexRecord rec) {
        // Optional: check that rec subtype matches ours, or complain.
        // We ASSUME that for a given datatype/subtype that the time is a UNIQUE key.
        dateToRecord.put(rec.getTime(), rec);
    }

    /** Get the number of records we are currently storing */
    public int getNumberOfRecords() {
        return dateToRecord.size();
    }

    // Time access functions
    /**
     * Get a time range from the DataType
     * note that from is INCLUSIVE and to is EXCLUSIVE
     * 
     * We can have the same time for two different subtypes (as in Windfield),
     * so this version of the records by time in in the SUBTYPE
     */
    public Map<Date, IndexRecord> getRecordsByTime(Date from, Date to) {

        SortedMap<Date, IndexRecord> sm = null;
        if (from != null) {
            if (to != null) {
                sm = dateToRecord.subMap(from, to);
            } else {
                sm = dateToRecord.tailMap(from);
            }
        } else {
            if (to != null) {
                sm = dateToRecord.headMap(to);
            } else {
                sm = dateToRecord;
            }
        }
        return (sm);
    }

    /**
     * 
     * @param time	date of record to get
     * @return the unique record with this time in the subtype.
     */
    protected IndexRecord getRecordByTime(Date time) {
        if (time != null) {
            return (dateToRecord.get(time));
        }
        return null;
    }

    /** First record by time for this IndexSubType */
    public IndexRecord getFirstRecordByTime() {
        IndexRecord candidate = null;
        Entry<Date, IndexRecord> entry = dateToRecord.firstEntry();
        if (entry != null) {
            candidate = entry.getValue();
        }
        return candidate;
    }

    /** Last record by time for this IndexSupType */
    public IndexRecord getLastRecordByTime() {
        IndexRecord candidate = null;
        Entry<Date, IndexRecord> entry = dateToRecord.lastEntry();
        if (entry != null) {
            candidate = entry.getValue();
        }
        return candidate;
    }

    /** Get latest time record up to and including a given date 
     * So you pass in 3 pm, and you get the closest record up to and including 3 pm.
     * For this subtype
     */
    public IndexRecord getLatestRecordByDate(Date d) {
        IndexRecord latest = null;
        Date aDate = dateToRecord.floorKey(d);
        if (aDate != null) {
            latest = dateToRecord.get(aDate);
        }
        return latest;
    }

    ;

	/** 
	 * @return the index record <= the given date for this IndexSubType
	 * Used by display to synchronize products such as Reflectivity/Velocity.
	 */
	public IndexRecord getLatestUpToDate(Date d) {
        IndexRecord candidate = null;
        Entry<Date, IndexRecord> entry = dateToRecord.floorEntry(d);
        if (entry != null) {
            candidate = entry.getValue();
        }
        return candidate;
    }

    /** Get the previous record in time for a given and time */
    public IndexRecord getPreviousTimeRecord(Date d) {
        IndexRecord candidate = null;
        Entry<Date, IndexRecord> entry = dateToRecord.lowerEntry(d);
        if (entry != null) {
            candidate = entry.getValue();
        }
        return candidate;
    }

    /** Get the next record in time for a given and time */
    public IndexRecord getNextTimeRecord(Date d) {
        IndexRecord candidate = null;
        Entry<Date, IndexRecord> entry = dateToRecord.higherEntry(d);
        if (entry != null) {
            candidate = entry.getValue();
        }
        return candidate;
    }

    /** Get the ceiling record in time for a given and time */
    public IndexRecord getAtLeastTimeRecord(Date d) {
        IndexRecord candidate = null;
        Entry<Date, IndexRecord> entry = dateToRecord.ceilingEntry(d);
        if (entry != null) {
            candidate = entry.getValue();
        }
        return candidate;
    }

    /** Remove a record if found within us.  Return true if found */
    public boolean removeRecord(IndexRecord rec) {
        IndexRecord removed = dateToRecord.remove(rec.getTime());
        return (removed != null);
    }
}

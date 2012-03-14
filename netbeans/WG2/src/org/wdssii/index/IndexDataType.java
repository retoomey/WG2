package org.wdssii.index;

import java.util.Map.Entry;
import java.util.*;

/**
 * the IndexDataType class handles a collection of records for a particular
 * DataType. A IndexDataType belongs to an Index
 *
 * @see Index
 *
 * @author REST
 *
 */
public class IndexDataType {

    /**
     * Do we delete subtypes when they are empty from deletion? The default is
     * true for now
     */
    private boolean deleteEmptySubtypes = true;
    /**
     * How many records do we currently hold?
     */
    private int reference;
    // Maintain a list of subtypes per DataType
    // Purpose: to get the string list of available subtypes for DataType
    private TreeSet<String> subtypes = new TreeSet<String>();
    // Subtype storage.  Currently subtype string to info (could make it a token)
    private TreeMap<String, IndexSubType> indexSubTypes = new TreeMap<String, IndexSubType>();

    /**
     * Create a DataTypeInfo with given id. The index assigns a unique number to
     * each DataType
     */
    public IndexDataType(int id) {
        reference = 0;		// The number of records with this datatype in the index
    }

    /**
     * Add a (date --> rec) mapping for our subtype
     *
     * @param rec	the record to add
     */
    protected void addRecord(IndexRecord rec) {
        String subtype = rec.getSubType();

        // Convenience set..could just get from subtypeInfo directly?
        subtypes.add(subtype);

        // Add to subtypeInfos...
        IndexSubType info = indexSubTypes.get(subtype);
        if (info == null) {
            info = new IndexSubType(rec.getSubType());
            indexSubTypes.put(rec.getSubType(), info);
        }
        info.addRecord(rec);
        reference++;
    }

    /**
     * Remove given record from IndexDataType, return true if found.
     */
    protected boolean removeRecord(IndexRecord rec) {
        Iterator<Entry<String, IndexSubType>> i = getSubtypeInfoIterator();
        ArrayList<String> deleteList = new ArrayList<String>();
        boolean found = false;
        while (i.hasNext()) {
            Entry<String, IndexSubType> current = i.next();
            IndexSubType theSubtype = current.getValue();
            boolean success = theSubtype.removeRecord(rec);

            // Add any subtypes holding record to delete list
            if (deleteEmptySubtypes && (theSubtype.getNumberOfRecords() < 1)) {
                deleteList.add(current.getKey());
            }
            if (success) {
                reference--;
                found = true;
                // Each record has a unique subtype, so should be only one.
                // If this changes, remove this break;
                break;
            }
        }

        // Delete those subtypes marked for deletion
        for (String deleteMe : deleteList) {
            subtypes.remove(deleteMe);
            indexSubTypes.remove(deleteMe);
        }
        return found;
    }

    /**
     * Iterator for handling subtypes
     */
    public Iterator<Entry<String, IndexSubType>> getSubtypeInfoIterator() {
        Set<Entry<String, IndexSubType>> set = indexSubTypes.entrySet();
        Iterator<Entry<String, IndexSubType>> i = set.iterator();
        return i;
    }

    /**
     * Get the subtypes for this DataType
     */
    protected TreeSet<String> getSubtypes() {
        // FIXME: Might just iterate over subtype info instead?
        //return (subtypeInfos.keySet());
        return subtypes;
    }

    /**
     * Get record with given subtype and time
     */
    public IndexRecord getSubtypeTime(
            String subtype,
            Date time) {
        IndexSubType info = indexSubTypes.get(subtype);
        IndexRecord aRecord = null;
        if (info != null) {
            aRecord = info.getRecordByTime(time); // null allowed
        }
        return aRecord;
    }

    /**
     * The number of records we hold
     */
    public int getRecordCount() {
        return reference;
    }

    /**
     * Return the records in time for a given subtype
     */
    public Map<Date, IndexRecord> getRecordsByTime(String subtype, Date from, Date to) {
        IndexSubType info = indexSubTypes.get(subtype);
        if (info != null) {
            return (info.getRecordsByTime(from, to));
        }
        return null;
    }

    /**
     * Get the records in time for all subtypes. Since subtype can be a mode
     * selection for some DataType, such as WindField, there can be more than
     * one record with the same time. Usually, you'd use getRecordsByTime above.
     * The GUI uses this for its '*' pattern match
     */
    public void getRecordsByTime(Date from, Date to, ArrayList<IndexRecord> output) {

        // For each of our subtypeInfos (we have a forest of red-black time trees)
        for (String s : this.subtypes) {
            IndexSubType theSubtype = indexSubTypes.get(s);
            // This is fairly cheap, just a subset marked of the red-black tree
            Map<Date, IndexRecord> records = theSubtype.getRecordsByTime(from, to);
            if (records != null) {
                // Linear copy of records
                for (Iterator<IndexRecord> it = records.values().iterator(); it.hasNext();) {
                    output.add(it.next());
                }

            }
        }
    }

    public IndexRecord getFirstRecordByTime(String subtype) {
        IndexSubType info = indexSubTypes.get(subtype);
        if (info != null) {
            return (info.getFirstRecordByTime());
        }
        return null;
    }

    public IndexRecord getLatestUpToDate(String subtype, Date d) {
        IndexSubType info = indexSubTypes.get(subtype);
        if (info != null) {
            return (info.getLatestUpToDate(d));
        }
        return null;
    }

    /**
     * Get the previous record in time for a given subtype and time
     *
     * @param subtype
     * @param timeStamp
     * @return
     */
    public IndexRecord getPreviousTimeRecord(String subtype, Date time) {
        IndexRecord previous = null;
        IndexSubType theSubtype = indexSubTypes.get(subtype);
        if (theSubtype != null) {
            previous = theSubtype.getPreviousTimeRecord(time);
        }
        return previous;
    }

    /**
     * Get the previous record in time for a given subtype and timestamp
     *
     * @param subtype
     * @param timeStamp
     * @return
     */
    public IndexRecord getNextTimeRecord(String subtype, Date time) {
        IndexRecord previous = null;
        IndexSubType theSubtype = indexSubTypes.get(subtype);
        if (theSubtype != null) {
            previous = theSubtype.getNextTimeRecord(time);
        }
        return previous;
    }

    /**
     * Get previous elevation (this algorithm is used when the subtype is an
     * elevation) Elevation assumes that subtypes are time ordered in a circular
     * pattern. This is the 'down' button when using elevation based products
     *
     * @param elevation
     * @param time
     * @return
     */
    public IndexRecord getPreviousElevation(String elevation, Date startTime) {
        return (getElevation(elevation, startTime, false));
    }

    /**
     * Get next elevation (this algorithm is used when the subtype is an
     * elevation) Elevation assumes that subtypes are time ordered in a circular
     * pattern. This is the 'up' button when using elevation based products
     *
     * @param elevation
     * @param time
     * @return
     */
    public IndexRecord getNextElevation(String elevation, Date startTime) {
        return (getElevation(elevation, startTime, true));
    }

    /**
     * Worker for the above functions
     *
     * @param elevation
     * @param startTime
     * @param up
     * @return
     */
    private IndexRecord getElevation(String elevation, Date startTime, boolean up) {
        IndexRecord rec = null;

        // Get next/prev subtype with circular roll
        Entry<String, IndexSubType> borderSubtype;
        if (up){
            borderSubtype = indexSubTypes.higherEntry(elevation);
            if (borderSubtype == null){ 
                borderSubtype = indexSubTypes.firstEntry();
            }
        }else{
            borderSubtype = indexSubTypes.lowerEntry(elevation);
            if (borderSubtype == null){
                borderSubtype = indexSubTypes.lastEntry();
            }
        }
        
        // Get the record with time <=/>= to given....
        IndexSubType theSubtype = borderSubtype.getValue();
        IndexRecord candidateRecord = (up == true)
                        ? theSubtype.getAtLeastTimeRecord(startTime)
                        : theSubtype.getLatestUpToDate(startTime);
        return candidateRecord;
    }

    /**
     * Called for 'down' arrow for a subtype based on product mode, such as
     * Windfield. This ALWAYS moves subtype, and gets the time <= (Treat subtype
     * as its own product)
     *

     *
     * @param subType
     * @param time
     * @return
     */
    public IndexRecord getPreviousMode(String subType, Date time) {

        // In previous mode, we force the subtype FIRST, then find the closest time <= given
        // Notice this will not 'roll' subtypes like with elevations.
        IndexRecord previous = null;
        String previousSubtypeKey = subtypes.lower(subType);
        if (previousSubtypeKey != null) {
            IndexSubType theSubtype = indexSubTypes.get(previousSubtypeKey);
            previous = theSubtype.getPreviousTimeRecord(time);
        }
        return previous;
    }

    /**
     * Called for 'up' arrow for a subtype based on product mode, such as
     * Windfield. This ALWAYS moves subtype, and gets the time >= (Treat subtype
     * as its own product)
     *
     * @param subType
     * @param time
     * @return
     */
    public IndexRecord getNextMode(String subType, Date time) {

        // In previous mode, we force the subtype FIRST, then find the closest time >= given
        // Notice this will not 'roll' subtypes like with elevations.
        IndexRecord previous = null;
        String previousSubtypeKey = subtypes.higher(subType);
        if (previousSubtypeKey != null) {
            IndexSubType theSubtype = indexSubTypes.get(previousSubtypeKey);
            previous = theSubtype.getAtLeastTimeRecord(time);
        }
        return previous;
    }

    /**
     * Get last record by time for given subtype
     */
    public IndexRecord getLastRecordByTime(String subtype) {

        IndexSubType theSubtype = indexSubTypes.get(subtype);
        if (theSubtype != null) {
            return theSubtype.getLastRecordByTime();
        }
        return null;
    }

    /**
     * Get last record by time for all subtypes Note this algorithm assumes
     * there is a unique time record for all subtypes, otherwise the results are
     * undefined if exists t1, t2 where t1 == t2 (This happens in Windfield
     * 'mode' subtype, where you have scale_1, scale_2 each with the same time
     * in the record. You just get one of those records then)
     */
    public IndexRecord getLastRecordByTime() {
        IndexRecord last = null;
        for (String s : this.subtypes) {
            IndexSubType theSubtype = indexSubTypes.get(s);
            IndexRecord candidate = theSubtype.getLastRecordByTime();
            if (last == null) {
                last = candidate;
            } else {
                if (candidate != null) {
                    if (candidate.getTime().compareTo(last.getTime()) > 0) {
                        last = candidate;
                    }
                }
            }
        }
        return last;
    }

    /**
     * Get first record by time for all subtypes Note this algorithm assumes
     * there is a unique time record for all subtypes, otherwise the results are
     * undefined if exists t1, t2 where t1 == t2 (This happens in Windfield
     * 'mode' subtype, where you have scale_1, scale_2 each with the same time
     * in the record. You just get one of those records then)
     */
    public IndexRecord getFirstRecordByTime() {
        IndexRecord last = null;
        for (String s : this.subtypes) {
            IndexSubType theSubtype = indexSubTypes.get(s);
            IndexRecord candidate = theSubtype.getFirstRecordByTime();
            if (last == null) {
                last = candidate;
            } else {
                if (candidate != null) {
                    if (candidate.getTime().compareTo(last.getTime()) < 0) {
                        last = candidate;
                    }
                }
            }
        }
        return last;
    }
}

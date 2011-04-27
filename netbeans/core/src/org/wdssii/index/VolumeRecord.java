package org.wdssii.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.wdssii.index.HistoricalIndex.Direction;

/** A circular buffer of records making up a (optional virtual) volume of IndexRecord
 * There is a current position in the volume, allowing navigation and peeking in an 'up' or 'down'
 * subtype direction.  Two static functions return a virtual or 'regular' VolumeRecord
 *
 * Basically, given an index and a record, finds the set of records making up the volume
 * 
 * Implements Iterable, so you can access records easily this way:
 * for (IndexRecord r: myVolumeRecord)
 * 
 * The comparator passed into static functions allows you to control how volumes are
 * created/sorted.
 * 
 * <code>
 *
 * VolumeRecord v = VolumeRecord.getVolumeRecord(index, aRecord, null);
 * for (IndexRecord r: v){
 * 	// do something with this volume record
 * }
 * </code>
 * @author Robert Toomey
 */
public class VolumeRecord implements java.lang.Iterable<IndexRecord> {

    /** The sorted list of IndexRecords in the volume */
    protected ArrayList<IndexRecord> myRecordSet;
    /** The marked record in the volume. This record is used
     * as a relative navigation reference for peek/movement functions */
    private IndexRecord myMarkedRecord;
    /** The index integer location of the record used as the marker */
    private int myMarkedIndex;
    /** The default comparator for records that allows a sorting of the records of a volume.
     * Default is based on the order of the subtype strings.  You might want to have a different type
     * of volume based on a different compare.
     */
    private static final Comparator<IndexRecord> defaultVolumeComparator = new Comparator<IndexRecord>() {

        @Override
        public int compare(IndexRecord arg0, IndexRecord arg1) {
            return (arg0.getSubType().compareTo(arg1.getSubType()));
        }
    };

    /** Create a volume record. Normally you'd use one of the static functions in this class to create one
     * 
     * @param refRecords collection of records making up the volume
     * @param refMarked the marked record used for relative navigation
     */
    private VolumeRecord(ArrayList<IndexRecord> refRecords, IndexRecord refMarked) {
        myRecordSet = refRecords;
        myMarkedRecord = refMarked;
        // Mark the index of the latest record...
        for (int i = 0; i < refRecords.size(); i++) {
            if (myRecordSet.get(i) == myMarkedRecord) {
                myMarkedIndex = i;
                break;
            }
        }
        // FIXME: marked record MUST be in the record set
    }

    /** Snag the default comparator for IndexRecords.  It just checks the subtype.
     *
     * @return the default comparator
     */
    public static Comparator<IndexRecord> getDefaultComparator() {
        return defaultVolumeComparator;
    }

    /**
     * Get the volume size
     * @return the number of records in the volume
     */
    public int size() {
        if (myRecordSet != null) {
            return myRecordSet.size();
        }
        return 0;
    }

    // The single interface for iterable, allows you to:
    // for (IndexRecord r: myVolumeRecord)
    @Override
    public Iterator<IndexRecord> iterator() {
        return myRecordSet.iterator();
    }

    /** Get the index of the marked IndexRecord in the volume
     * 
     * @return index of the marked IndexRecord
     */
    public int getMarkedIndex() {
        return myMarkedIndex;
    }

    /** Set the index of the marked IndexRecord in the volume 
     * 
     * @param mark ordered record number
     */
    public void setMarkedIndex(int mark) {
        if ((mark >= 0) && (mark < size())) {
            myMarkedIndex = mark;
        }
    }

    /** The base record is the first in the set 
     * 
     * @return the base subtype record of the volume
     */
    public IndexRecord getBaseRecord() {
        return (getRecord(0));
    }

    /**
     * Get an IndexRecord out of the VolumeRecord at a given position
     * @param i index of the IndexRecord with circular rolling
     * @return the IndexRecord at index
     */
    public IndexRecord getRecord(int i) {
        // Gets given record number. ROLLS if below or above range of
        // myRecordSet
        // System.out.println("Get record called with index "+i);
        int index = i % myRecordSet.size(); // 'remainder' not modulus...soooo
        if (index < 0) { 					// Make it real modulus
            index += myRecordSet.size();
        }

        // System.out.println("Getting record "+index);
        // size = 4 --> 0,1,2,3,0,1,2,3,0,1,2,3 etc...
        // -1 mod 4 == 3 reverse as expected
        return myRecordSet.get(index);
    }

    /**
     * Age test of two IndexRecord
     * @param a	first record
     * @param b second record
     * @return true is first is older than second
     */
    private boolean isOlder(IndexRecord a, IndexRecord b) {
        boolean older = true;
        if ((a != null) && (b != null)) {
            Date aDate = a.getTime();
            Date bDate = b.getTime();
            older = aDate.before(bDate);
        }
        return older;
    }

    /** Is the record in this volume with next subtype in the latest volume,
     * or a previous one?  In the GUI this would be the pink/green button test for 
     * the virtual buttons.
     */
    public boolean upIsInLatestVolume() {
        // If record above is older than the base, it's not in latest
        // This assumes base is always the first record in a set to arrive
        return (!isOlder(peekUp(), getBaseRecord()));
    }

    /** Is the record in this volume with previous subtype in the latest volume,
     * or a previous one?  In the GUI this would be the pink/green button test for 
     * the virtual buttons.
     */
    public boolean downIsInLatestVolume() {
        // If record below is older than the base, it's not in latest
        // This assumes base is always the first record in a set to arrive
        return (!isOlder(peekDown(), getBaseRecord()));
    }

    /** Is the record that is lowest in the volume record in latest volume?
     * In the GUI this would be the pink/green button test for the virtual
     * 'base' button
     */
    public boolean baseIsInLatestVolume() {
        // Assuming base always newest record...
        return true;
    }

    /** Peek 'up' a subtype from the current marked record (circular rolling)
     * 
     * @return the next subtype or lowest in volume on overflow
     */
    public IndexRecord peekUp() {
        return (getRecord(getMarkedIndex() + 1));
    }

    /** Peek 'down' a subtype from the current marked record (circular rolling)
     * 
     * @return the previous subtype or highest in volume on underflow
     */
    public IndexRecord peekDown() {
        return (getRecord(getMarkedIndex() - 1));
    }

    /** Create a VolumeRecord for a virtual volume 
     * @param index	the index record is part of
     * @param reference	the IndexRecord used to find the virtual volume for
     * @param compare	used to sort volume into logical order
     * @return the virtual volume, if any
     */
    public static VolumeRecord getVirtualVolumeRecord(HistoricalIndex index,
            IndexRecord reference, Comparator<IndexRecord> compare) {

        if (index == null) {
            return null;
        }
        if (compare == null) {
            compare = defaultVolumeComparator;
        }

        // Ok algorithm works like this. Get latest of all records, then...keep
        // adding previous records to the set as long as the subtype is different...
        // Then we sort the records by subtype.
        VolumeRecord volume = null;
        ArrayList<IndexRecord> myList = new ArrayList<IndexRecord>();
        Set<String> mySubtypes = new HashSet<String>();

        //IndexRecord latestAtSubtype = SourceManager.getRecord(indexKey,
        //		reference, Direction.LatestTime);
        IndexRecord latestAtSubtype = index.getNextRecord(reference, Direction.LatestTime);
        //IndexRecord latestRecord = SourceManager.getRecord(indexKey,
        //		reference, Direction.LatestAllSubTypes);
        IndexRecord latestRecord = index.getNextRecord(reference, Direction.LatestAllSubTypes);

        if (latestRecord != null) {

            // Grab each previous subtype as long as it's different
            // FIXME: what about VCP changes? We will end up snagging two
            // volumes worth of records probably...
            // FIXME: Check for null index and records?
            IndexRecord current = latestRecord;
            boolean keepGoing = true;
            int safetyCounter = 0;

            // Add the very last record
            myList.add(latestRecord);
            mySubtypes.add(latestRecord.getSubType());

            // Now search until we encounter a duplicate subtype or null
            while (keepGoing) {
                //IndexRecord previous = SourceManager.getRecord(indexKey,
                //		current, Direction.PreviousSubType);
                IndexRecord previous = index.getNextRecord(current, Direction.PreviousSubType);
                if ((previous != null)
                        && (mySubtypes.add(previous.getSubType()))) {
                    myList.add(previous);
                    current = previous;
                    keepGoing = true;
                    safetyCounter++;
                    if (safetyCounter > 50) {
                        keepGoing = false;
                    }
                } else {
                    keepGoing = false;
                }
            }

            // Now sort the records in the arraylist by the subtype (comparator
            // part of product)
            Collections.sort(myList, compare);
            volume = new VolumeRecord(myList, latestAtSubtype);

        }
        return volume;
    }

    /** Get the standard volume for a given record
     * 
     * @param index	the index record is part of
     * @param reference	the IndexRecord used to find the volume for
     * @param compare	the comparator for sorting/matching volume records
     * @return	new volume containing volume for the given IndexRecord
     */
    public static VolumeRecord getVolumeRecord(HistoricalIndex index, IndexRecord reference,
            Comparator<IndexRecord> compare) {

        if (index == null) {
            return null;
        }
        if (compare == null) {
            compare = defaultVolumeComparator;
        }

        VolumeRecord volume = null;
        ArrayList<IndexRecord> myList = new ArrayList<IndexRecord>();
        Set<String> mySubtypes = new HashSet<String>();

        if (reference != null) {
            IndexRecord current = reference;
            boolean keepGoing = true;
            int safetyCounter = 0;

            // The reference record is always part of its own volume
            myList.add(current);
            mySubtypes.add(current.getSubType());

            // ---------------------------------------------------
            // Add previous records until comparator fails or null,
            // this will get the 'lowest' subtype based on comparator type
            while (keepGoing) {
                //IndexRecord previous = SourceManager.getRecord(indexKey,
                //		current, Direction.PreviousSubType);				
                IndexRecord previous = index.getNextRecord(current, Direction.PreviousSubType);
                if ((previous != null)
                        //&& (previous.getSubType().compareTo(
                        //		current.getSubType()) < 0)) {
                        && (compare.compare(previous, current) < 0)) {
                    mySubtypes.add(previous.getSubType());
                    myList.add(previous);
                    current = previous;
                    keepGoing = true;
                    safetyCounter++;
                    if (safetyCounter > 25) {
                        keepGoing = false;
                    }
                } else {
                    keepGoing = false;
                }
            }

            // Add future records until comparator fails or null
            current = reference;
            keepGoing = true;
            while (keepGoing) {
                //IndexRecord next = SourceManager.getRecord(indexKey, current,
                //		Direction.NextSubType);
                IndexRecord next = index.getNextRecord(current, Direction.NextSubType);
                if ((next != null) && (mySubtypes.add(next.getSubType()))) {
                    myList.add(next);
                    current = next;
                    keepGoing = true;
                    safetyCounter++;
                    if (safetyCounter > 25) {
                        keepGoing = false;
                    }
                } else {
                    keepGoing = false;
                }
            }

            // Now sort the records in the arraylist by the subtype (comparator
            // part of product)
            Collections.sort(myList, compare);
            volume = new VolumeRecord(myList, reference);
        }
        return volume;
    }
};
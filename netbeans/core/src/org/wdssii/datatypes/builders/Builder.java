package org.wdssii.datatypes.builders;

import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.DataType;
import org.wdssii.index.IndexRecord;

/**
 * @author lakshman
 * 
 */
public interface Builder {

    /** Create a DataType and all its stuff.  Do everything in one thread */
    public abstract DataType createObject(IndexRecord rec);

    /** Create a DataRequest.  Separate thread.  See DataRequest for details */
    public abstract DataRequest createObjectBackground(IndexRecord rec);
}

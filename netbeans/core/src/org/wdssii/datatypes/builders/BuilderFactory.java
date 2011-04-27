package org.wdssii.datatypes.builders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.core.DataUnavailableException;
import org.wdssii.core.PrototypeFactory;
import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.DataType;
import org.wdssii.index.IndexRecord;

/**
 * @author lakshman
 * 
 */
public abstract class BuilderFactory {

    private static Log log = LogFactory.getLog(BuilderFactory.class);
    /** name to Builder */
    private static PrototypeFactory<Builder> factory = new PrototypeFactory<Builder>(
            "java/Builder.xml");

    /** The single thread do all the work call.  This blocks until DataType is completely loaded and ready.
     * This is what you want for algorithms probably.
     * @param rec
     * @return
     * @throws DataUnavailableException
     */
    public static DataType createDataType(IndexRecord rec)
            throws DataUnavailableException {

        String builderName = rec.getParams()[0];

        // Hack....in the GUI we're gonna use pure URL format for data
        if (builderName.equals("http")) {
            builderName = "webindex";
        };

        Builder builder = factory.getPrototypeMaster(builderName);
        if (builder == null) {
            log.error("ERROR: no such builder: " + builderName);
            return null;
        }
        return builder.createObject(rec);
    }

    /** The background job version.  This doesn't block and returns a DataRequest which is a future
     * that holds a pointer to a future DataType.  See DataRequest for example 
     */
    public static DataRequest createDataRequest(IndexRecord rec) {
        String builderName = rec.getParams()[0];

        // Hack....in the GUI we're gonna use pure URL format for data
        if (builderName.equals("http")) {
            builderName = "webindex";
        };

        Builder builder = factory.getPrototypeMaster(builderName);
        if (builder == null) {
            log.error("ERROR: no such builder: " + builderName);
            return null;
        }
        return builder.createObjectBackground(rec);
    }
}

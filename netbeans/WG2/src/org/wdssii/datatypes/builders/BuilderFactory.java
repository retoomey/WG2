package org.wdssii.datatypes.builders;

import java.net.URL;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.PrototypeFactory;
import org.wdssii.datatypes.DataRequest;

/**
 * @author lakshman
 * 
 * BuilderFactory maps a set of builder names to the objects that know how to
 * build them.  For example, if an IndexRecord refers to a netcdf file, we
 * pass the information to the netcdfbuilder to build the datatype.
 * 
 */
public class BuilderFactory {

    private final static Logger LOG = LoggerFactory.getLogger(BuilderFactory.class);
    /** name to Builder */
    private static final PrototypeFactory<Builder> factory;

    /** Create the factory from Builder.xml in the xml, OR use
     * a stock set of built in defaults.  This rarely changes, so this
     * allows overriding without breaking if w2config is missing.
     */
    static {
        factory = new PrototypeFactory<Builder>(
                "java/Builder.xml");
        factory.addDefault("netcdf", "org.wdssii.datatypes.builders.netcdf.NetcdfBuilder");
        factory.addDefault("W2ALGS", "org.wdssii.datatypes.builders.xml.XMLBuilder");

    }
    
    /** Get the builder for a given builder name */
    public static Builder getBuilder(String builderName) {
        Builder builder = factory.getPrototypeMaster(builderName);
        return builder;
    }

    /** The single thread do all the work call.  This blocks until DataType is completely loaded and ready.
     * This is what you want for algorithms probably.
     * @param rec
     * @return
     * @throws DataUnavailableException
     */
  /*  public static DataType createDataType(IndexRecord rec)
            throws DataUnavailableException {
	Builder builder = rec.getBuilder();
	if (builder == null){
            LOG.error("No builder for this record");
            return null;
	}
        return builder.createDataType(rec, null);
    }*/

    /** The background job version.  This doesn't block and returns a DataRequest which is a future
     * that holds a pointer to a future DataType.  See DataRequest for example 
     */
    public static DataRequest createDataRequestURL(Builder builder, URL theURL, String theDataType) 
    {
	//Builder builder = rec.getBuilder();
	if (builder == null){
            LOG.error("No builder for this record");
            return null;
	}
        return builder.createDataRequest(theURL, theDataType);
    }
}

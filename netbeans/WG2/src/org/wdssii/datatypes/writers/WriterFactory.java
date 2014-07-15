package org.wdssii.datatypes.writers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.PrototypeFactory;

/**
 * @author Robert Toomey
 * 
 * WriterFactory maps a set of writer names to the objects that know how to
 * write them. 
 * 
 */
public class WriterFactory {

    private final static Logger LOG = LoggerFactory.getLogger(WriterFactory.class);
    /** name to Builder */
    private static final PrototypeFactory<DataTypeWriter> factory;

    /** Create the factory from Builder.xml in the xml, OR use
     * a stock set of built in defaults.  This rarely changes, so this
     * allows overriding without breaking if w2config is missing.
     */
    static {
        factory = new PrototypeFactory<DataTypeWriter>();
        factory.addDefault("ESRI", "org.wdssii.datatypes.writers.esri.ESRIWriter");
        factory.addDefault("CSV", "org.wdssii.datatypes.writers.csv.CSVWriter");

    }
    
    /** Get the builder for a given builder name */
    public static DataTypeWriter getWriter(String writerName) {
        DataTypeWriter writer = factory.getPrototypeMaster(writerName);
        return writer;
    }
}

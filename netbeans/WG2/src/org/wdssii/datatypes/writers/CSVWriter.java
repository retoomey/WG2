package org.wdssii.datatypes.writers;

import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;

import org.wdssii.datatypes.DataType;
import org.wdssii.geom.GridVisibleArea;

/**
 * Base class for all CSV output from a DataType
 * *
 * @author Robert Toomey
 */
public class CSVWriter extends DataTypeWriter {
    
    private final static Logger LOG = LoggerFactory.getLogger(CSVWriter.class);

    /**
     * Dispatch to helper classes
     */
    @Override
    public void exportDataTypeToURL(DataType d, URL aURL, GridVisibleArea g, WdssiiJobMonitor m) {
        
        String name = d.getClass().getSimpleName();
        if (m != null){
            m.subTask("Creating CSV writer for " + name);
        }
        DataTypeWriter w = getHelperClass("org.wdssii.datatypes.writers.csv." + name + "CSVWriter");
        if (w != null) {
            DataTypeWriterOptions o2 = new DataTypeWriterOptions(name + " to CSV", m, aURL, d);
            o2.setSubGrid(g);
            w.export(o2);
        } else {
            LOG.error("Couldn't find CSV writer for datatype "+name);
        }
    }

    /**
     * Override with helper classes
     */
    @Override
    public WdssiiJobStatus export(DataTypeWriterOptions o) {
        return WdssiiJobStatus.CANCEL_STATUS;
    }
}

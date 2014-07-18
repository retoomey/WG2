package org.wdssii.datatypes.writers.esri;

import java.net.URL;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;

import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.writers.DataTypeWriter;
import org.wdssii.geom.GridVisibleArea;

/**
 * Base class for all ESRI geotools output from a DataType
 *
 * FIXME: common esri stuff will go here eventually..
 *
 * @author Robert Toomey
 */
public class ESRIWriter extends DataTypeWriter {
    
    private final static Logger LOG = LoggerFactory.getLogger(ESRIWriter.class);

    /**
     * Dispatch to helper classes
     */
    @Override
    public void exportDataTypeToURL(DataType d, URL aURL, GridVisibleArea g, WdssiiJobMonitor m) {
        
        String name = d.getClass().getSimpleName();
        if (m != null){
            m.subTask("Creating ESRI writer for " + name);
        }
        DataTypeWriter w = getHelperClass("org.wdssii.datatypes.writers.esri." + name + "ESRIWriter");
        if (w != null) {
            DataTypeWriterOptions o2 = new DataTypeWriterOptions(name + " to Quads", m, aURL, d);
            o2.setSubGrid(g);
            w.export(o2);
        } else {
            LOG.error("Couldn't find ESRI writer for datatype "+name);
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

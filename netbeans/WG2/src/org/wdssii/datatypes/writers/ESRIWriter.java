package org.wdssii.datatypes.writers;

import org.wdssii.core.WdssiiJob.WdssiiJobStatus;

/**
 * Base class for all ESRI geotools output from a DataType
 *
 * FIXME: common esri stuff will go here eventually..
 *
 * @author Robert Toomey
 */
public class ESRIWriter extends DataTypeWriter {

    @Override
    public WdssiiJobStatus export(DataTypeWriterOptions o) {
        return WdssiiJobStatus.CANCEL_STATUS;
    }
}

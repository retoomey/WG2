package org.wdssii.datatypes.writers;

import java.net.URL;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataType;

/**
 * Base class for all ESRI geotools output from a DataType
 *
 * @author Robert Toomey
 */
public class ESRIWriter {

    /**
     * The worker job if we are threading
     */
    private backgroundRender myWorker = null;

    /**
     * Run job in worker thread...
     */
    public class backgroundRender extends WdssiiJob {

        private DataType myDataType = null;
        private URL myOutput = null;

        public backgroundRender(String jobName, DataType data, URL output, WdssiiJobMonitor m) {
            super(jobName, m);
            myDataType = data;
            myOutput = output;
        }

        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
            return export(myDataType, myOutput, monitor); // no this we want override
        }
    }

    /** Called to export data */
    public void export(String jobName, DataType data, URL output, WdssiiJobMonitor m){
        if (myWorker == null) {
             myWorker = new backgroundRender(jobName, data, output, m);
             myWorker.schedule();
        }
    }
    
    /** Do the work here */
    protected WdssiiJobStatus export(DataType data, URL aURL, WdssiiJobMonitor monitor) {
        return WdssiiJobStatus.CANCEL_STATUS;
    }
}

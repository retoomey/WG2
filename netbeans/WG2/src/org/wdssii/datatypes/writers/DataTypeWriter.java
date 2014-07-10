package org.wdssii.datatypes.writers;

import java.net.URL;
import org.wdssii.geom.GridVisibleArea;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.core.WdssiiJob.WdssiiSameThreadJobMonitor;
import org.wdssii.datatypes.DataType;
import org.wdssii.gui.products.ProductFeatureGUI;

/**
 * Base class of all DataType writers
 *
 * @author Robert Toomey
 */
public abstract class DataTypeWriter {

    /**
     * The worker job iff we are threading
     */
    private BackgroundJob myWorker = null;

    /** Export data type to a URL */
    public abstract void exportDataTypeToURL(DataType d, URL aURL, GridVisibleArea g, WdssiiJobMonitor m);

    /**
     * Base class of all writer options. Options have all the information needed
     * to write something out.  Subclass to pass more information into a writer,
     * this has the basic info to get you started.
     */
    public static class DataTypeWriterOptions {

        /**
         * The optional monitor for this job, can be null for synchronous
         */
        private WdssiiJobMonitor myMonitor;
        /**
         * The job name
         */
        private String myJobName;
        /**
         * The URL to output to
         */
        private URL myURL;
        /**
         * The datatype for output
         */
        private DataType myDataType;
        /**
         * Sub grid for table output...not sure this belongs here..might
         * subclass eventually
         */
        private GridVisibleArea mySubGrid;

        /**
         * Create writer options
         */
        public DataTypeWriterOptions(String jobName, WdssiiJobMonitor m, URL out, DataType d) {
            if (m == null){
                m = new WdssiiSameThreadJobMonitor();  // Empty monitor
            }
            myMonitor = m;
            myURL = out;
            myDataType = d;
            myJobName = jobName;
        }

        public WdssiiJobMonitor getMonitor() {
            return myMonitor;
        }

        public String getJobName() {
            return myJobName;
        }

        public URL getURL() {
            return myURL;
        }

        public DataType getData() {
            return myDataType;
        }

        public GridVisibleArea getSubGrid() {
            return mySubGrid;
        }

        public void setSubGrid(GridVisibleArea g) {
            mySubGrid = g;
        }
    }

    /**
     * Run job in worker thread...
     */
    private class BackgroundJob extends WdssiiJob {

        private DataTypeWriterOptions myOptions;

        public BackgroundJob(DataTypeWriterOptions o) {
            super(o.myJobName, o.myMonitor);
            myOptions = o;
        }

        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
            return export(myOptions);
        }
    }

    /**
     * Export with a background job
     */
    public void exportBackground(DataTypeWriterOptions o) {
        if (myWorker == null) {  // Just to keep from doing twice....
            myWorker = new BackgroundJob(o);
            myWorker.schedule();
        }
    }

    /** Get a helper class.  For example ESRIWriter could create a RadialSetESRIWriter to
     * handle that particular datatype */
    public DataTypeWriter getHelperClass(String createByName) {

        DataTypeWriter newWriter = null;
        // Create particular writer from datatype name by reflection
        try {
            Class<?> aClass = null;
            aClass = Class.forName(createByName);
            //   Class<?>[] argTypes = new Class[]{NetcdfFile.class, boolean.class};
            //   Object[] args = new Object[]{ncfile, sparse}; // Actual args

            //DataType createFromNetcdf(NetcdfFile ncfile, boolean sparse)
            //   //Constructor<?> c = aClass.getConstructor(argTypes);
            Object classInstance = aClass.newInstance();
            newWriter = (DataTypeWriter)(classInstance);
            //    Method aMethod = aClass.getMethod("createFromNetcdf", argTypes);
            //   obj = (DataType) aMethod.invoke(classInstance, args);
        } catch (Exception e) {
        }
        return newWriter;
    }
    
    /**
     * Do the actual export work
     */
    public abstract WdssiiJobStatus export(DataTypeWriterOptions o);
}

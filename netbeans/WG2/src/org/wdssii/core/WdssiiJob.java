package org.wdssii.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * WdssiiJob is our class that acts as a wrapper to another job type, such as
 * swt's Job or Netbean's AsyncGUIJob or even just a regular thread. The reason
 * is that GUI based jobs let us have progress bars, cancel buttons, etc..that
 * you can't get with regular background threads. Having this decouples our code
 * from the particular GUI library requirement.
 *
 * org.wdssii.gui.rcp.EclipseJob is the RCP version, the code must call
 * WdssiiJob.introduce(new EclipseJob.EclipseJobFactory());
 * org.wdssii.gui.nbm.NetbeansJob is the netbeans version, the code must call
 * WdssiiJob.introduce(new NetbeansJob.NetbeansJobFactory());
 *
 * Based this currently off the Eclipse Job classes (since it is the first use
 * of it), probably will have to modify for netbeans/or text based progress
 * output.
 *
 * @author Robert Toomey
 *
 */
public class WdssiiJob {

    private final static Logger LOG = LoggerFactory.getLogger(WdssiiJob.class);
    /**
     * The factory used for all created jobs
     */
    private static WdssiiJobFactory myFactory = null;
    /**
     * The max threads
     */
    private static final int myMaxThreads = 10;
    /**
     * Our single executor service, for now at least
     */
    private static final ExecutorService myService = Executors.newFixedThreadPool(myMaxThreads);
    /**
     * Count of total started jobs
     */
    private static final Object startSync = new Object();
    private static long totalJobsStarted = 0;
    /**
     * Count of total finished jobs
     */
    private static final Object endSync = new Object();
    private static long totalJobsFinished = 0;

    public static void upStartCount() {
        synchronized (startSync) {
            totalJobsStarted++;
        }
    }

    public static void upEndCount() {
        synchronized (endSync) {
            totalJobsFinished++;
        }
    }

    public static long getStartCount() {
        synchronized (startSync) {
            return totalJobsStarted;
        }
    }

    public static long getEndCount() {
        synchronized (endSync) {
            return totalJobsFinished;
        }
    }
    /**
     * The job runner for our job
     */
    private WdssiiJobRunner myJobRunner = null;

    /**
     * The Executor service for all WdssiiJobs
     */
    public static ExecutorService getService() {
        return myService;
    }

    /**
     * Introduce job class by name. Job class must have a constructor of form
     * Job(WdssiiJob) and a method schedule() that starts the job. This is just
     * instead of making some static factory creator.
     *
     * @see EclipseJob
     */
    public static void introduce(WdssiiJobFactory factory) {
        myFactory = factory;
    }

    /**
     * Interface for job factory
     */
    public static interface WdssiiJobFactory {

        public WdssiiJobRunner createJobRunner(WdssiiJob job);
    }

    /**
     * A job can use a monitor to report progress back.
     */
    public static interface WdssiiJobMonitor {

        /**
         * Unknown duration is -1
         */
        public int UNKNOWN = -1;

        /**
         * Job letting monitor know it's done
         */
        public void done();

        /**
         * Job letting monitor know a task of given name is beginning and to
         * expect totalUnits worth of work
         */
        public void beginTask(String taskName, int totalUnits);

        /**
         * A subtask of the current job is beginning (secondary job label in
         * GUI)
         */
        public void subTask(String subTaskName);

        /**
         * Notification of units worked
         */
        public void worked(int howMany);

        /**
         * Has the job been canceled?
         */
        public boolean isCanceled();

        /**
         * Cancel this job
         */
        public void cancel();
    };

    public static interface WdssiiJobRunner {

        public void WdssiiStartJob();

        public void WdssiiCancelJob();
    }

    /**
     * Returned to tell job status.
     */
    public static enum WdssiiJobStatus {

        OK_STATUS, // Job completed successfully
        CANCEL_STATUS	// Job was canceled
    }
    /**
     * The name of the job
     */
    private String myName;
    /**
     * A monitor for this job
     */
    private WdssiiJobMonitor myMonitor = null;

    /**
     * Create a job with given name
     */
    public WdssiiJob(String jobName) {
        myName = jobName;
    }

    /**
     * Create a job with given name and an extra job listener
     */
    public WdssiiJob(String jobName, WdssiiJobMonitor monitor) {
        myName = jobName;
        myMonitor = monitor;
    }

    public String getName() {
        return myName;
    }

    public WdssiiJobMonitor getMonitor() {
        return myMonitor;
    }

    /**
     * Run the job with a given monitor
     */
    public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
        return WdssiiJobStatus.OK_STATUS;
    }

    /**
     * Schedule the job to run
     */
    public void schedule() {

        if (myFactory != null) {
            if (myJobRunner == null) {
                myJobRunner = myFactory.createJobRunner(this);
                myJobRunner.WdssiiStartJob();
            }
        } else {
            LOG.error("No JobFactory exists.  You must call WdssiiJob.introduce(WdssiiJobFactory) to run background jobs");
        }

    }

    public void cancel() {
        if (myJobRunner != null) {
            myJobRunner.WdssiiCancelJob();
        }
    }

    /**
     * Filler for non-job routines (to save null checking)
     */
    public static class WdssiiSameThreadJobMonitor implements WdssiiJobMonitor {

        @Override
        public void done() {
        }

        @Override
        public void beginTask(String taskName, int totalUnits) {
        }

        @Override
        public void subTask(String subTaskName) {
        }

        @Override
        public void worked(int howMany) {
        }

        @Override
        public boolean isCanceled() {
            return true;
        }

        @Override
        public void cancel() {
        }
    }
}

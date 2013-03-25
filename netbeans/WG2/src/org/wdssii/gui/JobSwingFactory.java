package org.wdssii.gui;

import java.util.concurrent.ExecutorService;
import org.wdssii.core.WdssiiJob;
import org.wdssii.gui.views.JobsView;

/**
 * Creates a job within the Swing environment
 *
 * @author Robert Toomey
 */
public class JobSwingFactory implements WdssiiJob.WdssiiJobFactory {

    @Override
    public WdssiiJob.WdssiiJobRunner createJobRunner(WdssiiJob job) {
        JobSwingRunner realJob = new JobSwingRunner(job);
        return realJob;
    }

    /**
     * Runs a particular job. Has swing based progress bar, etc...
     */
    public static class JobSwingRunner implements WdssiiJob.WdssiiJobRunner {

        /**
         * Currently we have one JobsView window..this is set to non-null once
         * it's made...bleh..might try to use before it's ready...
         *
         */
        // public static JobsView myView = null;
        public static JobsView.JobsViewHandler myHandler = new JobsView.JobsViewHandler();
        /**
         * Sync object for changing WdssiiJob
         */
        private final Object jobSync = new Object();
        /**
         * The job we are handling
         */
        private WdssiiJob myWdssiiJob;
        /**
         * The monitor object for this job. Used by the job to send back info on
         * the job state/status as it runs.
         */
        private JobSwingMonitor myWdssiiJobMonitor;

        private JobSwingRunner(WdssiiJob job) {
            myWdssiiJob = job;

            // Register this runner with JobView?
        }

        /**
         * The monitor used by jobs for reporting to the GUI
         */
        public static class JobSwingMonitor implements WdssiiJob.WdssiiJobMonitor {

            private int myTotalUnits = 0;
            private int myCurrentUnits = 0;
            private boolean myIsStarted = false;
            private boolean myHaveFinished = false;
            private final Object myStartSync = new Object();
            private boolean myCanceled = false;

            private JobSwingMonitor(WdssiiJob job) {
                myHandler.addJob(this.toString());
            }

            @Override
            public void done() {
                synchronized (myStartSync) {
                    if (!myHaveFinished) {
                        myHandler.finish(this.toString());
                        myHaveFinished = true;
                    }
                }
            }

            @Override
            public void beginTask(String taskName, int totalUnits) {
                myTotalUnits = totalUnits;

                synchronized (myStartSync) {

                    /**
                     * Start the job by adding to handler
                     */
                    try {
                        //if (JobSwingRunner.myView != null) {
                        myHandler.setLabel(this.toString(), taskName);
                        // }
                    } catch (Exception e) {
                    } finally {
                        // Assume started even on exception, so we will try
                        // to remove everying on 'done'
                        myIsStarted = true;
                    }

                    /**
                     * Set units for the task
                     */
                    if (myTotalUnits == -1) {
                        // if (myView != null) {
                        myHandler.switchToIndeterminate(this.toString());
                        // }
                        // myProgressHandle.start();
                        // myProgressHandle.switchToIndeterminate();
                    } else {

                        // if (myView != null) {
                        myHandler.switchToDeterminate(this.toString());
                        myHandler.setMaximum(this.toString(), myTotalUnits);
                        // }
                        // myProgressHandle.start(totalUnits);
                    }
                }

            }

            @Override
            public void subTask(String subTaskName) {
                myHandler.setSubTask(this.toString(), subTaskName);
            }

            @Override
            /**
             * How how many is added, netbeans is the total so far
             */
            public void worked(int howMany) {
                myCurrentUnits += howMany;
                if (myTotalUnits == -1) {
                    // do anything here?
                } else {
                    if (myCurrentUnits > myTotalUnits) {
                        myCurrentUnits = myTotalUnits;
                    }
                    //if (myView != null) {
                    myHandler.progress(this.toString(), myCurrentUnits);
                    // }

                    // myProgressHandle.progress(myCurrentUnits);
                }
            }

            @Override
            public boolean isCanceled() {
                return myCanceled;
            }

            public void cancel() {
                synchronized (myStartSync) {
                    if (myIsStarted) {
                        // myProgressHandle.finish();
                        myIsStarted = false;
                    }
                    myCanceled = true;
                }
            }
        }

        @Override
        public void WdssiiStartJob() {

            /**
             * Create the runnable
             */
            Runnable r = new Runnable() {
                @Override
                public void run() {

                    // When about to run, set up the swing gui stuff.
                    // note we're in a different thread..
                    try {
                        // Multiple runners....
                        WdssiiJob.upStartCount();  
                        synchronized (jobSync) {
                            myWdssiiJobMonitor = new JobSwingMonitor(myWdssiiJob);
                        }
                        myWdssiiJob.run(myWdssiiJobMonitor);
                    } finally {
                        // Make sure and cleanup if job didn't call done...
                        WdssiiJob.upEndCount(); 
                        synchronized (jobSync) {
                            myWdssiiJobMonitor.done();
                        }
                        // When job is complete, we need to remove from
                        // list of running jobs...
                    }
                }
            };


            /**
             * Send to our executor pool
             */
            ExecutorService s = WdssiiJob.getService();
            Thread t = new Thread(r, "WdssiiBackgroundTask");
            // t.start();
            if (!s.isShutdown()) {
                s.submit(t);
            }


        }

        @Override
        public void WdssiiCancelJob() {
            synchronized (jobSync) {
                if (myWdssiiJobMonitor != null) {
                    myWdssiiJobMonitor.cancel();
                }
            }
        }
    }
}

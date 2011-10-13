package org.wdssii.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JProgressBar;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobRunner;
import org.wdssii.gui.views.JobsView;

/**
 *
 * @author Robert Toomey
 * 
 * A job manager for the GUI
 * 
 * The GUI that will use our own swing view for displaying
 * running jobs.  We'll use an executor to handle running jobs
 * 
 * We'll link to a JobsView...
 * FIXME: make all the job stuff part of org.wdssii.jobs
 * The 'swing' stuff, etc... all here for now as I crank out the code
 * will organize it later
 * 
 */
public class JobManager {

    /** The singleton */
    private static JobManager instance = null;
    /** Our single executor service, for now at least */
    private ExecutorService myService;
    /** The max threads */
    private int myMaxThreads = 10;

    /** Factory for creating a job */
    public static class JobSwingFactory implements WdssiiJobFactory {

        @Override
        public WdssiiJobRunner createJobRunner(WdssiiJob job) {
            JobSwingRunner realJob = new JobSwingRunner(job);
            return realJob;
        }
    }

    /** Runs a particular job.  Has swing based progress bar, etc...*/
    public static class JobSwingRunner implements WdssiiJobRunner {

        /** Currently we have one JobsView window..this is set to non-null
         * once it's made...bleh..might try to use before it's ready...
         * 
         */
       // public static JobsView myView = null;
        public static JobsView.JobsViewHandler myHandler = new JobsView.JobsViewHandler();
        
        /** The job we are handling */
        private WdssiiJob myWdssiiJob;
        /** The monitor object for this job.  Used by the job to send back
        info on the job state/status as it runs. */
        private JobSwingMonitor myWdssiiJobMonitor;

        private JobSwingRunner(WdssiiJob job) {
            myWdssiiJob = job;

            // Register this runner with JobView?
        }

        /** The monitor used by jobs for reporting to the GUI */
        public static class JobSwingMonitor implements WdssiiJobMonitor {

            private int myTotalUnits = 0;
            private int myCurrentUnits = 0;
            private boolean myIsStarted = false;
            private boolean myHaveFinished = false;
            private final Object myStartSync = new Object();
            // private JProgressBar myBar;

            private JobSwingMonitor(WdssiiJob job) {
            }

            @Override
            public void done() {
                synchronized (myStartSync) {
                    if (myIsStarted && !myHaveFinished) {
                        if (myTotalUnits != -1) {
                            //  myProgressHandle.progress(myTotalUnits);
                                myHandler.progress(this.toString(), myTotalUnits);
                        }
                        // myProgressHandle.finish();
                            myHandler.finish(this.toString());
                            myHaveFinished = true;
                    }
                }
            }

            @Override
            public void beginTask(String taskName, int totalUnits) {
                myTotalUnits = totalUnits;

                synchronized (myStartSync) {
                    // Try to create a bar in the JobsView
                    // Humm..maybe we should just send job INFO to JobsView...
                    // that way it can sync
                    try {
                        //if (JobSwingRunner.myView != null) {
                            myHandler.addJob(this.toString(), taskName);
                           
                       // }
                    } catch (Exception e) {
                    }

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
                    myIsStarted = true;
                }

            }

            @Override
            public void subTask(String subTaskName) {
                //myProgressHandle.setDisplayName(subTaskName); 
            }

            @Override
            /** How how many is added, netbeans is the total so far */
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
                return false; // @todo implement
            }

            public void cancel() {
                synchronized (myStartSync) {
                    if (myIsStarted) {
                        // myProgressHandle.finish();
                        myIsStarted = false;
                    }
                }
            }
        }

        @Override
        public void WdssiiStartJob() {

            /** Create the runnable */
            Runnable r = new Runnable() {

                @Override
                public void run() {

                    // When about to run, set up the swing gui stuff.
                    // note we're in a different thread..
                    try {
                        myWdssiiJobMonitor = new JobSwingMonitor(myWdssiiJob);
                        myWdssiiJob.run(myWdssiiJobMonitor);
                    } finally {
                        // Make sure and cleanup if job didn't call done...
                        myWdssiiJobMonitor.done();
                        // When job is complete, we need to remove from
                        // list of running jobs...
                    }
                }
            };


            /** Send to our executor pool */
            ExecutorService s = JobManager.getInstance().getService();
            Thread t = new Thread(r, "WdssiiBackgroundTask");
            // t.start();
            if (!s.isShutdown()) {
                s.submit(t);
            }


        }

        @Override
        public void WdssiiCancelJob() {
        }
    }

    // Manager ---------------------
    /**
     * @return the singleton for the manager
     */
    public static JobManager getInstance() {
        if (instance == null) {
            instance = new JobManager();

            // The netbeans job handler which uses the netbeans
            // bar for doing stuff...
            // WdssiiJob.introduce(new NBJobHandler.NBJobFactory());
            WdssiiJob.introduce(new JobSwingFactory());
            // This is crashing...some ordering issue FIXME
            //SingletonManager.registerSingleton(instance);

        }
        return instance;
    }

    public ExecutorService getService() {
        if (myService == null) {
            myService = Executors.newFixedThreadPool(myMaxThreads);
        }
        return myService;
    }
}

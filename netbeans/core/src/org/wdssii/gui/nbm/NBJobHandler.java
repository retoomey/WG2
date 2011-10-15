package org.wdssii.gui.nbm;

import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobRunner;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;

/**
 * Wdssii job runner that uses the netbeans background progress bar at
 * the bottom of the main window.  We could always plug in another
 * job handler later for different behavior for our background jobs.
 * 
 * @author Robert Toomey
 */
public class NBJobHandler implements WdssiiJobRunner {

    private WdssiiJob myWdssiiJob;
    private NBJobMonitor myWdssiiJobMonitor;
    
    private NBJobHandler(WdssiiJob job) {
       myWdssiiJob = job;
    }
    
    /** Factory for creating an NBJobHandler */
    public static class NBJobFactory implements WdssiiJobFactory {

        @Override
        public WdssiiJobRunner createJobRunner(WdssiiJob job) {
            NBJobHandler realJob = new NBJobHandler(job);
            return realJob;
        }
    }

    public static class NBJobMonitor implements WdssiiJobMonitor{

        private ProgressHandle myProgressHandle;
        private int myTotalUnits = 0;
        private int myCurrentUnits = 0;
        private boolean myIsStarted = false;
        private final Object myStartSync = new Object();
        
        private NBJobMonitor(WdssiiJob job) {
            myProgressHandle = ProgressHandleFactory.createHandle(job.getName());
        }
        
        @Override
        public void done() {
           synchronized(myStartSync){
                if (myIsStarted){
                    if (myTotalUnits != -1){
                        myProgressHandle.progress(myTotalUnits);
                    }
                  myProgressHandle.finish();
                }
            }
        }

        @Override
        public void beginTask(String taskName, int totalUnits) {
            myTotalUnits = totalUnits;
            synchronized(myStartSync){
                if (myTotalUnits == -1){
                    myProgressHandle.start();
                    myProgressHandle.switchToIndeterminate();
                }else{
                    myProgressHandle.start(totalUnits);
                }
                myIsStarted = true;
            }
        }

        @Override
        public void subTask(String subTaskName) {
            myProgressHandle.setDisplayName(subTaskName); 
        }

        @Override
        /** How how many is added, netbeans is the total so far */
        public void worked(int howMany) {
            myCurrentUnits += howMany;
            if (myTotalUnits == -1){
                // do anything here?
            }else{
                if (myCurrentUnits > myTotalUnits){
                    myCurrentUnits = myTotalUnits;
                }
                myProgressHandle.progress(myCurrentUnits);
            }
        }

        @Override
        public boolean isCanceled() {
            return false; // @todo implement
        }
        
        public void cancel(){
            synchronized(myStartSync){
                if (myIsStarted){
                  myProgressHandle.finish();
                  myIsStarted = false;
                }
            }
        }
        
    }
    
    @Override
    public void WdssiiStartJob() {
        
        // For the moment, just make a simple worker thread for it,
        // we should use executor or something to make it more efficient
        // We'll have to add code to gui to update only during GUI thread,
        // just like in RCP...bleh.
        Runnable r = new Runnable(){
            @Override
            public void run() {
    
                try{
                  myWdssiiJobMonitor = new NBJobMonitor(myWdssiiJob);
                  myWdssiiJob.run(myWdssiiJobMonitor);
                }finally{
                    myWdssiiJobMonitor.done();
                }
            }
        };
        Thread t = new Thread(r, "WdssiiBackgroundTask");
        t.start();
    }

    @Override
    public void WdssiiCancelJob() {
        myWdssiiJobMonitor.cancel();
    }
}

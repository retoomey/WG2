package org.wdssii.gui;

import java.util.ArrayList;
import org.wdssii.core.CommandManager;
import org.wdssii.core.WdssiiJob;
import org.wdssii.gui.animators.Animator;
import org.wdssii.gui.animators.Mover3D;
import org.wdssii.gui.animators.TimeLooper;
import org.wdssii.gui.commands.AnimateCommand.AnimateNotifyCancelCommand;
import org.wdssii.gui.features.FeatureList;

/* Animate manager handles grouping all Animators...
 * TimeLooper handles looping in the display...
 *  
 * @author Robert Toomey
 *
 */
public class AnimateManager {

    public ArrayList<Animator> myAnimatorList = new ArrayList<Animator>();
    /** The number of frames in a product based loop (non-time) */
    private int myLoopFrames = 10;
    /** Sync object for number of frames */
    private final Object myLoopFramesSync = new Object();
    /** The RCP loop job that runs all the animator objects */
    private AnimatorJob myLoopJob = null;
    /** Sync object for the RCP loop job */
    private final Object myLoopJobSync = new Object();
    /** The single visual collection */
    private static AnimateManager myVisualCollection = new AnimateManager();

    /** Create a new visual collection containing the default animators */
    public AnimateManager() {
        // The basic animator list.  (Could be by reflection or xml)
        // No sync needed in constructor, only we have access to it
        myAnimatorList.add(new TimeLooper());
        myAnimatorList.add(new Mover3D());
    }

    // Not sure where the earth ball should be in design really..
    // probably will finalize when we create multiple views...
    // Since updating 'could' affect looping we'll put it here for now
    
    /** Update the drawing of the main earth ball */
    public static void updateDuringRender() {
        // FIXME: disable this update if we are looping...loop
        // will update us...
        FeatureList.getFeatureList().updateOnMinTime();
    }
    
    // Collection of animators
    public static AnimateManager getVisualCollection() {
        return myVisualCollection;
    }
    
    /** Get the list of animators that show up in the animation view */
    public ArrayList<Animator> getAnimatorList() {
        return myAnimatorList;
    }

    /** Get the number of loop frames in a product based loop (non-time) */
    public int getLoopFrames() {
        synchronized (myLoopFramesSync) {
            return myLoopFrames;
        }
    }

    /** Set the number of loop frames in a product based loop (non-time) */
    public void setLoopFrames(int frames) {
        synchronized (myLoopFramesSync) {
            myLoopFrames = frames;
        }
    }

    /** Toggle the animation state on or off depending on current state */
    public void toggleLoopEnabled() {
        synchronized (myLoopJobSync) {
            setLoopEnabled(myLoopJob == null);
        }
    }

    /** Turn on the global 'animate' mode which will loop all active animators */
    public void setLoopEnabled(boolean flag) {
        synchronized (myLoopJobSync) {
            if (!flag && (myLoopJob != null)) {
                myLoopJob.cancel();
                myLoopJob = null;
            } else if (flag) {
                setUpJobIfNeeded();
            }
        }
    }

    public boolean getLoopEnabled() {
        synchronized (myLoopJobSync) {
            return (myLoopJob != null);
        }
    }

    /** Start the background RCP job if it needs to be started */
    public void setUpJobIfNeeded() {
        synchronized (myLoopJobSync) {
            if (myLoopJob == null) {

                // Create a new job for doing looping....
                myLoopJob = new AnimatorJob(this, "Animating");
                myLoopJob.schedule();
            }
        }
    }

    public int getMinDwellMS() {
        int minDwell = -1; // ms
        for (Animator a : myAnimatorList) {
            int aMinDwell = a.getMinDwellMS();
            if (minDwell == -1) {
                minDwell = aMinDwell;
            }  // I know there will always be at least one animator
            if (aMinDwell < minDwell) {
                minDwell = aMinDwell;
            }
        }
        return minDwell;
    }

    /** Called from job thread 
     * With a single 'dwell' how do we animate everything nicely?  Crap...*/
    public int animate(int lastDwell) {
        int minDwell = -1;
        for (Animator a : myAnimatorList) {  // Ok not to sync since currently only add to list in constructor
            boolean enabled = a.isEnabled();
            if (enabled) {
                boolean need = a.needToAnimate(lastDwell);
                if (need) {
                    int aDwell = a.animate();
                    a.setDwellTime(aDwell);
                    if (minDwell == -1) {
                        minDwell = aDwell;
                    }
                    if (aDwell < minDwell) {
                        minDwell = aDwell;
                    }
                }
            }
        }
        return minDwell; // Let job know the minimum time to dwell
    }

    public static class AnimatorJob extends WdssiiJob {

        private int myDwell = 0;
        private AnimateManager myVisualCollection = null;

        public AnimatorJob(AnimateManager v, String name) {
            super(name);
            myVisualCollection = v;
        }

        /** The true dwell of the job, which should be the min of any dwell required by all animators */
        //public void setJobDwell(int dwell){
        //	myDwell = dwell;
        //}
        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {

            boolean wasCancelled = false;
            monitor.beginTask("", WdssiiJobMonitor.UNKNOWN); // IProgressMonitor.UNKNOWN			
            while (true) {  // Work forever until canceled.  It's ok, we sleep in the thread to yield.

                // Pass 'old' dwell to animate
                myDwell = 100;
                myVisualCollection.animate(myDwell);  // thread safety?  FIXME
                monitor.worked(1);

                // Yield for at least the dwell time.
                try {
		    // Not sure how I can't call this in a loop, some warnings are silly
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }

                if (monitor.isCanceled()) {
                    wasCancelled = true;
                    break;
                }
            }
            if (wasCancelled) {
                myVisualCollection.setLoopEnabled(false);
                CommandManager.getInstance().executeCommand(new AnimateNotifyCancelCommand(), true);
            }
            return WdssiiJobStatus.OK_STATUS;
        }
    }
}

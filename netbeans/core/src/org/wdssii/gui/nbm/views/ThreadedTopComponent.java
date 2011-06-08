package org.wdssii.gui.nbm.views;

import javax.swing.SwingUtilities;
import org.openide.windows.TopComponent;

/**
 * A ThreadedTopComponent only calls update within the Swing thread.
 * We run a lot of threads so sometimes we call updates from a different thread.
 * This tries to make it safer.
 * @author Robert Toomey
 */
public class ThreadedTopComponent extends TopComponent {

    /** The thread name of the swing thread, whatever it is */
    private static String SWING_THREAD;

    /** Get the swing thread name */
    public static String getSwingThreadName() {
        if (SWING_THREAD == null) {
            Thread t = Thread.currentThread();
            SWING_THREAD = t.getName();
        }
        return SWING_THREAD;
    }

    /** Called to update the GUI of this TopComponent */
    public final void updateGUI(Object anyInfoWanted) {
        Thread t = Thread.currentThread();
        String currentName = t.getName();
        if (currentName.equals(SWING_THREAD)) {
            updateInSwingThread(anyInfoWanted);
        } else {
            // Invoke it later in the swing thread....
            // FIXME: limit to one queued update?  Tricky.
            final Object info = anyInfoWanted;
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    updateInSwingThread(info);
                }
            });
        }
    }
    
    public final void updateGUI(){
        updateGUI(null);
    }

    /** Override to update GUI objects within the TopComponent */
    public void updateInSwingThread(Object info) {
    }
}

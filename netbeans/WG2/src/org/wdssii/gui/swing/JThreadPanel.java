package org.wdssii.gui.swing;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A JThreadPanel only calls update within the Swing thread.
 * We run a lot of threads so sometimes we call updates from a different thread.
 * This tries to make it safer.
 * 
 * Right now I call updateGUI with some extra object info..this is passed
 * onto updateInSwingThread.  This is the method to override to
 * fill in contained widget information.
 * 
 * TODO: Could make this 'smarter' and override the standard refresh, etc.
 * in order to make a fully thread-safe widget.
 * 
 * @author Robert Toomey
 */
public abstract class JThreadPanel extends JPanel {
   /** The thread name of the swing thread, whatever it is */
    private String SWING_THREAD;

    public JThreadPanel(){
        // Assume we are created within the swing thread.
        getSwingThreadName();
    }
    
    /** Get the swing thread name */
    public final String getSwingThreadName() {
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
    public abstract void updateInSwingThread(Object info);
}

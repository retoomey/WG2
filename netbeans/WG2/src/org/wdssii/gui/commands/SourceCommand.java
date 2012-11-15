package org.wdssii.gui.commands;

import org.wdssii.gui.commands.SourceAddCommand.SourceAddParams;
import org.wdssii.gui.sources.Source;
import org.wdssii.gui.sources.SourceList;

/**
 * The base for any SourceCommand
 *
 * @author Robert Toomey
 */
public abstract class SourceCommand extends DataCommand {

    /**
     * Only one global source list in display
     */
    protected SourceList myList = SourceList.theSources;
    /**
     * The index key for affected index, if any
     */
    public String mySourceKey = null;

    /**
     * Get the index key for this command
     */
    public String getSourceKey() {
        return mySourceKey;
    }

    /**
     * Set the index key for this command
     */
    public void setSourceKey(String s) {
        mySourceKey = s;
    }

    /**
     * Do we have a valid index or selected source?
     */
    protected boolean validSourceOrSelected() {
        String name = getSourceKey();
        if (name == null) {
            Source s = myList.getTopSelected();
            name = s.getKey();
        }
        if (name != null) {
            setSourceKey(name);
        }
        return (name != null);
    }

    // Private method access (for subclasses) ----------------------------
    /**
     * Remove an index
     */
    protected void removeSource(String name) {
        myList.removeSource(name);
    }

    /**
     * Add an index
     */
    protected String add(SourceAddParams params) {
        // Don't allow a duplicate URL to be added.  We assume every
        // source has a unique URL path...
        params.message = "";
        Source old = myList.getSourceURL(params.getSourceURL());
        if (old == null) {
            Source s = params.createSource();
            myList.addSource(s);
            return s.getKey();
        } else {
            params.message = "Already have this source";
            return null;
        }
    }

    /**
     * Called before a connect, so that we can update GUI to show connecting
     * statuses
     */
    protected boolean aboutToConnect(String keyName, boolean start) {
        return myList.aboutToConnect(keyName, start);
    }

    protected boolean connect(String keyName) {
        return myList.connectSource(keyName);
    }

    protected void disconnect(String keyName) {
        myList.disconnectSource(keyName);
    }
}
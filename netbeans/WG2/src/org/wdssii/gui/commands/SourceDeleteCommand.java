package org.wdssii.gui.commands;

import java.util.ArrayList;

//import org.eclipse.jface.dialogs.IDialogConstants;
//import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import org.wdssii.gui.PreferencesManager;
import org.wdssii.gui.SourceManager;
import org.wdssii.gui.PreferencesManager.PrefConstants;
import org.wdssii.index.IndexWatcher;

/** Called by name from WdssiiDynamic
 * 
 * The 'delete source' button or menu in the GUI
 * @author Robert Toomey
 */
public class SourceDeleteCommand extends SourceClearCommand {

    /** Called by name from WdssiiDynamic.
     * We need an object for direct calling from a button/menu, but the 'work' will go to the
     * regular delete command
     * FIXME: might be better to use DeleteCommand and pass a RCP optional parameter */
    public static class SourceDeleteAllCommand extends SourceDeleteCommand {

        @Override
        public boolean execute() {
            return (deleteAllSources());
        }
    }

    /** Called from rcp button click, this means disconnect the selected source */
    public SourceDeleteCommand() {
    }

    /** Called by other code */
    public SourceDeleteCommand(String key) {
        setIndexName(key);
    }

    /** Delete all sources from display */
    protected boolean deleteAllSources() {
        boolean myUserConfirm = true;  // FIXME: for scripting should be false
        boolean doJob = true;

        // Remember if user wants dialog to show on add or not...
        PreferencesManager p = PreferencesManager.getInstance();
        boolean showDialog = p.getBoolean(PrefConstants.PREF_showDeleteALLCommandDialog);
        if (myUserConfirm && showDialog) {
            JCheckBox checkbox = new JCheckBox("Do not show this message again.");
            String message = "Delete all sources and products in display?";
            Object[] params = {message, checkbox};
            int n = JOptionPane.showConfirmDialog(null, params, "Delete all sources", JOptionPane.YES_NO_OPTION);
            boolean dontShow = checkbox.isSelected();
            if (n == 0) { // Yes
                doJob = true;
            } else {
                doJob = false;
            }
            p.setValue(PrefConstants.PREF_showDeleteALLCommandDialog, !dontShow); 
            
        }
        if (doJob) {
            // Get copy of current list...(iteration shouldn't get the ConcurrentModificationError)
            // This is important since we're spawning jobs that delete these
            ArrayList<IndexWatcher> list = SourceManager.getInstance().getIndexList();
            for (IndexWatcher w : list) {
                deleteSingleSource(w.getKeyName(), false);
            }
        }
        return doJob; // if job was done, fire GUI update
    }

    /** Delete a single source (either get the current selection or index name setting) */
    protected boolean deleteSingleSource(String toDelete, boolean userConfirmAllowed) {
        boolean doJob = true;

        // Remember if user wants dialog to show on add or not...
        PreferencesManager p = PreferencesManager.getInstance();
        boolean showDialog = p.getBoolean(PrefConstants.PREF_showDeleteCommandDialog);
        if (userConfirmAllowed && showDialog) {

            String niceName = SourceManager.getInstance().getNiceShortName(toDelete);
            JCheckBox checkbox = new JCheckBox("Do not show this message again.");
            String message = "Disconnect/Remove source " + niceName + "?";
            Object[] params = {message, checkbox};
            int n = JOptionPane.showConfirmDialog(null, params, "Remove source", JOptionPane.YES_NO_OPTION);
            boolean dontShow = checkbox.isSelected();
            if (n == 0) { // Yes
                doJob = true;
            } else {
                doJob = false;
            }
            p.setValue(PrefConstants.PREF_showDeleteCommandDialog, !dontShow);
        }
        if (doJob) {

            // Clear all products matching us...
            super.execute();

            // But we also remove the source completely from display....
            removeIndexKey(toDelete);
        }

        return doJob; // if job was done, fire GUI update
    }

    @Override
    public boolean execute() {
        boolean success = false;
        if (validIndexNameOrSelected()) {
            String toDelete = getIndexName();
            success = deleteSingleSource(toDelete, true);
        }
        return success;
    }
}

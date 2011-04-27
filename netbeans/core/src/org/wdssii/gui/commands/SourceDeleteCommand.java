package org.wdssii.gui.commands;

import java.util.ArrayList;

//import org.eclipse.jface.dialogs.IDialogConstants;
//import org.eclipse.jface.dialogs.MessageDialogWithToggle;
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
        /*boolean myUserConfirm = true;  // FIXME: for scripting should be false
        boolean doJob = true;
        
        // Remember if user wants dialog to show on add or not...
        PreferencesManager p = PreferencesManager.getInstance();
        boolean showDialog = p.getBoolean(PrefConstants.PREF_showDeleteALLCommandDialog);
        if (myUserConfirm && showDialog){
        
        MessageDialogWithToggle dlg = MessageDialogWithToggle.openOkCancelConfirm(
        null, "Confirm source(s) deletion", 
        "Delete all sources and products in display?", 
        "Don't show this message next time", false, null, null);
        if (dlg.getReturnCode() != IDialogConstants.OK_ID){
        doJob = false;
        }else{
        p.setValue(PrefConstants.PREF_showDeleteALLCommandDialog, !dlg.getToggleState());
        }
        }
        if (doJob){				
        // Get copy of current list...(iteration shouldn't get the ConcurrentModificationError)
        // This is important since we're spawning jobs that delete these
        ArrayList<IndexWatcher> list = SourceManager.getInstance().getIndexList();
        for(IndexWatcher w: list){
        System.out.println("DELETING "+w.getKeyName());
        deleteSingleSource(w.getKeyName(), false);
        }
        }
        return doJob; // if job was done, fire GUI update
         */
        return false;

    }

    /** Delete a single source (either get the current selection or index name setting) */
    protected boolean deleteSingleSource(String toDelete, boolean userConfirmAllowed) {
        //boolean myUserConfirm = true;  // FIXME: for scripting should be false
		/*boolean doJob = true;
        
        // Remember if user wants dialog to show on add or not...
        PreferencesManager p = PreferencesManager.getInstance();
        boolean showDialog = p.getBoolean(PrefConstants.PREF_showDeleteCommandDialog);
        if (userConfirmAllowed && showDialog){
        
        String niceName = SourceManager.getInstance().getNiceShortName(toDelete);
        
        //doJob = MessageDialog.openConfirm(null, "Confirm source addition", 
        //	"Add and connect to source '"+getIndexName()+"'?");
        // getWindowConfigurator? FIXME
        MessageDialogWithToggle dlg = MessageDialogWithToggle.openOkCancelConfirm(
        null, "Confirm source deletion", 
        "Delete source and all products in display for '"+niceName+"'?", 
        "Don't show this message next time", false, null, null);
        if (dlg.getReturnCode() != IDialogConstants.OK_ID){
        doJob = false;
        }else{
        p.setValue(PrefConstants.PREF_showDeleteCommandDialog, !dlg.getToggleState());
        }
        }
        if (doJob){
        
        // Clear all products matching us...
        super.execute();
        
        // But we also remove the source completely from display....
        removeIndexKey(toDelete);
        }
        
        return doJob; // if job was done, fire GUI update
         * 
         */
        return false;
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

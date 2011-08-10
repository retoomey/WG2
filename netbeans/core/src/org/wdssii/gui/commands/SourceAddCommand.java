package org.wdssii.gui.commands;

//import org.eclipse.jface.dialogs.IDialogConstants;
//import org.eclipse.jface.dialogs.MessageDialog;
//import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.PreferencesManager;
import org.wdssii.gui.PreferencesManager.PrefConstants;

/**  Add a new source (and then spawn a connect to it)
 * @author Robert Toomey
 */
public class SourceAddCommand extends SourceClearCommand {

    private String myPath;
    private boolean myRealtime = false;
    private String myNiceName = null;
    private boolean myConnect = false;
    /** By default if user clicks a button, require a confirm dialog */
    private boolean myUserConfirm = true;

    /** Called directly if linked to button */
    public SourceAddCommand() {
    }

    /**
     * 
     * @param niceName The 'nice' name of the index, such as 'KTLX'.  User changable
     * @param path	The path such as "http://...."
     * @param needUserConfirm	Do we use user dialogs? (scripting turns this off)
     * @param realtime	Is this a realtime index?  (requires a socket connection)
     */
    public SourceAddCommand(String niceName, String path, boolean needUserConfirm, boolean realtime, boolean connect) {
        //setIndexName(key);
        myNiceName = niceName;
        myUserConfirm = needUserConfirm;
        myPath = path;
        myRealtime = realtime;
        myConnect = connect;
    }

    @Override
    public boolean execute() {


        boolean updateGUI = false;
        boolean doJob = true;
        if (myPath != null) {

            // Remember if user wants dialog to show on add or not...
            PreferencesManager p = PreferencesManager.getInstance();
            boolean showDialog = p.getBoolean(PrefConstants.PREF_showAddCommandDialog);
            if (myUserConfirm && showDialog) {
                /*  // getWindowConfigurator? FIXME
                MessageDialogWithToggle dlg = MessageDialogWithToggle.openOkCancelConfirm(
                null, "Confirm source addition", 
                "Add and connect to source '"+myNiceName+"'?", 
                "Don't show this message next time", false, null, null);
                if (dlg.getReturnCode() != IDialogConstants.OK_ID){
                doJob = false;
                }else{
                p.setValue(PrefConstants.PREF_showAddCommandDialog, !dlg.getToggleState());
                }*/
            }
            if (doJob) {

                // FIXME: flag true or false or what?
                String newKey = add(myNiceName, myPath, myRealtime);  // Don't lag here.
                updateGUI = true;  // Add needs a 'unconnected' icon and name in list.
                if (myUserConfirm) {
                    if (newKey != null) {
                        //	MessageDialog.openInformation(null, "Add success", "Add was successful");
                    } else {
                       // MessageDialog.openInformation(null, "Add failure", "Add failed");
                    }
                }

                // Pass on to the connect command...
                if (newKey != null) {

                    if (myConnect) {
                        // Spawns worker thread....
                        SourceConnectCommand c = new SourceConnectCommand(newKey);
                        CommandManager.getInstance().executeCommand(c, false);
                    }
                }

            }
        }
        // This will update the list to show the 'unconnected' icon (job probably still running)
        // After the job is done, another fire will update again...
        // Or job already completed and we just update twice..either way ok...

        return updateGUI;
    }
}

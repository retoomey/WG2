package org.wdssii.gui.commands;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
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
    private boolean myUserReport = true;
    
    /** Root window that called this command.  Used by dialogs to link
     * to proper caller window.  Important for multiple displays
     */
    private JComponent myRoot = null;
    
    /**
     * 
     * @param niceName The 'nice' name of the index, such as 'KTLX'.  User changable
     * @param path	The path such as "http://...."
     * @param realtime	Is this a realtime index?  (requires a socket connection)
     */
    public SourceAddCommand(String niceName, String path, boolean realtime, boolean connect) {
        myNiceName = niceName;
        myUserConfirm = false;
        myUserReport = false;
        myPath = path;
        myRealtime = realtime;
        myConnect = connect;
    }
    
       /**
     * 
     * @param niceName The 'nice' name of the index, such as 'KTLX'.  User changable
     * @param path	The path such as "http://...."
     * @param needUserConfirm	Do we use user dialogs? (scripting turns this off)
     * @param realtime	Is this a realtime index?  (requires a socket connection)
     */
    public SourceAddCommand(JComponent root, String niceName, String path, boolean needUserConfirm,
            boolean needUserReport, boolean realtime, boolean connect) {
        myRoot = root;
        myNiceName = niceName;
        myUserConfirm = needUserConfirm;
        myUserReport = needUserReport;
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
                JCheckBox checkbox = new JCheckBox("Do not show this message again.");
                String message = "Add and connect to source '" + myNiceName + "'?";
                Object[] params = {message, checkbox};
                int n = JOptionPane.showConfirmDialog(myRoot, params, "Confirm source addition", JOptionPane.YES_NO_OPTION);
                boolean dontShow = checkbox.isSelected();
                if (n == 0) { // Yes
                    doJob = true;
                } else {
                    doJob = false;
                }
                p.setValue(PrefConstants.PREF_showAddCommandDialog, !dontShow);

            }
            if (doJob) {

                String newKey = add(myNiceName, myPath, myRealtime);  // Don't lag here.
                updateGUI = true;  // Add needs a 'unconnected' icon and name in list.
                if (myUserReport) {
                    if (newKey != null) {
                        JOptionPane.showMessageDialog(myRoot, "Add was successful",
                                "Add success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(myRoot, "Add not successful",
                                "Add failure", JOptionPane.ERROR_MESSAGE);
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

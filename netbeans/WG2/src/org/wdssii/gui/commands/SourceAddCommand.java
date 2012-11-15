package org.wdssii.gui.commands;

import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.PreferencesManager;
import org.wdssii.gui.PreferencesManager.PrefConstants;
import org.wdssii.gui.sources.IndexSource;
import org.wdssii.gui.sources.Source;
import org.wdssii.gui.sources.WMSSource;

/**
 * Add a new source (and then spawn a connect to it)
 *
 * @author Robert Toomey
 */
public class SourceAddCommand extends SourceClearCommand {

    /**
     * Info needed to add a source, passed to Source such as IndexSource
     */
    public static abstract class SourceAddParams {

        private String niceName;
        private URL sourceURL;
        private boolean connect;
        public JComponent rootWindow = null; // root for any dialogs
        public String message;
        
        public SourceAddParams(String aNiceName, URL aSourceURL, boolean aConnect) {
            niceName = aNiceName;
            sourceURL = aSourceURL;
            connect = aConnect;
        }

        public SourceAddParams(String aNiceName, String path, boolean aConnect) throws MalformedURLException {
            niceName = aNiceName;
            connect = aConnect;
            URL aSourceURL = new URL(path);
            sourceURL = aSourceURL;
        }

        public String getNiceName() {
            return niceName;
        }

        public URL getSourceURL() {
            return sourceURL;
        }

        public boolean getConnect() {
            return connect;
        }

        public abstract Source createSource();
    }

    /**
     * WDSSII source index... FIXME: eventually move somewhere else
     */
    public static class IndexSourceAddParams extends SourceAddParams {

        public boolean realTime = false;
        public int history = IndexSource.HISTORY_ARCHIVE;

        public IndexSourceAddParams(String aNiceName, URL aSourceURL, boolean aRealtime, boolean aConnect, int aHistory) {
            super(aNiceName, aSourceURL, aConnect);
            realTime = aRealtime;
            history = aHistory;
        }

        public IndexSourceAddParams(String aNiceName, String path, boolean aRealtime, boolean aConnect, int aHistory) throws MalformedURLException {
            super(aNiceName, path, aConnect);
            realTime = aRealtime;
            history = aHistory;
        }

        @Override
        public Source createSource() {
            return new IndexSource(getNiceName(), getSourceURL(), history);
        }
    }

    public static class WMSSourceAddParams extends SourceAddParams {

        public WMSSourceAddParams(String aNiceName, URL aSourceURL, boolean connect) {
            super(aNiceName, aSourceURL, connect);
        }

        public WMSSourceAddParams(String aNiceName, String path, boolean connect) throws MalformedURLException {
            super(aNiceName, path, connect);
        }

        @Override
        public Source createSource() {
            return new WMSSource(getNiceName(), getSourceURL());
        }
    }
    private static Logger log = LoggerFactory.getLogger(SourceAddCommand.class);
    private SourceAddParams myParams;
    /**
     * By default if user clicks a button, require a confirm dialog
     */
    private boolean myUserConfirm = false;
    private boolean myUserReport = false;

    /**
     *
     * @param niceName The 'nice' name of the index, such as 'KTLX'. User
     * changable
     * @param path	The path such as "http://...."
     * @param realtime	Is this a realtime index? (requires a socket connection)
     */
    public SourceAddCommand(SourceAddParams params) {
        myParams = params;
    }

    /**
     * Set if dialogs show up or not
     */
    public void setConfirmReport(boolean c, boolean r, JComponent root) {
        myUserConfirm = c;
        myUserReport = r;
        myParams.rootWindow = root;
    }

    @Override
    public boolean execute() {

        boolean updateGUI = false;
        boolean doJob = true;
        if (myParams.sourceURL != null) {

            // Remember if user wants dialog to show on add or not...
            PreferencesManager p = PreferencesManager.getInstance();
            boolean showDialog = p.getBoolean(PrefConstants.PREF_showAddCommandDialog);
            if (myUserConfirm && showDialog) {
                JCheckBox checkbox = new JCheckBox("Do not show this message again.");
                String message = "Add and connect to source '" + myParams.niceName + "'?";
                Object[] params = {message, checkbox};
                int n = JOptionPane.showConfirmDialog(myParams.rootWindow, params, "Confirm source addition", JOptionPane.YES_NO_OPTION);
                boolean dontShow = checkbox.isSelected();
                if (n == 0) { // Yes
                    doJob = true;
                } else {
                    doJob = false;
                }
                p.setValue(PrefConstants.PREF_showAddCommandDialog, !dontShow);

            }
            if (doJob) {

                // String newKey = add(myParams.niceName, myParams.sourceURL, myParams.realTime, myParams.history);  // Don't lag here.
                String newKey = add(myParams);
                updateGUI = true;  // Add needs a 'unconnected' icon and name in list.
                if (myUserReport) {
                    if (newKey != null) {
                        JOptionPane.showMessageDialog(myParams.rootWindow, "Add was successful",
                                "Add success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(myParams.rootWindow, myParams.message,
                                "Add failure", JOptionPane.ERROR_MESSAGE);
                    }
                }

                // Pass on to the connect command...
                if (newKey != null) {

                    if (myParams.connect) {
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

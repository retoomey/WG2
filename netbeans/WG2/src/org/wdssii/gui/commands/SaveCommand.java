package org.wdssii.gui.commands;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JFileChooser;
import static javax.swing.JFileChooser.SAVE_DIALOG;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.wdssii.core.WdssiiCommand;
import org.wdssii.gui.PreferencesManager;

/**
 *
 * @author Robert Toomey
 */
public class SaveCommand extends WdssiiCommand {

    boolean myAs = false;

    public int myReturn = -1;
    
    public SaveCommand(boolean as) {
        super();
        myAs = as;
    }

    @Override
    public boolean execute() {
        if (myAs) {
            doExportSettingsDialog();
        } else {
            URL u = PreferencesManager.getInstance().getDocumentPath();
            if (u == null) {
                doExportSettingsDialog();
            }else{
                PreferencesManager.getInstance().saveDocument();
                myReturn = JFileChooser.APPROVE_OPTION;
            }
        }
        return true;
    }

    /**
     * Code duplication, need general utility somewhere. Probably need to
     * decouple commands from Swing anyway...
     */
    private static class mySaveChooser extends JFileChooser {

        @Override
        public void approveSelection() {
            File f = getSelectedFile();
            if (f.exists() && getDialogType() == SAVE_DIALOG) {
                int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file", JOptionPane.YES_NO_CANCEL_OPTION);
                switch (result) {
                    case JOptionPane.YES_OPTION:
                        super.approveSelection();
                        return;
                    case JOptionPane.NO_OPTION:
                        return;
                    case JOptionPane.CANCEL_OPTION:
                        cancelSelection();
                        return;
                }
            }
            super.approveSelection();
        }
    }

    public void doExportSettingsDialog() {
        mySaveChooser fileopen = new mySaveChooser();
        fileopen.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String t = f.getName().toLowerCase();
                // FIXME: need to get these from the Builders
                return (f.isDirectory() || t.endsWith(".xml"));
            }

            @Override
            public String getDescription() {
                return "XML file";
            }
        });
        fileopen.setDialogTitle("Save WG2 XML document to...");
        myReturn = fileopen.showSaveDialog(getRootComponent());
        
        if (myReturn == JFileChooser.APPROVE_OPTION) {
            File file = fileopen.getSelectedFile();
            try {
                String temp = file.toString();
                if (!(temp.endsWith(".xml"))) {
                    file = new File(temp + ".xml");
                }
                URL aURL = file.toURI().toURL();
                // Tag root = SourceList.theSources.getTag();
                PreferencesManager.getInstance().saveAsDocument(aURL);
                OpenCommand.addDocument(aURL.toExternalForm());
                //if (root != null) {
                //     root.writeAsRoot(aURL);
                // }

            } catch (MalformedURLException ex) {
            }
        }
    }
}

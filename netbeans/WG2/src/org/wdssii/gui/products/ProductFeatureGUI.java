package org.wdssii.gui.products;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.writers.DataTypeWriter;
import org.wdssii.datatypes.writers.WriterFactory;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.swing.ProgressDialog;

/**
 * Create the GUI for a product feature
 *
 * @author Robert Toomey
 */
public class ProductFeatureGUI extends FeatureGUI {

    private final static Logger LOG = LoggerFactory.getLogger(ProductFeatureGUI.class);
    private ProductFeature myProductFeature;
    private SymbologyJPanel mySymbologyPanel;
    
    private JComponent myParent = null;

    public ProductFeatureGUI(ProductFeature p) {
        myProductFeature = p;
        setupComponents();
    }

    @Override
    public void updateGUI() {
    }

    @Override
    public void sendMessage(String message){
        if (message.equals("product")){
            if (mySymbologyPanel != null){
                mySymbologyPanel.updateProduct();
            }
        }
    }
     
    @Override
    public void activateGUI(JComponent parent) {
        parent.setLayout(new java.awt.BorderLayout());
        parent.add(this, java.awt.BorderLayout.CENTER);
        myParent = parent;
        doLayout();
    }

    @Override
    public void deactivateGUI() {
    }

    private void setupComponents() {

        setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));

        JPanel exportPanel = new JPanel();
        //exportPanel.setLayout(new MigLayout(new LC().fill().insetsAll("10"), null, null));
        JButton export = new JButton("Export ESRI Shp...");
        export.setToolTipText("Export data as ESRI file");
        add(export, new CC());
        export.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jExportActionPerformed(e);
            }
        });
        exportPanel.add(export, new CC());

        // FIXME: probably should create a tabbed pane that only creates the
        // swing interface of the selected tab only. (Save memory)
        JTabbedPane tabs = new JTabbedPane();
        mySymbologyPanel = new SymbologyJPanel(myProductFeature, this, "Symbology");
        tabs.addTab("Symbology", null, mySymbologyPanel, "Edit symbology");
        tabs.addTab("Export", null, exportPanel, "Export data functions");
        add(tabs, new CC().growX().growY());

    }

    public static class ProgressDialogJobMonitor implements WdssiiJob.WdssiiJobMonitor {

        private JDialog myRootDialog;
        private JFrame myRootFrame;
        private JComponent myLocation;
        private volatile ProgressDialog myDialog = null;
        private int myProgress = 0;
        private String myTaskName;
        private String myTitle;
        private int myTotalUnits = 0;

        /**
         * Called from GUI thread
         */
        public ProgressDialogJobMonitor(JDialog d, JComponent location, String title) {
            myRootDialog = d;
            myRootFrame = null;
            myLocation = location;
            myTitle = title;
        }

        public ProgressDialogJobMonitor(JFrame f, JComponent location, String title) {
            myRootDialog = null;
            myRootFrame = f;
            myLocation = location;
            myTitle = title;
        }

        public void createGUIIfNeeded() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    initInGUIThread();
                }
            });
        }

        public final void initInGUIThread() {
            if (myDialog == null) {  // Only we create and change value, so no sync needed...
                if (myRootDialog != null) {
                    myDialog = new ProgressDialog(myRootDialog, myLocation, myTitle);
                    myDialog.setVisible(true); // Have to call AFTER creation this locks this thread
                } else {
                    myDialog = new ProgressDialog(myRootFrame, myLocation, myTitle);
                    myDialog.setVisible(true); // Have to call AFTER creation this locks this thread
                }
            }
        }

        @Override
        public void done() {

            // Hide it....or possible show 'ok' 'error' messages, etc...
            ProgressDialog d = getDialog();
            if (d != null) {
                if (d.isVisible()) {
                    d.setVisible(false);
                }
            }
        }

        @Override
        public void beginTask(String taskName, int totalUnits) {
            createGUIIfNeeded();  // Non-locking separate 
            myTaskName = taskName;
            myTotalUnits = totalUnits;
        }

        @Override
        public void subTask(String subTaskName) {
            myTaskName = subTaskName;
        }

        public ProgressDialog getDialog() {
            ProgressDialog d = null;
            if (myDialog != null) {  // We only read value...
                d = myDialog;
            }
            return d;
        }

        @Override
        public void worked(int howMany) {

            myProgress += howMany;

            ProgressDialog d = getDialog();
            if (d != null) {
                if (d.isVisible()) {
                    d.setMinMax(1, myTotalUnits);
                    d.setProgress(myProgress);
                    d.setMessage(myTaskName);
                }
            }


        }

        @Override
        public boolean isCanceled() {
            return true;
        }

        @Override
        public void cancel() {
        }
    }

    public void jExportActionPerformed(ActionEvent e) {
        JFileChooser fileopen = new JFileChooser();
        fileopen.setDialogType(JFileChooser.SAVE_DIALOG);
        fileopen.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String t = f.getName().toLowerCase();
                return (f.isDirectory() || t.endsWith(".shp"));
            }

            @Override
            public String getDescription() {
                return "ERSI SHP File Format";
            }
        });
        fileopen.setDialogTitle("Export Shapefile");
        int ret = fileopen.showSaveDialog(myParent);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileopen.getSelectedFile();
            try {
                // Bim's format....
                URL aURL = file.toURI().toURL();
                //  LOG.debug("Would try to write to " + aURL.toString());
                if (myProductFeature != null) {
                    DataType d = myProductFeature.getLoadedDatatype();
                    if (d != null) {
                        LOG.debug("Create progress monitor ");
                        // ProgressMonitor m = new ProgressMonitor(this, "Test", "GOOP", 0, 1000);
                        // m.setProgress(500);
                        // m.setMillisToPopup(100);

                        Component something = (Component) SwingUtilities.getRoot(this);
                        ProgressDialogJobMonitor m;
                        String title = "Exporting ESRI .shp Data";
                        if (something instanceof JDialog) {
                            m = new ProgressDialogJobMonitor((JDialog) something, this, title);
                        } else {
                            // Assume JFrame....
                            m = new ProgressDialogJobMonitor((JFrame) something, this, title);
                        }

                        // Use factory to do it now...
                        DataTypeWriter writer =  WriterFactory.getWriter("ESRI");
                        writer.exportDataTypeToURL(d, aURL, null, m);
                           

                       // d.exportToESRI(aURL, m);
                    } else {
                        // warn or something?
                    }
                }
            } catch (MalformedURLException ex) {
            }
        }
    }
}

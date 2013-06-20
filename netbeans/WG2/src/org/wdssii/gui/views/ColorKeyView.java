package org.wdssii.gui.views;

import org.wdssii.core.CommandListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.net.URL;
import java.util.TreeMap;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.renderers.ColorMapRenderer;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.ProductManager.ProductDataInfo;
import org.wdssii.gui.commands.ProductCommand;
import org.wdssii.gui.swing.JColorMap;
import org.wdssii.gui.swing.JThreadPanel;

/**
 *  A View showing a color key
 * 
 * @author Robert Toomey
 */
public class ColorKeyView extends JThreadPanel implements CommandListener {

    public static final String ID = "ColorKeyView";

    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works
    // When sources or products change, update the navigation controls

    public void ProductCommandUpdate(ProductCommand command) {
        // Any loading/deleting of products, etc..can cause a change in the
        // product data info and filters, etc.
        updateGUI(command);
    }
    private JTable myTable;
    private DefaultTableModel myModel;
    private JColorMap myRenderer = new JColorMap();

    @Override
    public void updateInSwingThread(Object command) {
        updateTable();
        updateColorKey();
    }
    /** Our factory, called by reflection to populate menus, etc...*/
    public static class Factory extends WdssiiDockedViewFactory {
        public Factory() {
             super("ColorKey", "color_wheel.png");   
        }
        @Override
        public Component getNewComponent(){
            return new ColorKeyView();
        }
    }
    
    public ColorKeyView() {
        initComponents();
        initTable();
        myRenderer.setColorMapRenderer(new ColorMapRenderer());
        CommandManager.getInstance().addListener(ColorKeyView.ID, this);
    }

    public void initTable() {
        final JTable t = new javax.swing.JTable();
        myTable = t;
        final DefaultTableModel m = new DefaultTableModel();
        m.addColumn("Product");
        m.addColumn("C_URL");
        m.addColumn("I_URL");

        myModel = m;
        t.setModel(m);
        t.setFillsViewportHeight(
                true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        jColorKeyScrollPane.setViewportView(t);

        jColorKeyPanel.add(myRenderer, BorderLayout.CENTER);
        updateTable();
        updateColorKey();
    }

    public void updateTable() {
        myModel.setRowCount(0);

        TreeMap<String, ProductDataInfo> info = ProductManager.getInstance().getProductDataInfoSet();

        for (ProductDataInfo i : info.values()) {
            String n = i.getName();

            URL cmapURL = i.getCurrentColorMapURL();
            String cmap;
            if (cmapURL != null) {
                cmap = cmapURL.toString();
            } else {
                cmap = "?";
            }

            URL imapURL = i.getCurrentIconSetURL();
            String imap;
            if (imapURL != null) {
                imap = imapURL.toString();
            } else {
                imap = "?";
            }
            
            if (i.isLoaded()) {  // Don't load them all just for this list
                ColorMap c = i.getColorMap();
                if (c != null) {
                    String[] columns = {n, cmap, imap};
                    myModel.addRow(columns);
                }
            }
        }
    }

    public void updateColorKey() {
        ProductManager man = ProductManager.getInstance();
        myRenderer.setColorMap(man.getCurrentColorMap());
    }
                      
    private void initComponents() {

        jColorKeyScrollPane = new javax.swing.JScrollPane();
        jColorKeyPanel = new javax.swing.JPanel();
        jSnapColorMap = new javax.swing.JButton();

        jColorKeyPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jColorKeyPanel.setLayout(new java.awt.BorderLayout());

       // org.openide.awt.Mnemonics.setLocalizedText(jSnapColorMap, org.openide.util.NbBundle.getMessage(ColorKeyTopComponent.class, "ColorKeyTopComponent.jSnapColorMap.text")); // NOI18N
        jSnapColorMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSnapColorMapActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSnapColorMap, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
            .addComponent(jColorKeyPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
            .addComponent(jColorKeyScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jSnapColorMap)
                .addGap(8, 8, 8)
                .addComponent(jColorKeyScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jColorKeyPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }                  

    private void jSnapColorMapActionPerformed(java.awt.event.ActionEvent evt) {                                              
        String theFileName = doImageSaveDialog();
        if (!theFileName.isEmpty()) {
            String success = myRenderer.paintToFile(theFileName);
            if (success.isEmpty()) {
                success = "Wrote file name " + theFileName;
            }
            JOptionPane.showMessageDialog(this, success,
                    "Image:" + theFileName, JOptionPane.PLAIN_MESSAGE);
        }
    }                                             
    // Variables declaration - do not modify                     
    private javax.swing.JPanel jColorKeyPanel;
    private javax.swing.JScrollPane jColorKeyScrollPane;
    private javax.swing.JButton jSnapColorMap;
    // End of variables declaration                   

    /** Filter to looks for local data files.  We can make this more 
     * advanced
     */
    private static class ImageDataFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            String l = f.getName().toLowerCase();
            if (l.endsWith(".png")
                    || (l.endsWith(".gif"))
                    || (l.endsWith(".jpg"))
                    || (l.endsWith(".bmp"))) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "png, gif, jpg, bmp image files";
        }
    }

    // Simple overwrite confirmation dialog.
    // FIXME: make general utility for this
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

    /** Bring up a dialog for saving a new image file */
    public String doImageSaveDialog() {

        String pickedFile = null;
        JFileChooser chooser = new mySaveChooser();
        chooser.setFileFilter(new ImageDataFilter());
        chooser.setDialogTitle("Save colormap image file");
        int returnVal = chooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            pickedFile = chooser.getSelectedFile().getAbsolutePath();

            int dot = pickedFile.lastIndexOf('.');
            String type = pickedFile.substring(dot + 1);
            if ((type.equals(pickedFile)) || (type.isEmpty())) {
                pickedFile += ".png";
            }
        }
        return pickedFile;
    }
}

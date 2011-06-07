/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wdssii.gui.nbm.views;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.wdssii.gui.LLHAreaManager.VolumeTableData;
import org.wdssii.gui.commands.LLHAreaCreateCommand;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.gui.LLHAreaManager;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;

/**
 * LLHAreaTopComponent
 * 
 * Displays the list of 3D objects in the earth window
 * 
 */
@ConvertAsProperties(dtd = "-//org.wdssii.gui.nbm.views//LLHArea//EN",
autostore = false)
@TopComponent.Description(preferredID = "LLHAreaTopComponent",
iconBase = "org/wdssii/gui/nbm/views/brick_add.png",
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.wdssii.gui.nbm.views.LLHAreaTopComponent")
@ActionReference(path = "Menu/Window/WDSSII" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_LLHAreaAction",
preferredID = "LLHAreaTopComponent")
public final class LLHAreaTopComponent extends TopComponent {

    /** The object 3D list shows the list of 3d objects in the window
     */
    private Object3DListTableModel myObject3DListTableModel;
    private JTable jObjects3DListTable;

    /** Storage for the current product list */
    private static class Objects3DTableData {

        public String visibleName; // Name shown in list
        public String keyName; // The key used to select this handler
        public boolean checked;
        public boolean onlyMode;
        public String type;
        public String timeStamp;
        public String subType;
        public String message;
    }

    private class Object3DListTableModel extends AbstractTableModel {

        /** The column headers */
        private final String headers[];
        private ArrayList<Objects3DTableData> myDataTypes;
        public static final int OBJ_VISIBLE = 0;
        public static final int OBJ_ONLY = 1;
        public static final int OBJ_NAME = 2;
        public static final int OBJ_MESSAGE = 3;

        public Object3DListTableModel() {

            this.headers = new String[]{
                "Visible", "Only", "Name", "Message"
            };
        }

        @Override
        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public int getRowCount() {
            int size = 0;
            if (myDataTypes != null) {
                size = myDataTypes.size();
            }
            return size;
        }

        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return Objects3DTableData.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int column) {
            if (myDataTypes != null) {
                if (rowIndex < myDataTypes.size()) {
                    return myDataTypes.get(rowIndex);
                }
            }
            return null;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
        }

        public void setDataTypes(ArrayList<Objects3DTableData> n) {
            myDataTypes = n;
            // Wow causes a null pointer exception in Swing...probably
            // because of changing the data out on the fly.  Just call
            // table.repaint after setDataTypes to force a full redraw.
            // this.fireTableDataChanged();
        }

        private Objects3DTableData getProductTableDataForRow(int row) {
            Objects3DTableData s = null;
            if (myDataTypes != null) {
                if ((row >= 0) && (row < myDataTypes.size())) {
                    s = myDataTypes.get(row);
                }
            }
            return s;
        }
    }

    /** Our custom renderer for our product view table */
    private static class Objects3DTableCellRenderer extends WG2TableCellRenderer {

        /** A shared JCheckBox for rendering every check box in the list */
        private JCheckBox checkbox = new JCheckBox();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info = "";
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof Objects3DTableData) {
                Objects3DTableData e = (Objects3DTableData) value;

                switch (trueCol) {

                    case Object3DListTableModel.OBJ_VISIBLE:
                        return getJCheckBox(table, e.checked, isSelected, cellHasFocus, row, col);
                    case Object3DListTableModel.OBJ_ONLY:
                        return getJCheckBox(table, e.onlyMode, isSelected, cellHasFocus, row, col);
                    case Object3DListTableModel.OBJ_NAME:
                        info = e.visibleName;
                        break;
                    case Object3DListTableModel.OBJ_MESSAGE:
                        info = e.message;
                        break;
                    default:
                        info = Integer.toString(trueCol) + "," + col;
                        break;
                }

                // For text...
                setText(info);
            } else {
                setText((String) (value));
            }
            return this;
        }
    }

    public LLHAreaTopComponent() {
        initComponents();
        initTable();
        setName(NbBundle.getMessage(LLHAreaTopComponent.class, "CTL_LLHAreaTopComponent"));
        setToolTipText(NbBundle.getMessage(LLHAreaTopComponent.class, "HINT_LLHAreaTopComponent"));

    }

    public void initTable() {
        myObject3DListTableModel = new Object3DListTableModel();
        jObjects3DListTable = new javax.swing.JTable();
        final JTable myTable = jObjects3DListTable;
        jObjects3DListTable.setModel(myObject3DListTableModel);
        final Object3DListTableModel myModel = myObject3DListTableModel;

        jObjects3DListTable.setFillsViewportHeight(true);
        jObjects3DListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jObjectScrollPane.setViewportView(jObjects3DListTable);

        Objects3DTableCellRenderer p = new Objects3DTableCellRenderer();
        jObjects3DListTable.setDefaultRenderer(Objects3DTableData.class, p);

        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();

        int count = myTable.getColumnCount();
        TableColumnModel cm = myTable.getColumnModel();
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            // Make all headers draw the same to be consistent.
            col.setHeaderRenderer(new IconHeaderRenderer());
            switch (i) {
                case 0:{
                    IconHeaderInfo info = new IconHeaderInfo("layervisible.png");
                    col.setHeaderValue(info);
                    // FIXME: this isn't right, how to do it with look + feel
                    col.setWidth(2 * d.width);
                    col.setMaxWidth(2 * d.width);
                    col.setResizable(false);
                }
                    break;
                 case 1: {
                    IconHeaderInfo info = new IconHeaderInfo("picture.png");
                    col.setHeaderValue(info);
                    // FIXME: this isn't right, how to do it with look + feel
                    col.setWidth(2 * d.width);
                    col.setMaxWidth(2 * d.width);
                    col.setResizable(false);
                 }
                    break;
            }
        }
        
        updateTable();
    }

    public void updateTable() {
        // FIXME: sync issue probably here, should copy, though right now
        // we only create/delete these from the GUI thread...so guess it's
        // safe enough for the moment.
        ArrayList<VolumeTableData> v = LLHAreaManager.getInstance().getVolumes();

        // We copy here
        ArrayList<Objects3DTableData> newList = new ArrayList<Objects3DTableData>();
        for (VolumeTableData d : v) {
            Objects3DTableData d2 = new Objects3DTableData();
            d2.visibleName = d.visibleName;
            d2.checked = d.checked;
            d2.keyName = d.keyName;
            d2.onlyMode = d.onlyMode;
            d2.message = d.message;
            newList.add(d2);
        }
        myObject3DListTableModel.setDataTypes(newList);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jObjectScrollPane = new javax.swing.JScrollPane();

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(LLHAreaTopComponent.class, "LLHAreaTopComponent.jButton1.text")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 305, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(85, Short.MAX_VALUE))
            .addComponent(jObjectScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jObjectScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        LLHAreaCreateCommand doit = new LLHAreaCreateCommand();
        CommandManager.getInstance().executeCommand(doit, true);
        // only place for now list can change:
        updateTable();
    }//GEN-LAST:event_jButton1ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JScrollPane jObjectScrollPane;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}

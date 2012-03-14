package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.MapGUI;
import org.wdssii.gui.commands.*;
import org.wdssii.gui.features.*;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.swing.RowEntryTable;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.RowEntryTableMouseAdapter;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;

public class FeaturesView extends JThreadPanel implements CommandListener {

    public static final String ID = "wdssii.FeaturesView";
    private static Logger log = LoggerFactory.getLogger(FeaturesView.class);

    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works
    // When sources or products change, update the navigation controls
    public void FeatureCommandUpdate(FeatureCommand command) {
        updateGUI(command);
    }

    @Override
    public void updateInSwingThread(Object command) {
        updateTable();
    }

    /**
     * Our factory, called by reflection to populate menus, etc...
     */
    public static class Factory extends WdssiiDockedViewFactory {

        public Factory() {
            super("Features", "brick_add.png");
        }

        @Override
        public Component getNewComponent() {
            return new FeaturesView();
        }
    }
    private FeatureListTableModel myFeatureListTableModel;
    private RowEntryTable jObjects3DListTable;
    private Feature myLastSelectedFeature = null;
    private javax.swing.JButton jNewMapButton;
    private javax.swing.JButton jNewVSliceButton;
    private javax.swing.JButton jNewPolarGridButton;
    private JPanel jFeatureGUIPanel;
    private javax.swing.JToolBar jEditToolBar;
    private javax.swing.JScrollPane jObjectScrollPane;
    private javax.swing.JScrollPane jControlScrollPane;

    /**
     * Storage for displaying the current feature list
     */
    private static class FeatureListTableData {

        public String visibleName; // Name shown in list
        public String group; // The feature group such as 'maps'
        public String keyName; // The key used to select this handler
        public boolean checked;
        public boolean onlyMode;
        public String type;
        public String timeStamp;
        public String subType;
        public String message;
    }

    private class FeatureListTableModel extends RowEntryTableModel<FeatureListTableData> {

        public static final int OBJ_VISIBLE = 0;
        public static final int OBJ_ONLY = 1;
        public static final int OBJ_NAME = 2;
        public static final int OBJ_GROUP = 3;
        public static final int OBJ_MESSAGE = 4;
        private boolean isRebuilding = false;

        public FeatureListTableModel() {
            super(FeatureListTableData.class, new String[]{
                        "Visible", "Only", "Name", "Type", "Message"
                    });
        }

        @Override
        public boolean rebuilding() {
            return isRebuilding;
        }

        @Override
        public void setRebuilding(boolean value) {
            isRebuilding = value;
        }
    }

    /**
     * Our custom renderer for drawing the table for the FeatureList
     */
    private static class FeatureListTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof FeatureListTableData) {
                FeatureListTableData e = (FeatureListTableData) value;

                switch (trueCol) {

                    case FeatureListTableModel.OBJ_VISIBLE:
                        return getJCheckBox(table, e.checked, isSelected, cellHasFocus, row, col);
                    case FeatureListTableModel.OBJ_ONLY:
                        return getJCheckBoxIcon(table, e.onlyMode, "picture.png", "pictures.png", isSelected, cellHasFocus, row, col);
                    case FeatureListTableModel.OBJ_NAME:
                        info = e.visibleName;
                        break;
                    case FeatureListTableModel.OBJ_MESSAGE:
                        info = e.message;
                        break;
                    case FeatureListTableModel.OBJ_GROUP:
                        info = e.group;
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

    public FeaturesView() {
        initComponents();
        initTable();

        CommandManager.getInstance().addListener(FeaturesView.ID, this);
    }

    private void initTable() {
        myFeatureListTableModel = new FeatureListTableModel();
        jObjects3DListTable = new RowEntryTable();
        final JTable myTable = jObjects3DListTable;
        jObjects3DListTable.setModel(myFeatureListTableModel);
        final FeatureListTableModel myModel = myFeatureListTableModel;

        jObjects3DListTable.setFillsViewportHeight(true);
        jObjects3DListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jObjectScrollPane.setViewportView(jObjects3DListTable);

        FeatureListTableCellRenderer p = new FeatureListTableCellRenderer();
        jObjects3DListTable.setDefaultRenderer(FeatureListTableData.class, p);

        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();

        int count = myTable.getColumnCount();
        TableColumnModel cm = myTable.getColumnModel();
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            // Make all headers draw the same to be consistent.
            col.setHeaderRenderer(new IconHeaderRenderer());
            switch (i) {
                case FeatureListTableModel.OBJ_VISIBLE: {
                    IconHeaderInfo info = new IconHeaderInfo("layervisible.png");
                    col.setHeaderValue(info);
                    // FIXME: this isn't right, how to do it with look + feel
                    col.setWidth(2 * d.width);
                    col.setMaxWidth(2 * d.width);
                    col.setResizable(false);
                }
                break;
                case FeatureListTableModel.OBJ_ONLY: {
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

        jObjects3DListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jObjects3DListTableValueChanged(e);
            }
        });

        jObjects3DListTable.addMouseListener(new RowEntryTableMouseAdapter(jObjects3DListTable, myModel) {

            class Item extends JMenuItem {

                private final FeatureListTableData d;

                public Item(String s, FeatureListTableData line) {
                    super(s);
                    d = line;
                }

                public FeatureListTableData getData() {
                    return d;
                }
            };

            @Override
            public JPopupMenu getDynamicPopupMenu(Object line, int row, int column) {

                // FIXME: Code a bit messy, we're just hacking the text value
                // for now.  Probably will need a custom JPopupMenu that has
                // our Objects3DTableData in it.
                ActionListener al = new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Item i = (Item) (e.getSource());
                        String text = i.getText();
                        if (text.startsWith("Delete")) {
                            FeatureDeleteCommand del = new FeatureDeleteCommand(i.getData().keyName);
                            CommandManager.getInstance().executeCommand(del, true);
                        }
                    }
                };
                JPopupMenu popupmenu = new JPopupMenu();
                FeatureListTableData entry = (FeatureListTableData) (line);
                String name = "Delete " + entry.visibleName;
                Item i = new Item(name, entry);
                popupmenu.add(i);
                i.addActionListener(al);
                return popupmenu;
            }

            @Override
            public void handleClick(Object stuff, int orgRow, int orgColumn) {

                if (stuff instanceof FeatureListTableData) {
                    FeatureListTableData entry = (FeatureListTableData) (stuff);

                    switch (orgColumn) {
                        case FeatureListTableModel.OBJ_VISIBLE: {
                            Feature f = FeatureList.theFeatures.getFeature(entry.keyName);
                            if (f != null) {
                                FeatureMemento m = f.getNewMemento();
                                m.setVisible(!entry.checked);
                                FeatureChangeCommand c = new FeatureChangeCommand(entry.keyName, m);
                                CommandManager.getInstance().executeCommand(c, true);
                            }
                        }
                        break;
                        case FeatureListTableModel.OBJ_ONLY: {
                            Feature f = FeatureList.theFeatures.getFeature(entry.keyName);
                            if (f != null) {
                                FeatureMemento m = f.getNewMemento();
                                m.setOnly(!entry.onlyMode);
                                FeatureChangeCommand c = new FeatureChangeCommand(entry.keyName, m);
                                CommandManager.getInstance().executeCommand(c, true);
                            }
                        }
                        break;
                        default:
                            break;
                    }
                }
            }
        });

        setUpSortingColumns();

        // updateTable();
    }
    // Disable for now since this does nothing yet

    /**
     * Set up sorting columns if wanted
     */
    private void setUpSortingColumns() {
    }

    private void jObjects3DListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        // We're in the updateTable and have set the selection to the old
        // value, we don't want to loop infinitely
        if (myFeatureListTableModel.rebuilding()) {
            return;
        }
        int row = jObjects3DListTable.getSelectedRow();
        if (row > -1) {
            int dataRow = jObjects3DListTable.convertRowIndexToModel(row);
            if (myFeatureListTableModel != null) {
                FeatureListTableData d = (FeatureListTableData) (myFeatureListTableModel.getDataForRow(dataRow));
                if (d != null) {
                    FeatureSelectCommand c = new FeatureSelectCommand(d.keyName);
                    CommandManager.getInstance().executeCommand(c, true);
                }
            }
        }
    }

    public void updateTable() {

        final FeatureList flist = FeatureList.theFeatures;

        /**
         * Static for now...
         */
        List<Feature> f = flist.getFeatures();

        int currentLine = 0;
        int select = -1;
        ArrayList<FeatureListTableData> newList = new ArrayList<FeatureListTableData>();
        Feature selectedFeature = null;
        Feature topFeature = flist.getTopSelected();

        for (Feature d : f) {
            FeatureListTableData d2 = new FeatureListTableData();
            d2.visibleName = d.getName();
            d2.group = d.getFeatureGroup();
            d2.checked = d.getVisible();  // methods allow internal locking
            d2.keyName = d.getKey();
            d2.onlyMode = d.getOnlyMode();
            d2.message = d.getMessage();
            newList.add(d2);
            if (topFeature == d) {
                select = currentLine;
                selectedFeature = d;
            }
            currentLine++;
        }
        myFeatureListTableModel.setDataTypes(newList);
        myFeatureListTableModel.fireTableDataChanged();

        if (select > -1) {
            select = jObjects3DListTable.convertRowIndexToView(select);

            // This of course fires an event, which calls jProductsListTableValueChanged
            // which would send a command which would do this again in an
            // infinite loop.  So we have a flag.  We don't use isAdjusting
            // because it still fires and event when you set it false
            myFeatureListTableModel.setRebuilding(true);
            jObjects3DListTable.setRowSelectionInterval(select, select);

            if (myLastSelectedFeature != selectedFeature) {
                jFeatureGUIPanel.removeAll();
                selectedFeature.setupFeatureGUI(jFeatureGUIPanel);
                jFeatureGUIPanel.validate();
                jFeatureGUIPanel.repaint();
                jControlScrollPane.revalidate();
                myLastSelectedFeature = selectedFeature;
            } else {
                selectedFeature.updateGUI();
            }

            myFeatureListTableModel.setRebuilding(false);

        } else {
            setEmptyControls();
            jFeatureGUIPanel.validate();
            jFeatureGUIPanel.repaint();
            myLastSelectedFeature = null;
        }
        jObjects3DListTable.repaint();
    }

    private void initComponents() {

        jEditToolBar = new javax.swing.JToolBar();
        jNewVSliceButton = new javax.swing.JButton("+Slice");
        jNewMapButton = new javax.swing.JButton("+Map");
        jNewPolarGridButton = new javax.swing.JButton("+PolarGrid");
        jFeatureGUIPanel = new JPanel();
        jObjectScrollPane = new JScrollPane();
        jControlScrollPane = new JScrollPane();
        jControlScrollPane.add(jFeatureGUIPanel);
        jFeatureGUIPanel.setLayout(new MigLayout(new LC().insetsAll("3"), null, null));

        JSplitPane northSouth = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                jObjectScrollPane, jControlScrollPane);
        northSouth.setResizeWeight(.50);
        jControlScrollPane.setViewportView(jFeatureGUIPanel);

        setLayout(new java.awt.BorderLayout());

        jEditToolBar.setRollover(true);

        jNewVSliceButton.setFocusable(false);
        jNewVSliceButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jNewVSliceButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jNewVSliceButton.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        jEditToolBar.add(jNewVSliceButton);

        jNewPolarGridButton.setFocusable(false);
        jNewPolarGridButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jNewPolarGridButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jNewPolarGridButton.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPolarGridButtonActionPerformed(evt);
            }
        });
        jEditToolBar.add(jNewPolarGridButton);

        jNewMapButton.setFocusable(false);
        jNewMapButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jNewMapButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jNewMapButton.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jEditToolBar.add(jNewMapButton);

        add(jEditToolBar, java.awt.BorderLayout.NORTH);

        setEmptyControls();

        add(northSouth, java.awt.BorderLayout.CENTER);
    }

    private void setEmptyControls() {
        jFeatureGUIPanel.removeAll();
        JTextField t = new javax.swing.JTextField();
        t.setText("Controls for selected feature");
        t.setEditable(false);
        jFeatureGUIPanel.setLayout(new java.awt.BorderLayout());
        jFeatureGUIPanel.add(t, java.awt.BorderLayout.CENTER);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        // LLHAreaCreateCommand doit = new LLHAreaCreateCommand("Slice");
        // CommandManager.getInstance().executeCommand(doit, true);
        // Hack for now allow adding of a shp file...gonna need more
        // generic interface for adding features...
        URL newMap = MapGUI.doSingleMapOpenDialog();
        if (newMap != null) {
            Feature testOne = new MapFeature(newMap);
            FeatureList.theFeatures.addFeature(testOne);
        }
        updateGUI();
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        FeatureCreateCommand doit = new FeatureCreateCommand("VSlice");
        CommandManager.getInstance().executeCommand(doit, true);
        //FeatureList.theFeatures.addFeature(new LLHAreaFeature(0));
    }
    
     private void jPolarGridButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // FIXME: need to generize the feature create command...
         PolarGridFeature pg = new PolarGridFeature();
         FeatureList.theFeatures.addFeature(pg);
         updateGUI();
    }
}

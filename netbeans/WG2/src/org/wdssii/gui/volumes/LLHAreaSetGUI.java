/*
 * LLHAreaSetGUI.java
 *
 * @author Robert Toomey
 * 
 */
package org.wdssii.gui.volumes;

import gov.nasa.worldwind.geom.LatLon;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.swing.RowEntryTable;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.RowEntryTableMouseAdapter;
import org.wdssii.gui.swing.TableUtil;
import org.wdssii.gui.volumes.LLHAreaSet.LLHAreaSetMemento;
import org.wdssii.properties.gui.IntegerGUI;

/**
 * LLHAreaSetGUI
 *
 * The gui that appears in the 3D Object list when a LLHAreaSet is selected.
 * This allows controls for a LLHAreaSet. All 'common' controls are in the
 * LLHAreaManager list, so for now at least we'll have to recreate height
 * controls for any subclass, but that's ok since we might want different layout
 * anyway.
 *
 * @author Robert Toomey
 */
public class LLHAreaSetGUI extends javax.swing.JPanel implements FeatureGUI {

    private IntegerGUI myTopHeightGUI;
    private IntegerGUI myBottomHeightGUI;
    private IntegerGUI myRowsGUI;
    private IntegerGUI myColsGUI;
    private LLHAreaFeature myFeature;
    private final LLHAreaSet myLLHAreaSet;
    private LLHAreaSetTableModel myLLHAreaSetTableModel;
    private RowEntryTable jObjects3DListTable;
    private javax.swing.JScrollPane jObjectScrollPane;

    /**
     * Creates new form LLHAreaSetGUI
     */
    public LLHAreaSetGUI(LLHAreaFeature f, LLHAreaSet owner) {
        myFeature = f;
        myLLHAreaSet = owner;
        setupComponents();
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        LLHAreaSetMemento m = myLLHAreaSet.getMemento();
        myTopHeightGUI.update(m);
        myBottomHeightGUI.update(m);
        myColsGUI.update(m);
        myRowsGUI.update(m);
        updateTable(m);
    }

    @Override
    public void activateGUI(JComponent parent) {
        parent.setLayout(new java.awt.BorderLayout());
        parent.add(this, java.awt.BorderLayout.CENTER);
        doLayout();
    }

    @Override
    public void deactivateGUI(JComponent parent) {
        parent.remove(this);
    }

    private void setupComponents() {

        /**
         * Completely control the layout within the scrollpane. Probably don't
         * want to fill here, let the controls do default sizes
         */
        setLayout(new MigLayout(new LC(), null, null));

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        JButton test = new JButton("Refresh");
        test.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                updateTable(null);
            }
            
        });
        toolbar.add(test);
        add(toolbar, new CC().growX().wrap());
        
        myRowsGUI = new IntegerGUI(myFeature, LLHAreaSetMemento.GRID_ROWS, "Rows", this,
                10, 100, 1, "");
        myRowsGUI.addToMigLayout(this);
        myColsGUI = new IntegerGUI(myFeature, LLHAreaSetMemento.GRID_COLS, "Cols", this,
                10, 100, 1, "");
        myColsGUI.addToMigLayout(this);
        myTopHeightGUI = new IntegerGUI(myFeature, LLHAreaSetMemento.TOP_HEIGHT, "Top", this,
                1, 20000, 1, "Meters");
        myTopHeightGUI.addToMigLayout(this);
        myBottomHeightGUI = new IntegerGUI(myFeature, LLHAreaSetMemento.BOTTOM_HEIGHT, "Bottom", this,
                0, 19999, 1, "Meters");
        myBottomHeightGUI.addToMigLayout(this);

        initTable();
        add(jObjectScrollPane, new CC().spanX(3));
        updateTable(null);

    }

    // First pass at 'table' of lat/lon values to edit...
    /**
     * Storage
     */
    private static class LLHAreaSetTableData {

        public double lat;
        public double lon;
        public int index;
    }

    private class LLHAreaSetTableModel extends RowEntryTableModel<LLHAreaSetTableData> {

        public static final int OBJ_NUMBER = 0;
        public static final int OBJ_LATITUDE = 1;
        public static final int OBJ_LONGITUDE = 2;
        private boolean isRebuilding = false;

        public LLHAreaSetTableModel() {
            super(LLHAreaSetTableData.class, new String[]{
                        "Point", "Latitude", "Longitude"
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
    private static class LLHAreaSetTableCellRenderer extends TableUtil.WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof LLHAreaSetTableData) {
                LLHAreaSetTableData e = (LLHAreaSetTableData) value;

                switch (trueCol) {
                    case LLHAreaSetTableModel.OBJ_NUMBER:
                        info = Integer.toString(row+1);
                        break;
                    case LLHAreaSetTableModel.OBJ_LATITUDE:
                        info = Double.toString(e.lat);
                        break;
                    case LLHAreaSetTableModel.OBJ_LONGITUDE:
                        info = Double.toString(e.lon);
                        break;
                    default:
                        info = "";
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

    private void initTable() {
        myLLHAreaSetTableModel = new LLHAreaSetTableModel();
        jObjects3DListTable = new RowEntryTable();
        final JTable myTable = jObjects3DListTable;
        jObjects3DListTable.setModel(myLLHAreaSetTableModel);
        final LLHAreaSetTableModel myModel = myLLHAreaSetTableModel;

        jObjects3DListTable.setFillsViewportHeight(true);
        jObjects3DListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jObjectScrollPane = new JScrollPane();
        jObjectScrollPane.setViewportView(jObjects3DListTable);

        LLHAreaSetTableCellRenderer p = new LLHAreaSetTableCellRenderer();
        jObjects3DListTable.setDefaultRenderer(LLHAreaSetTableData.class, p);

        int count = myTable.getColumnCount();
        TableColumnModel cm = myTable.getColumnModel();
        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();
        // IconHeaderRenderer r = new IconHeaderRenderer();

        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            // Make all headers draw the same to be consistent.
            // col.setHeaderRenderer(r);
           /* switch (i) {
             case LLHAreaTableModel.OBJ_LAT: {
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
             */
        }


        jObjects3DListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                //  jObjects3DListTableValueChanged(e);
            }
        });

        jObjects3DListTable.addMouseListener(new RowEntryTableMouseAdapter(jObjects3DListTable, myModel) {
            class Item extends JMenuItem {

                private final LLHAreaSetTableData d;

                public Item(String s, LLHAreaSetTableData line) {
                    super(s);
                    d = line;
                }

                public LLHAreaSetTableData getData() {
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
                            //i.getData().index;
                            
                            // FeatureDeleteCommand del = new FeatureDeleteCommand(i.getData().keyName);
                            // CommandManager.getInstance().executeCommand(del, true);
                        }
                    }
                };
                JPopupMenu popupmenu = new JPopupMenu();
                LLHAreaSetTableData entry = (LLHAreaSetTableData) (line);
                // if (entry.candelete) {
                     String name = "Delete point "+(row+1);
                     Item i = new Item(name, entry);
                     popupmenu.add(i);
                     i.addActionListener(al);
                // } else {
                //    String name = "This feature cannot be deleted";
                //    Item i = new Item(name, entry);
                //     popupmenu.add(i);
                // }
                return popupmenu;
            }

            @Override
            public void handleClick(Object stuff, int orgRow, int orgColumn) {

                if (stuff instanceof LLHAreaSetTableData) {
                    LLHAreaSetTableData entry = (LLHAreaSetTableData) (stuff);

                    switch (orgColumn) {

                        default:
                            break;
                    }
                }
            }
        });

        // setUpSortingColumns();

        // Initial update (some stuff created on start up statically)
        updateTable(null);
        //updateLabel();
    }

    public void updateTable(Object info) {

        // We only want to change selection when the user directly
        // changes one, not from other updates like from looping
        boolean changeSelection = false;

        int currentLine = 0;
        int select = -1;
        int oldSelect = -1;
        
        //myLLHAreaSet.getLocationList(); GUI thread different
        ArrayList<LLHAreaSetTableData> aList = new ArrayList<LLHAreaSetTableData>();
        List<LatLon> list = myLLHAreaSet.getLocations();
        int i = 0;
        for (LatLon l:list){
            LLHAreaSetTableData d = new LLHAreaSetTableData();
            d.lat = l.latitude.degrees;  // possible jitter...
            d.lon = l.longitude.degrees;
            d.index = i++;
            aList.add(d);
        }
        /*
         for (Feature d : f) {
         FeatureListTableData d2 = new FeatureListTableData();
         d2.visibleName = d.getName();
         d2.group = d.getFeatureGroup();
         d2.checked = d.getVisible();  // methods allow internal locking
         d2.keyName = d.getKey();
         d2.onlyMode = d.getOnlyMode();
         d2.message = d.getMessage();
         d2.candelete = d.getDeletable();
         newList.add(d2);
         if (topFeature == d) {
         select = currentLine;
         }
         if (myLastSelectedFeature == d) {
         oldSelect = currentLine;
         }
         currentLine++;
         }
         myFeatureListTableModel.setDataTypes(newList);
         myFeatureListTableModel.fireTableDataChanged();

         // Keep old selection unless it's gone...
         if (!changeSelection) {
         // Use old selection if exists...
         if (oldSelect > 0) {
         select = oldSelect;
         topFeature = myLastSelectedFeature;
         }
         } else {
         //log.debug("CHANGE SELECTION IS TRUE");
         }
         * */
         myLLHAreaSetTableModel.setDataTypes(aList);
         myLLHAreaSetTableModel.fireTableDataChanged();
    }
}

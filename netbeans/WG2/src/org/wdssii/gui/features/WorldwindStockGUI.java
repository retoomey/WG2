package org.wdssii.gui.features;

import gov.nasa.worldwind.layers.Layer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.FeatureChangeCommand;
import org.wdssii.gui.features.WorldwindStockFeature.WorldwindStockMemento;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.TableUtil;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.worldwind.WWCategoryLayer;

/**
 * LegendGUI handles gui controls for colorkey
 *
 * @author Robert Toomey
 */
public class WorldwindStockGUI extends FeatureGUI {

    /**
     * The LegendFeature we are using
     */
    private WorldwindStockFeature myFeature;
    //  private BooleanGUI myShowLabelsGUI;
    private javax.swing.JScrollPane jObjectScrollPane;
    private JTable myTable;
    private LayerTableModel myModel;

    /**
     * Creates new LegendGUI
     */
    public WorldwindStockGUI(WorldwindStockFeature owner) {
        myFeature = owner;
        setupComponents();
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        WorldwindStockMemento m = (WorldwindStockMemento) myFeature.getNewMemento();
        //  myShowLabelsGUI.update(m);
        updateLayerList();
    }

    @Override
    public void activateGUI(JComponent parent, JComponent secondary) {
        parent.setLayout(new java.awt.BorderLayout());
        parent.add(this, java.awt.BorderLayout.CENTER);
        secondary.setLayout(new java.awt.BorderLayout());
        secondary.add(jObjectScrollPane, java.awt.BorderLayout.CENTER);
        doLayout();
    }

    @Override
    public void deactivateGUI(JComponent parent, JComponent secondary) {
        parent.remove(this);
    }

    private void setupComponents() {

        /**
         * Completely control the layout within the scrollpane. Probably don't
         * want to fill here, let the controls do default sizes
         */
        setLayout(new MigLayout(new LC(), null, null));
        CC mid = new CC().growX().width("min:pref:");

        JLabel label = new JLabel("These are the basemap layers");
        add(label, new CC().growX());
        // myShowLabelsGUI = new BooleanGUI(myFeature, WorldwindStockMemento.SHOWWORLDWIND, "All layers", this);
        // myShowLabelsGUI.addToMigLayout(this);

        initTable();
        updateLayerList();
    }

    public void initTable() {

        jObjectScrollPane = new JScrollPane();

        jObjectScrollPane.setBorder(null);
        final JTable t = new javax.swing.JTable();
        myTable = t;
        final LayerTableModel m = new LayerTableModel();
        myModel = m;
        t.setModel(m);
        t.setFillsViewportHeight(
                true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set renderer for all LayerTableEntry cells
        LayerTableCellRenderer l = new LayerTableCellRenderer();
        t.setDefaultRenderer(LayerTableEntry.class, l);

        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();
        int count = t.getColumnCount();
        TableColumnModel cm = t.getColumnModel();
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            // Make all headers draw the same to be consistent.
            col.setHeaderRenderer(new IconHeaderRenderer());
            switch (i) {
                case 0:
                    IconHeaderInfo info = new IconHeaderInfo("layervisible.png");
                    col.setHeaderValue(info);
                    // FIXME: this isn't right, how to do it with look + feel
                    col.setWidth(2 * d.width);
                    col.setMaxWidth(2 * d.width);
                    col.setResizable(false);
                    break;
            }
        }

        /**
         * Add the mouse listener that handles clicking in any cell of our
         * custom Layer table
         */
        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // You actually want the single AND the double clicks so
                // that you always toggle even if they are clicking fast,
                // so we don't check click count.
                if (e.getComponent().isEnabled()
                        && e.getButton() == MouseEvent.BUTTON2) {
                    updateLayerList();
                    return;
                }
                if (e.getComponent().isEnabled()
                        && e.getButton() == MouseEvent.BUTTON1/* && e.getClickCount() == 1*/) {
                    Point p = e.getPoint();
                    int row = t.rowAtPoint(p);
                    int column = t.columnAtPoint(p);

                    if ((row > -1) && (column > -1)) {
                        int orgColumn = myTable.convertColumnIndexToModel(column);
                        int orgRow = myTable.convertRowIndexToModel(row);
                        Object stuff = myModel.getValueAt(orgRow, orgColumn);
                        if (stuff instanceof LayerTableEntry) {
                            LayerTableEntry entry = (LayerTableEntry) (stuff);


                            /**
                             * a click on visible checkbox toggles layer
                             * visibility
                             */
                            if (orgColumn == LayerTableModel.COL_VISIBLE) {
                                handleVisibleChange(entry.name);
                            }
                        }
                    }

                }
            }
        });

        jObjectScrollPane.setViewportView(t);
        setUpSortingColumns();
    }

    private void handleVisibleChange(String name) {

        ArrayList<Feature3DRenderer> list = myFeature.myRenderers;
        for (Feature3DRenderer f : list) {
            if (f instanceof WorldWindLayerRenderer) {
                WorldWindLayerRenderer w = (WorldWindLayerRenderer) (f);
                Layer layer = w.getLayer();
                if (layer.getName().equalsIgnoreCase(name)) {
                    layer.setEnabled(!layer.isEnabled());
                }
            }
        }
        // At least fire a command for now.  We aren't storing the visible
        // layers in memento.  Might matter when we 'save'
        FeatureMemento m = myFeature.getNewMemento();
        FeatureChangeCommand c = new FeatureChangeCommand(myFeature, m);
        CommandManager.getInstance().executeCommand(c, true);
        //updateLayerList();
        
    }

    /**
     * Set up sorting columns if wanted
     */
    private void setUpSortingColumns() {

        /**
         * Set the sorters for each column
         */
        TableRowSorter<LayerTableModel> sorter =
                new TableRowSorter<LayerTableModel>(myModel);
        myTable.setRowSorter(sorter);

        // This sort is strange, since toggle makes the item 'jump'  Seems
        // to be worker though.  Might be nice to disable the checkbox
        // columns from sorting....
        sorter.setComparator(LayerTableModel.COL_VISIBLE, new Comparator<LayerTableEntry>() {
            @Override
            public int compare(LayerTableEntry o1, LayerTableEntry o2) {
                return Boolean.valueOf(o1.enabled).compareTo(Boolean.valueOf(o2.enabled));
            }
        });
        sorter.setComparator(LayerTableModel.COL_LAYER_NAME, new Comparator<LayerTableEntry>() {
            @Override
            public int compare(LayerTableEntry o1, LayerTableEntry o2) {
                return o1.name.compareToIgnoreCase(o2.name);
            }
        });
        sorter.setComparator(LayerTableModel.COL_CATEGORY_NAME, new Comparator<LayerTableEntry>() {
            @Override
            public int compare(LayerTableEntry o1, LayerTableEntry o2) {
                return o1.category.compareToIgnoreCase(o2.category);
            }
        });
        myTable.getRowSorter().toggleSortOrder(LayerTableModel.COL_CATEGORY_NAME);
        myTable.getRowSorter().toggleSortOrder(LayerTableModel.COL_CATEGORY_NAME);
    }

    /**
     * Update the layer list from information pulled from a WorldWind LayerList
     */
    public final void updateLayerList() {
        ArrayList<LayerTableEntry> e = new ArrayList<LayerTableEntry>();

        // Try to save old selection.  We'll assume if the number of layers
        // is the same that the old row is the same row...
        int oldRow = myTable.getSelectedRow();

        ArrayList<Feature3DRenderer> list = myFeature.myRenderers;
        for (Feature3DRenderer f : list) {
            if (f instanceof WorldWindLayerRenderer) {
                WorldWindLayerRenderer w = (WorldWindLayerRenderer) (f);
                Layer layer = w.getLayer();
                LayerTableEntry n = new LayerTableEntry();
                layer.getName();
                n.name = layer.getName();
                n.enabled = layer.isEnabled();
                if (layer instanceof WWCategoryLayer) {
                    n.category = ((WWCategoryLayer) layer).getCategory();
                } else {
                    n.category = "NASA WorldWind Layer";
                }
                e.add(n);
            }
        }

        myModel.setDataTypes(e);
        myModel.fireTableDataChanged();
        if (oldRow > -1) {
            myTable.setRowSelectionInterval(oldRow, oldRow);
        }
    }

    /**
     * Our custom renderer for our layer view table
     */
    private static class LayerTableCellRenderer extends TableUtil.WG2TableCellRenderer {

        /**
         * A shared JCheckBox for rendering every check box in the list
         */
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
            if (value instanceof LayerTableEntry) {
                LayerTableEntry e = (LayerTableEntry) value;

                switch (trueCol) {
                    case LayerTableModel.COL_VISIBLE:
                        return getJCheckBox(table, e.enabled, isSelected, cellHasFocus, row, col);
                    case LayerTableModel.COL_LAYER_NAME:
                        info = e.name;
                        break;
                    case LayerTableModel.COL_CATEGORY_NAME:
                        info = e.category;
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

    /**
     * Each row in our table will have a LayerTableEntry
     */
    public static class LayerTableEntry {

        String name;
        boolean enabled;
        String category;
    }

    /**
     * We have a custom model that stores a single LayerTableEntry for each row
     * of the table
     */
    private static class LayerTableModel extends RowEntryTableModel<LayerTableEntry> {

        private static final int COL_VISIBLE = 0;
        private static final int COL_LAYER_NAME = 1;
        private static final int COL_CATEGORY_NAME = 2;

        public LayerTableModel() {
            super(LayerTableEntry.class, new String[]{
                        "Visible", "Layer", "Category"});
        }
    }
}

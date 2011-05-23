package org.wdssii.gui.nbm.views;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.UIManager;
import javax.swing.plaf.TableHeaderUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.worldwind.WWCategoryLayer;
import com.sun.java.swing.plaf.windows.WindowsTableHeaderUI;

/**
 * LayersTopCompoent
 * 
 * This view shows the list of WorldWind layers and lets us toggle
 * visibility and move them up and down in a stack.
 * 
 * @todo Pull out the stock code into a superclass so others can use it
 * as a generic WorldWind layer control view.
 * 
 * @author Robert Toomey
 * 
 */
@ConvertAsProperties(dtd = "-//org.wdssii.gui.nbm.views//Layers//EN",
autostore = false)
@TopComponent.Description(preferredID = "LayersTopComponent",
iconBase = "org/wdssii/gui/nbm/views/layers.png",
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "explorer", openAtStartup = true)
@ActionID(category = "Window", id = "org.wdssii.gui.nbm.views.LayersTopComponent")
@ActionReference(path = "Menu/Window/WDSSII" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_LayersAction",
preferredID = "LayersTopComponent")
public final class LayersTopComponent extends TopComponent {

    private final LayerTableModel myModel;
    private final JTable myTable;

    /** Our custom renderer for our layer view table */
    private static class LayerTableCellRenderer extends DefaultTableCellRenderer {

        protected ImageIcon createImageIcon(String path,
                String description) {
            java.net.URL imgURL = getClass().getResource(path);
            if (imgURL != null) {
                return new ImageIcon(imgURL, description);
            } else {
                // System.err.println("Couldn't find file: " + path);
                return null;
            }
        }

        /** Code taken from open source example on web:
         * http://tips4java.wordpress.com/2009/02/27/default-table-header-cell-renderer/
         * 
         */
        protected Icon getIcon(JTable table, int column) {
            SortKey sortKey = getSortKey(table, column);
            if (sortKey != null && table.convertColumnIndexToView(sortKey.getColumn()) == column) {
                switch (sortKey.getSortOrder()) {
                    case ASCENDING:
                        return UIManager.getIcon("Table.ascendingSortIcon");
                    case DESCENDING:
                        return UIManager.getIcon("Table.descendingSortIcon");
                }
            }
            return null;
        }

        protected String getSortText(JTable table, int column) {
            SortKey sortKey = getSortKey(table, column);
            if (sortKey != null && table.convertColumnIndexToView(sortKey.getColumn()) == column) {
                switch (sortKey.getSortOrder()) {
                    case ASCENDING:
                        return "-";
                    case DESCENDING:
                        return "+";
                }
            }
            return "";
        }

        protected SortKey getSortKey(JTable table, int column) {
            RowSorter rowSorter = table.getRowSorter();
            if (rowSorter == null) {
                return null;
            }

            List sortedColumns = rowSorter.getSortKeys();
            if (sortedColumns.size() > 0) {
                return (SortKey) sortedColumns.get(0);
            }
            return null;
        }
        /** A shared JCheckBox for rendering every check box in the list */
        private JCheckBox checkbox = new JCheckBox();
        /** The icon for the 'visible' checkbox column of table */
        private ImageIcon p1 = createImageIcon("layervisible.png", "Layer is visible");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            String info = "";
            int trueCol = table.convertColumnIndexToModel(col);

            // Only our headers are type 'string'
            if (value instanceof String) {
                info = (String) (value);

                // First, set the basic label stuff...
                super.getTableCellRendererComponent(table, info,
                        isSelected, cellHasFocus, row, col);

                // Change the needed drawing for a table 'header'
                // Note state is KEPT, so must undo this for other rows
                setHorizontalAlignment(CENTER);
                setVerticalAlignment(BOTTOM);
                setHorizontalTextPosition(LEFT);
                setOpaque(false);
                setBorder(UIManager.getBorder("TableHeader.cellBorder"));
                if (info.equals("Visible")) {
                    setIcon(p1);
                    setText(getSortText(table, col));
                } else {
                    setIcon(getIcon(table, col));

                }
                return this;

            }

            // Each row uses a single LayerTableEntry...
            if (value instanceof LayerTableEntry) {
                LayerTableEntry e = (LayerTableEntry) value;

                switch (trueCol) {
                    case LayerTableModel.COL_VISIBLE:

                        // We have to make sure we set EVERYTHING we use everytime,
                        // since we are just using a single checkbox.        
                        checkbox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                        checkbox.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                        checkbox.setEnabled(isEnabled());
                        checkbox.setSelected(e.enabled);
                        checkbox.setFont(getFont());
                        checkbox.setFocusPainted(false);
                        checkbox.setBorderPainted(true);
                        checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder")
                                : noFocusBorder);
                        return checkbox;


                    case LayerTableModel.COL_LAYER_NAME:
                        info = e.name;
                        break;
                    case LayerTableModel.COL_CATEGORY_NAME:
                        info = e.category;
                        break;
                }

            }

            // Default is to render a stock label.
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(BOTTOM);
            //  setHorizontalTextPosition(LEFT);
            setOpaque((row != -1));
            setBorder(null);
            setIcon(null);
            return super.getTableCellRendererComponent(table, info,
                    isSelected, cellHasFocus, row, col);


        }
    }

    /** Each row in our table will have a LayerTableEntry */
    private static class LayerTableEntry {

        String name;
        boolean enabled;
        String category;
    }

    /** We have a custom model that stores a single LayerTableEntry
     * for each row of the table
     */
    private static class LayerTableModel extends AbstractTableModel {

        private final String headers[];
        private ArrayList<LayerTableEntry> myEntries;
        private static final int COL_VISIBLE = 0;
        private static final int COL_LAYER_NAME = 1;
        private static final int COL_CATEGORY_NAME = 2;

        public LayerTableModel() {

            this.headers = new String[]{
                "Visible", "Layer", "Category"
            };
        }

        public void setLayerTableEntries(ArrayList<LayerTableEntry> l) {
            myEntries = l;
            this.fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            int size = 0;
            if (myEntries != null) {
                size = myEntries.size();
            }
            return size;
        }

        @Override
        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex == -1) {
                return "H";
            }
            if (myEntries != null) {
                if (rowIndex < myEntries.size()) {
                    return myEntries.get(rowIndex);
                }
            }
            return null;
        }
    }

    public LayersTopComponent() {
        initComponents();
        final JTable t = new javax.swing.JTable();
        myTable = t;
        final LayerTableModel m = new LayerTableModel();
        myModel = m;
        t.setModel(m);
        t.setFillsViewportHeight(
                true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Ok, wtf? setDefaultRenderer is supposed to work, but doesn't..
        // our model is returning a LayerTableEntry object  Either it's
        // broken or I'm missing something in the API
        // FIXME: figure out what I'm doing wrong.
        LayerTableCellRenderer l = new LayerTableCellRenderer();
        t.setDefaultRenderer(LayerTableEntry.class, l);

        // Set each column individually, since above doesn't work.
        int count = t.getColumnCount();
        TableColumnModel cm = t.getColumnModel();
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            col.setCellRenderer(l);
            col.setHeaderRenderer(l);

            switch (i) {
                case 0:
                    col.setWidth(
                            50 * JCheckBox.WIDTH);
                    col.setMaxWidth(
                            50 * JCheckBox.WIDTH);
                    col.setResizable(false);
                    break;
            }
        }

        jLayersScrollPane.setViewportView(t);

        setUpSortingColumns();

        /** Add the mouse listener that handles clicking in any cell of our
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
                    int orgColumn = myTable.convertColumnIndexToModel(column);
                    int orgRow = myTable.convertRowIndexToModel(row);
                    Object stuff = myModel.getValueAt(orgRow, orgColumn);
                    if (stuff instanceof LayerTableEntry) {
                        LayerTableEntry entry = (LayerTableEntry) (stuff);


                        /** a click on visible checkbox toggles layer visibility */
                        if (orgColumn == LayerTableModel.COL_VISIBLE) {
                            LayerList list = getLayerList();
                            if (list != null) {
                                Layer l = list.getLayerByName(entry.name);
                                l.setEnabled(!l.isEnabled());
                            }
                            updateLayerList();
                        }
                    }
                }
            }
        });

        // Create our list within the scroll pane
        setName(NbBundle.getMessage(LayersTopComponent.class, "CTL_LayersTopComponent"));
        setToolTipText(NbBundle.getMessage(LayersTopComponent.class, "HINT_LayersTopComponent"));

    }

    /** Set up sorting columns if wanted */
    private void setUpSortingColumns() {

        /** Set the sorters for each column */
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

    /** Return the layer list we use
    FIXME: Make this interface/superclass so others could use this
    view if wanted in their own code
     */
    public LayerList getLayerList() {
        LayerList layers = CommandManager.getInstance().getLayerList();
        return layers;
    }

    /** Update the layer list from information pulled from a WorldWind
     * LayerList
     */
    public void updateLayerList() {
        ArrayList<LayerTableEntry> e = new ArrayList<LayerTableEntry>();

        // Try to save old selection.  We'll assume if the number of layers
        // is the same that the old row is the same row...
        int oldRow = myTable.getSelectedRow();

        LayerList layers = getLayerList();
        if (layers != null) {
            for (Layer layer : layers) {
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
        myModel.setLayerTableEntries(e);
        if (oldRow > -1) {
            myTable.setRowSelectionInterval(oldRow, oldRow);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayersScrollPane = new javax.swing.JScrollPane();

        jLayersScrollPane.setBorder(null);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayersScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayersScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jLayersScrollPane;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        updateLayerList();
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

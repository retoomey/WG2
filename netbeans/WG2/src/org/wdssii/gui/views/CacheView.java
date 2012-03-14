package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.SourceManager.SourceCommand;
import org.wdssii.gui.commands.CacheClearCommand;
import org.wdssii.gui.commands.CacheCommand;
import org.wdssii.gui.commands.CacheSetSizeCommand;
import org.wdssii.gui.commands.ProductCommand;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.storage.DataManager;

public class CacheView extends JThreadPanel implements CommandListener {

    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works
    // When sources or products change, update the navigation controls
    public void ProductCommandUpdate(ProductCommand command) {
        updateGUI(command);
    }

    public void SourceCommandUpdate(SourceCommand command) {
        updateGUI(command);
    }

    public void CacheCommandUpdate(CacheCommand command) {
        updateGUI(command);
    }
    private final CacheTableModel myModel;
    private final JTable myTable;
    private javax.swing.JScrollPane jLayersScrollPane;
    private JLabel myLabel;
    /** When on, update GUI on the fly.  Updating this list a lot can
     * make the display slugish, so making it gui controlled.
     */
    private boolean myActiveMonitor = false;

    /** Our factory, called by reflection to populate menus, etc...*/
    public static class Factory extends WdssiiDockedViewFactory {

        public Factory() {
            super("Cache", "pictures.png");
        }

        @Override
        public Component getNewComponent() {
            return new CacheView();
        }
    }

    /** Our custom renderer for our layer view table */
    private static class CacheTableCellRenderer extends WG2TableCellRenderer {

        /** A shared JCheckBox for rendering every check box in the list */
        //private JCheckBox checkbox = new JCheckBox();
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info = "";
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof CacheTableEntry) {
                CacheTableEntry e = (CacheTableEntry) value;

                switch (trueCol) {
                    // case CacheTableModel.COL_VISIBLE:
                    //     return getJCheckBox(table, e.enabled, isSelected, cellHasFocus, row, col);
                    case CacheTableModel.COL_KEY_NAME:
                        info = e.name;
                        break;
                    case CacheTableModel.COL_DATATYPE:
                        info = e.datatype;
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

    /** Each row in our table will have a CacheTableEntry */
    public static class CacheTableEntry {

        String name;
        String datatype;
    }

    /** We have a custom model that stores a single CacheTableEntry
     * for each row of the table
     */
    private static class CacheTableModel extends RowEntryTableModel<CacheTableEntry> {

        //  private static final int COL_VISIBLE = 0;
        private static final int COL_KEY_NAME = 0;
        private static final int COL_DATATYPE = 1;

        public CacheTableModel() {
            super(CacheTableEntry.class, new String[]{
                        "Key", "DataType"});
        }
    }

    @Override
    public void updateInSwingThread(Object command) {
        if (command != null){
            if (command instanceof Boolean){  // From refresh button
                updateCacheList(true, true);
            }
        }
        updateCacheList(myActiveMonitor, true);
    }

    public CacheView() {
        initComponents();
        final JTable t = new javax.swing.JTable();
        myTable = t;
        final CacheTableModel m = new CacheTableModel();
        myModel = m;
        t.setModel(m);
        t.setFillsViewportHeight(
                true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set renderer for all CacheTableEntry cells
        CacheTableCellRenderer l = new CacheTableCellRenderer();
        t.setDefaultRenderer(CacheTableEntry.class, l);

        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();

        int count = t.getColumnCount();
        TableColumnModel cm = t.getColumnModel();
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            // Make all headers draw the same to be consistent.
            col.setHeaderRenderer(new IconHeaderRenderer());
            /*switch (i) {
            case 0:
            IconHeaderInfo info = new IconHeaderInfo("layervisible.png");
            col.setHeaderValue(info);
            // FIXME: this isn't right, how to do it with look + feel
            col.setWidth(2 * d.width);
            col.setMaxWidth(2 * d.width);
            col.setResizable(false);
            break;
            }*/
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
                    // updateCacheList();
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
                        if (stuff instanceof CacheTableEntry) {
                            CacheTableEntry entry = (CacheTableEntry) (stuff);


                            /** a click on visible checkbox toggles layer visibility */
                            /*if (orgColumn == CacheTableModel.COL_VISIBLE) {
                            LayerList list = getLayerList();
                            if (list != null) {
                            Layer l = list.getLayerByName(entry.name);
                            l.setEnabled(!l.isEnabled());
                            }
                            updateCacheList();
                            }*/
                        }
                    }

                }
            }
        });
        CommandManager.getInstance().addListener("cache", this);

        // FIXME: Earth ball currently has to be created first or this
        // can't get the layer list...
        updateCacheList(myActiveMonitor, true);
    }

    private void initComponents() {
        // Create Mig fill layout...
        setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));

        // Create the toolbar
        JToolBar jToolBar1;
        jToolBar1 = new javax.swing.JToolBar();
        JButton clearButton = new JButton("Clear");
        clearButton.setToolTipText("Clear all products from the cache");
        jToolBar1.add(clearButton);
        JButton sizeButton = new JButton("Size");
        sizeButton.setToolTipText("Set the maximum size of the product cache");
        jToolBar1.add(sizeButton);
        JCheckBox monitorButton = new JCheckBox("Watch");
        monitorButton.setToolTipText("Update table on the fly (can slow display)");
        jToolBar1.add(monitorButton);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setToolTipText("Refresh product watch table");
        jToolBar1.add(refreshButton);
        JButton garbageButton = new JButton("GC");
        garbageButton.setToolTipText("Force a java garbage collection");
        jToolBar1.add(garbageButton);
        add(jToolBar1, new CC().dockNorth());
        JButton tileClearButton = new JButton("TClear");
        tileClearButton.setToolTipText("Clear tiles");
        jToolBar1.add(tileClearButton);
        
        // Create the infomation label
        myLabel = new JLabel();
        myLabel.setText("No cache information");
        add(myLabel, new CC().dockNorth().growX());

        // Create a scroll bar to show the cache table(s)
        jLayersScrollPane = new javax.swing.JScrollPane();
        jLayersScrollPane.setBorder(null);
        add(jLayersScrollPane, new CC().growX().growY());

        clearButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CacheClearCommand ccc = new CacheClearCommand();
                CommandManager.getInstance().executeCommand(ccc, true);
            }
        });
        final JComponent rootView = this;
        sizeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                CacheSetSizeCommand cssc = new CacheSetSizeCommand(rootView);
                CommandManager.getInstance().executeCommand(cssc, true);
            }
        });

        monitorButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                myActiveMonitor = !myActiveMonitor;
                updateGUI();
            }
        });
        
        refreshButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
               updateGUI(true);
            }
        });
        
        garbageButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
               System.gc();
               updateGUI();
            }
        });
        
        tileClearButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                DataManager.getInstance().purgeAllTiles();
            }
        });
    }

    /** Set up sorting columns if wanted */
    private void setUpSortingColumns() {
    }

    /** Update the cache list with information pulled from the Product cache
     */
    public final void updateCacheList(boolean updateTable, boolean updateMemory) {
        ArrayList<CacheTableEntry> e = new ArrayList<CacheTableEntry>();

        // Shared code with side effects
        ProductManager pm = ProductManager.getInstance();
        ArrayList<Product> products = pm.getCurrentCacheList();
        int aSize = products.size();
        int maxSize = pm.getCacheSize();

        int oldRow = -1;
        if (updateTable) {
            // Try to save old selection.  We'll assume if the number of layers
            // is the same that the old row is the same row...
            oldRow = myTable.getSelectedRow();
            for (int i = 0; i < aSize; i++) {
                Product p = products.get(i);
                CacheTableEntry n = new CacheTableEntry();
                if (p != null) {
                    n.name = p.getCacheKey();
                    n.datatype = p.getDataType();
                } else {
                    n.name = "null";
                    n.datatype = "None";
                }
                e.add(n);
            }
        }

        if (updateMemory) {
            // Get current size of heap in bytes
            long heapSize = Runtime.getRuntime().totalMemory();
            heapSize = heapSize / 1024 / 1024; // MB
            // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
            // Any attempt will result in an OutOfMemoryException.
            long heapMaxSize = Runtime.getRuntime().maxMemory();
            heapMaxSize = heapMaxSize / 1024 / 1024;
            // Get amount of free memory within the heap in bytes. This size will increase
            // after garbage collection and decrease as new objects are created.
            long heapFreeSize = Runtime.getRuntime().freeMemory();

            heapFreeSize = heapFreeSize / 1024 / 1024;

            long bufferSize = DataManager.getInstance().getAllocatedBytes();
            bufferSize = bufferSize / 1024 / 1024;

            long fbufferSize = DataManager.getInstance().getFailedAllocatedBytes();
            fbufferSize = fbufferSize / 1024 / 1024;

            long d = DataManager.getInstance().getDeallocatedBytes();
            d = d / 1024 / 1024;
            
            int nt = DataManager.getInstance().getNumberOfCachedItems();
            
            String out = String.format(
                    "<html>Products %d of %d stored.<br> Java Heap %d/%d MB, %d MB Available<br> DataManager: Allocated %d MB, Deallocated %d MB, %d MB<b>failed</b> Tiles: %d",
                    aSize, maxSize, heapSize, heapMaxSize, heapFreeSize, bufferSize, d, fbufferSize, nt);
            myLabel.setText(out);
        }

        if (updateTable) {
            myModel.setDataTypes(e);
            myModel.fireTableDataChanged();
            if ((oldRow > -1) && (oldRow < aSize)) {
                myTable.setRowSelectionInterval(oldRow, oldRow);
            }
        }
    }
}

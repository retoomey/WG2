package org.wdssii.gui.views;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.ProductManager.ProductDataInfo;
import org.wdssii.gui.SourceManager;
import org.wdssii.gui.SourceManager.SourceCommand;
import org.wdssii.gui.commands.ProductLoadCommand;
import org.wdssii.gui.commands.ProductLoadCommand.ProductLoadCaller;
import org.wdssii.gui.commands.SourceConnectCommand;
import org.wdssii.gui.commands.SourceDeleteCommand;
import org.wdssii.gui.commands.SourceDeleteCommand.SourceDeleteAllCommand;
import org.wdssii.gui.commands.SourceDisconnectCommand;
import org.wdssii.gui.swing.JThreadPanel;
import org.wdssii.gui.swing.RowEntryTable;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.RowEntryTableMouseAdapter;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer;
import org.wdssii.gui.swing.TableUtil.IconHeaderRenderer.IconHeaderInfo;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.index.HistoricalIndex;
import org.wdssii.index.HistoricalIndex.RecordQuery;
import org.wdssii.index.IndexRecord;
import org.wdssii.index.IndexWatcher;

/**
 *
 * ProductsView is a JPanel that handles showing an IndexCollection and
 * allowing searching/choosing of records, connecting/disconnecting from
 * the current sources.
 * 
 * @author Robert Toomey
 * 
 */
public class ProductsView extends JThreadPanel implements WdssiiView {
    
    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works

    // For everything BUT delete, just update the parts
    // This could be called by different threads, so use the update method,
    // NOT the thread specific updateParts
    public void SourceCommandUpdate(SourceCommand command) {
        updateGUI();
    }
    
    private String[] mySelection = new String[3];
    private RowEntryTable jSourceListTable;
    private javax.swing.JTable jProductsListTable;
    private javax.swing.JTable jChoicesListTable;
    private javax.swing.JTable jResultsListTable;
    /** The source list shows all the data sources you have */
    private SourceListTableModel mySourceListTableModel;
    /** The product list shows the list of all available products in the
     * current selected source
     */
    private ProductListTableModel myProductListTableModel;
    /** Show the list of individual data choices for a selected source/product */
    private ChoicesListTableModel myChoicesListTableModel;
    /** Show the actual records for selected source, product, choices */
    private ResultsListTableModel myResultsListTableModel;
    protected String myIndexName;
    private javax.swing.JScrollPane jChoicesScrollPane;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jProductsScrollPane;
    private javax.swing.JLabel jResultsLabel;
    private javax.swing.JScrollPane jResultsScrollPane;
    private javax.swing.JLabel jSelectedSourceLabel;
    private javax.swing.JScrollPane jSourcesScrollPane;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JSplitPane jSplitPane3;
    private javax.swing.JSplitPane jTopBottomSplitPane;

    @Override
    public void updateInSwingThread(Object info) {       
        // Regenerate our stuff...
        updateParts();
    }

    /**The data model for the 'source' list (copies source manager record at
     * moment)  Copy best for thread safety, though might save some memory
     * on large data sets if we didn't.  The reason for our own data structure
     * is that we might have stuff likes colors, etc. that are GUI only.
     */
    private static class SourceTableData {

        public String visibleName;
        public String indexKey;
        public boolean realtime;
        public String indexLocation;
        public boolean connected;  // connection success
        public boolean connecting; // connection being attempted
    };

    /** Storage for the current product list */
    private static class ProductTableData {

        String visibleName; // Name shown in list
        String datatype; // The datatype (used to access the record);
        Color textColor; // The text color for the datatype (null is system
        // default)
        Color background; // The background color for the datatype (null is
        // system default)
    }

    /** Storage for the current Choices list */
    private static class ChoicesTableData {

        String visibleName; // Name shown in list
    }

    /** Storage for a single row of the Results table */
    private static class ResultsTableData {

        public ResultsTableData(Date time, String timestamp, String subtype,
                String datatype, String source) {
            this.time = time;
            this.timestamp = timestamp;
            this.subtype = subtype;
            this.datatype = datatype;
            this.source = source;
        }
        Date time;
        String timestamp;
        String subtype;
        String datatype;
        String source;
    }

    /** Table model to handle the Sources list */
    private class SourceListTableModel extends RowEntryTableModel<SourceTableData> {

        public static final int SOURCE_STATUS = 0;
        public static final int SOURCE_NAME = 1;
        public static final int SOURCE_PATH = 2;

        public SourceListTableModel() {
            super(SourceTableData.class, new String[]{
                        "?", "Source", "Path"
                    });
        }
    }

    /** Our custom renderer for our product view table */
    private static class SourceListTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info = "";
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof SourceTableData) {
                SourceTableData e = (SourceTableData) value;

                switch (trueCol) {
                    case SourceListTableModel.SOURCE_STATUS:
                        String icon = "link_break.png"; // Not connected
                        if (e.connecting) {
                            icon = "link_go.png";
                        }
                        if (e.connected) {
                            icon = "link.png";
                        }
                        return getIcon(table, icon, isSelected, cellHasFocus, row, col);
                    case SourceListTableModel.SOURCE_NAME:
                        info = e.visibleName;
                        break;
                    case SourceListTableModel.SOURCE_PATH:
                        info = e.indexLocation;
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

    private class ProductListTableModel extends AbstractTableModel {

        /** The column headers */
        private final String headers[];
        private ArrayList<ProductTableData> myDataTypes;

        public ProductListTableModel() {

            // Hardcoded to match bookmarks.
            this.headers = new String[]{
                "Products"
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
        public Object getValueAt(int row, int column) {
            if (myDataTypes != null) {
                ProductTableData d = myDataTypes.get(row);
                switch (column) {
                    case 0: // name
                        return d.visibleName;
                    default:
                        return "";
                }
            } else {
                return "";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
        }

        public void setDataTypes(ArrayList<ProductTableData> n) {
            myDataTypes = n;
            this.fireTableDataChanged();
        }

        private ProductTableData getProductTableDataForRow(int row) {
            ProductTableData s = null;
            if (myDataTypes != null) {
                if ((row >= 0) && (row < myDataTypes.size())) {
                    s = myDataTypes.get(row);
                }
            }
            return s;
        }
    }

    private class ChoicesListTableModel extends AbstractTableModel {

        /** The column headers */
        private final String headers[];
        private ArrayList<ChoicesTableData> myChoicesTableData;

        public ChoicesListTableModel() {

            // Hardcoded to match bookmarks.
            this.headers = new String[]{
                "Choices"
            };
        }

        @Override
        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public int getRowCount() {
            int size = 0;
            if (myChoicesTableData != null) {
                size = myChoicesTableData.size();
            }
            return size;
        }

        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (myChoicesTableData != null) {
                ChoicesTableData d = myChoicesTableData.get(row);
                switch (column) {
                    case 0: // name
                        return d.visibleName;
                    default:
                        return "";
                }
            } else {
                return "";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
        }

        public void setChoices(ArrayList<ChoicesTableData> n) {
            myChoicesTableData = n;
            this.fireTableDataChanged();
        }

        private ChoicesTableData getChoicesTableDataForRow(int row) {
            ChoicesTableData s = null;
            if (myChoicesTableData != null) {
                if ((row >= 0) && (row < myChoicesTableData.size())) {
                    s = myChoicesTableData.get(row);
                }
            }
            return s;
        }
    }

    private class ResultsListTableModel extends AbstractTableModel {

        /** The column headers */
        private final String headers[];
        private ArrayList<ResultsTableData> myResultsTableData;
        private boolean myShowChoices = false;
        private boolean myShowProducts = false;

        public ResultsListTableModel() {

            // Hardcoded to match bookmarks.
            this.headers = new String[]{
                "Times", "Choices", "Products"
            };

        }

        @Override
        public int getColumnCount() {
            // Times always show.  Two other columns are hidden iff
            // the data is the same for each row.
            if (!myShowChoices) {
                return 1;
            }
            if (!myShowProducts) {
                return 2;
            }
            return 3;
        }

        @Override
        public int getRowCount() {
            int size = 0;
            if (myResultsTableData != null) {
                size = myResultsTableData.size();
            }
            return size;
        }

        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (myResultsTableData != null) {
                ResultsTableData d = myResultsTableData.get(row);
                switch (column) {
                    case 0: // Time
                        return d.time;
                    case 1: // Choice
                        return d.subtype;
                    case 2: // Products
                        return d.datatype;
                    default:
                        return "";
                }

            } else {
                return "";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
        }

        private void setRecordData(ArrayList<ResultsTableData> d,
                boolean showSubtypes, boolean showDatatypes) {
            myResultsTableData = d;
            myShowChoices = showSubtypes;
            myShowProducts = showDatatypes;
            this.fireTableStructureChanged();
            //   this.fireTableDataChanged();
        }

        private ResultsTableData getResultsTableDataForRow(int row) {
            ResultsTableData s = null;
            if (myResultsTableData != null) {
                if ((row >= 0) && (row < myResultsTableData.size())) {
                    s = myResultsTableData.get(row);
                }
            }
            return s;
        }
    }

    /** Create the four special tables for the product selection */
    private void initTables() {

        // Could create these with class..
        mySourceListTableModel = new SourceListTableModel();
        myProductListTableModel = new ProductListTableModel();
        myChoicesListTableModel = new ChoicesListTableModel();
        myResultsListTableModel = new ResultsListTableModel();

        jSourceListTable = new RowEntryTable();
        jSourceListTable.setModel(mySourceListTableModel);
        jSourceListTable.setFillsViewportHeight(true);
        jSourceListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jSourcesScrollPane.setViewportView(jSourceListTable);

        SourceListTableCellRenderer p = new SourceListTableCellRenderer();
        jSourceListTable.setDefaultRenderer(SourceTableData.class, p);

        jProductsListTable = new javax.swing.JTable();
        jProductsListTable.setModel(myProductListTableModel);
        jProductsListTable.setFillsViewportHeight(true);
        jProductsListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jProductsScrollPane.setViewportView(jProductsListTable);

        jChoicesListTable = new javax.swing.JTable();
        jChoicesListTable.setModel(myChoicesListTableModel);
        jChoicesListTable.setFillsViewportHeight(true);
        jChoicesListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jChoicesScrollPane.setViewportView(jChoicesListTable);

        jResultsListTable = new javax.swing.JTable();
        jResultsListTable.setModel(myResultsListTableModel);
        jResultsListTable.setFillsViewportHeight(true);
        jResultsListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jResultsScrollPane.setViewportView(jResultsListTable);

        jSourceListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jSourceListTableValueChanged(e);
            }
        });

        jSourceListTable.addMouseListener(new RowEntryTableMouseAdapter(jSourceListTable, mySourceListTableModel) {

            class Item extends JMenuItem {

                private final SourceTableData d;

                public Item(String s, SourceTableData line) {
                    super(s);
                    d = line;
                }

                public SourceTableData getData() {
                    return d;
                }
            };

            @Override
            public JPopupMenu getDynamicPopupMenu(Object line, int row, int column) {

                // FIXME: Code a bit messy, we're just hacking the text value
                // for now.  Probably will need a custom JPopupMenu that has
                // our Objects3DTableData in it.
                // FIXME: Really need a cleaner way to do this...
                ActionListener al = new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Object z = e.getSource();
                        if (z instanceof Item) {
                            Item i = (Item) (e.getSource());
                            String text = i.getText();
                            String vis = i.getData().visibleName;
                            if (!vis.equals(HistoricalIndex.MANUAL)) {
                                if (text.startsWith("Delete")) {
                                    SourceDeleteCommand del = new SourceDeleteCommand(i.getData().indexKey);
                                    CommandManager.getInstance().executeCommand(del, true);
                                } else if (text.startsWith("Connect")) {
                                    SourceConnectCommand r = new SourceConnectCommand(i.getData().indexKey);
                                    CommandManager.getInstance().executeCommand(r, true);
                                } else if (text.startsWith("Disconnect")) {
                                    SourceDisconnectCommand r = new SourceDisconnectCommand(i.getData().indexKey);
                                    CommandManager.getInstance().executeCommand(r, true);
                                }
                            }
                        } else {
                            JMenuItem i = (JMenuItem) (z);
                            String text = i.getText();
                            if (text.startsWith("Delete All Sources")) {
                                SourceDeleteAllCommand del = new SourceDeleteAllCommand();
                                CommandManager.getInstance().executeCommand(del, true);
                            }
                        }
                    }
                };
                JPopupMenu popupmenu = new JPopupMenu();
                SourceTableData entry = (SourceTableData) (line);

                String vis = entry.visibleName;
                if (!vis.equals(HistoricalIndex.MANUAL)) {
                    // Disconnect/Reconnect command...
                    if (entry.connected) {
                        String name = "Disconnect " + entry.visibleName;
                        Item i = new Item(name, entry);
                        popupmenu.add(i);
                        i.addActionListener(al);
                    } else {
                        if (!entry.connecting) {
                            String name = "Connect " + entry.visibleName;
                            Item i = new Item(name, entry);
                            popupmenu.add(i);
                            i.addActionListener(al);
                        }
                    }

                    // Delete 'ktlx'
                    String name = "Delete " + entry.visibleName;
                    Item i = new Item(name, entry);
                    popupmenu.add(i);
                    i.addActionListener(al);

                    popupmenu.add(new JSeparator());
                }

                // Delete all
                String name = "Delete All Sources";
                JMenuItem z = new JMenuItem(name, null);
                popupmenu.add(z);
                z.addActionListener(al);
                return popupmenu;
            }

            @Override
            public void handleClick(Object stuff, int orgRow, int orgColumn) {

                if (stuff instanceof SourceTableData) {
                    SourceTableData entry = (SourceTableData) (stuff);

                    //switch (orgColumn) {

                    // }
                }
            }
        });

        JCheckBox aBox = new JCheckBox();
        Dimension d = aBox.getMinimumSize();
        int count = jSourceListTable.getColumnCount();
        TableColumnModel cm = jSourceListTable.getColumnModel();
        for (int i = 0; i < count; i++) {
            TableColumn col = cm.getColumn(i);
            // Make all headers draw the same to be consistent.
            col.setHeaderRenderer(new IconHeaderRenderer());
            switch (i) {
                case SourceListTableModel.SOURCE_STATUS: {
                    IconHeaderInfo info = new IconHeaderInfo("link.png");
                    col.setHeaderValue(info);
                    // FIXME: this isn't right, how to do it with look + feel
                    col.setWidth(2 * d.width);
                    col.setMaxWidth(2 * d.width);
                    col.setResizable(false);
                }
                break;
            }
        }

        jProductsListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jProductsListTableValueChanged(e);
            }
        });

        jChoicesListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jChoicesListTableValueChanged(e);
            }
        });

        jResultsListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jResultsListTableValueChanged(e);
            }
        });
        // Update all data....
        updateParts();
    }

    private void jSourceListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        int row = jSourceListTable.getSelectedRow();
        if (mySourceListTableModel != null) {
            //   SourceTableData d = mySourceListTableModel.getSourceTableDataForRow(row);
            SourceTableData d = mySourceListTableModel.getDataForRow(row);
            if (d != null) {
                setSourceIndexSelection(d.indexKey);
            }
        }
    }

    private void jProductsListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        int row = jProductsListTable.getSelectedRow();
        if (myProductListTableModel != null) {
            ProductTableData d = myProductListTableModel.getProductTableDataForRow(row);
            setProductSelection(d);
        }
    }

    private void jChoicesListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        int row = jChoicesListTable.getSelectedRow();
        if (myChoicesListTableModel != null) {
            ChoicesTableData d = myChoicesListTableModel.getChoicesTableDataForRow(row);
            setChoicesSelection(d);
        }
    }

    private void jResultsListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        int row = jResultsListTable.getSelectedRow();
        if (myResultsListTableModel != null) {
            ResultsTableData d = myResultsListTableModel.getResultsTableDataForRow(row);
            setResultsSelection(d);
        }
    }

    protected void setSourceIndexSelection(String key) {
        SourceManager.getInstance().selectIndexKey(key);
        updateContentDescription();
        //clearProductList();
        fillDatatypeList(key, "", "");
    }

    private void setProductSelection(ProductTableData d) {
        // Keep the current datatype to allow it to track over different
        // sources
        // Ref is selected, change source and auto selects reflectivity in
        // new source
        //myLatestClickedDatatype = aData.datatype;
        //myLatestClickedDatatypeVisible = aData.visibleName;
        //System.out.println("ok call fillsubtype with "
        //		+ myLatestClickedDatatype + ", " + aData.datatype);
        if (d != null) {
            fillChoicesList(d.datatype);
        }
    }

    private void setChoicesSelection(ChoicesTableData d) {
        if (d != null) {
            fillRecordList(d.visibleName);
        }
    }

    private void setResultsSelection(ResultsTableData d) {
        if (d != null) {

            // Fire command to load the clicked product
            ProductLoadCommand doIt = new ProductLoadCommand(
                    ProductLoadCaller.FROM_RECORD_PICKER, myIndexName, d.datatype, d.subtype, d.time);
            CommandManager.getInstance().executeCommand(doIt, true);
        }
    }

    /** Update the label at the top for current selection of source,
     * gives a long description of it.
     */
    protected void updateContentDescription() {
        // Get fields of selected index watcher.
        String d = getSelectedSourceDescription();
        jSelectedSourceLabel.setText(d);
        jSelectedSourceLabel.setToolTipText(d);
    }

    protected String getSelectedSourceDescription() {
        String description;

        // Get fields of selected index watcher.
        String s = SourceManager.getInstance().getSelectedIndexKey();

        if (s != null) {
            int aSize = 0;
            int maxSize = 0;
            int totalSize = 0;
            String location = "";
            IndexWatcher info = SourceManager.getInstance().getIndexWatcher(s);
            if (info != null) {
                HistoricalIndex i = info.getIndex();
                if (i != null) {
                    aSize = i.getCurrentHistorySize();
                    maxSize = i.getMaxHistorySize();
                    totalSize = i.getTotalHistorySize();
                    location = info.getPath();
                }
            }

            // Update the content description for selected index
            String shortName = SourceManager.getInstance().getNiceShortName(s);
            String content = String.format("%s @ %s (%d/%d [%d lifetime])",
                    shortName,
                    location,
                    aSize,
                    maxSize,
                    totalSize);
            description = content;
        } else {
            description = " ";
        }
        return description;
    }

    public void updateParts() {

        // Fill in the source list...
        fillSourceList();

        //  mySourcesViewer.setItemCount(0);
        // mySourceList = newList;
        // mySourcesViewer.setItemCount(mySourceList.size());

        // Refresh the table...
      /*  mySourcesViewer.getTable().clearAll();
        if (haveOldSelection){
        mySourcesViewer.getTable().select(selectIndex);
        // Have to update the columns as well....
        newSelection(selectKey);
        }else{
        clearProductList();
        }
         * */

        //myProducts.clearAll(); // interesting...why does it fail on delete then?

        updateContentDescription();

    }

    /** Fill in the list of sources
     * 
     */
    public void fillSourceList() {
        // ----------------------------------------------------------------------
        // Fill in the sources table model with information from the
        // SourceManager IndexWatcher.  Add extra graphic info here too
        SourceManager manager = SourceManager.getInstance();
        ArrayList<IndexWatcher> names = manager.getIndexList();

        // Remember the current selection...
        String selected = SourceManager.getInstance().getSelectedIndexKey();

        // Copy info we need out of index record, add anything else we need
        // Careful...the IndexWatchers might be updating in different threads
        // However, it's ok for us to have old information, the working thread
        // should notify us when it's done changing stuff so we'll get another update
        // later.
        ArrayList<SourceTableData> newList = new ArrayList<SourceTableData>();
        int rowCount = 0;
        int selectIndex = 0;
        String selectKey = null;
        boolean haveOldSelection = false;
        for (IndexWatcher r : names) {
            SourceTableData d = new SourceTableData();
            d.visibleName = SourceManager.getInstance().getNiceShortName(r.getKeyName());
            d.indexKey = r.getKeyName();
            if (d.indexKey.equals(selected)) {
                haveOldSelection = true;
                selectIndex = rowCount;
                selectKey = d.indexKey;
            }
            d.realtime = r.getRealtime();
            d.connected = r.isConnected();
            d.connecting = r.isConnecting();
            d.indexLocation = r.getPath();
            newList.add(d);
            rowCount++;
        }

        // Finally update the table....
        mySourceListTableModel.setDataTypes(newList);
        mySourceListTableModel.fireTableDataChanged();
        myProductListTableModel.setDataTypes(null);
        myProductListTableModel.fireTableDataChanged();
        myChoicesListTableModel.setChoices(null);
        myChoicesListTableModel.fireTableDataChanged();

    }

    public void fillDatatypeList(String indexFilter, String oldDatatype,
            String oldSubtype) {

        // ----------------------------------------------------------------------
        // Fill in product list, given an index name
        // Copy data types over to a sorted tree set, filtered by
        // visibility and name. Call whenever a datatype is added or removed
        // from index.
        ArrayList<ProductTableData> sortedList = new ArrayList<ProductTableData>();

        // Fill first column with available data types, if any
        HistoricalIndex anIndex = SourceManager.getIndexByName(indexFilter);
        if (anIndex != null) {

            Set<String> dataList = anIndex.getDataTypes();

            for (String name : dataList) {

                // Look up product data info for this product
                // String name = iter.next();
                ProductDataInfo theInfo = ProductManager.getInstance().getProductDataInfo(name);

                // If it's visible, add it to our list of products, color it
                // etc...
                if (theInfo.getVisibleInList()) {
                    ProductTableData theData = new ProductTableData();
                    // the awt color is default for data, but we need a swt
                    // color which requires a display pointer
                    java.awt.Color raw = theInfo.getListColor();
                    if (raw != null) {
                        theData.textColor = new Color(raw.getRed(), raw.getGreen(), raw.getBlue());
                        //theData.textColor = new Color(myDisplay, raw.getRed(),
                        //		raw.getGreen(), raw.getBlue());
                        // java.awt.Color raw2 = raw.darker().darker().darker();
                        // theData.background = new Color(myDisplay,
                        // raw2.getRed(), raw2.getGreen(), raw2.getBlue());
                    } else {
                        // Explicitly set for clarity. Null means use system
                        // theme color
                        theData.textColor = null;
                        theData.background = null;
                    }
                    // theData.visibleName =
                    // name+","+theInfo.getName()+";"+theInfo.getAbbreviation();
                    theData.visibleName = theInfo.getListName();
                    theData.datatype = name; // The raw name such as
                    // 'Reflectivity' the index uses
                    // to look up the data
                    sortedList.add(theData);
                }
            }

            Collections.sort(sortedList, new Comparator<ProductTableData>() {

                @Override
                public int compare(ProductTableData arg0, ProductTableData arg1) {
                    return (arg0.visibleName.compareTo(arg1.visibleName));
                }
            });
        }
        myProductListTableModel.setDataTypes(sortedList);

        //  this.myProductsListTableModel
        //  myDataTypes = sortedList; // List is ready to use now

        // When switching products, try to keep the selected datatype (if found)
        // int oldIndex = Collections.binarySearch(sortedList, oldDatatype);
        //datatypeData holder = new datatypeData();
        //holder.visibleName = this.myLatestClickedDatatypeVisible;
        // System.out.println("OLD DATATYPE IS "+oldDatatype);
      /*  int oldIndex = Collections.binarySearch(sortedList, holder,
        new Comparator<datatypeData>() {
        
        @Override
        // FIXME: same search as above, maybe make it non-anonymous
        public int compare(datatypeData o1, datatypeData o2) {
        return (o1.visibleName.compareTo(o2.visibleName));
        }
        });
         */
        /*int actualIndex = oldIndex;
        int aSize = myDataTypes.size();
        if (myStarDatatype) {
        aSize += 1;
        actualIndex += 1;
        }
        myProducts.setItemCount(aSize);
        myProducts.clearAll(); // force virtual refresh
        
        if (oldIndex >= 0) {
        //System.out.println("Selecting OLD INDEX OF " + oldIndex);
        myProducts.select(oldIndex);
        myProducts.showSelection();
        
        //System.out.println("WOULD CALL " + oldDatatype + ", " + oldSubtype);
        //System.out.println("HERE CALL " + myLatestClickedDatatype + ", "
        //		+ myLatestClickedDatatypeVisible);
        
        // It found the visible datatype in current list, so it should be in
        // the index.
        fillSubtypeList(myLatestClickedDatatype);
        // fillSubtypeList(oldDatatype, oldSubtype);
        }
         * 
         */
    }

    public void fillChoicesList(String datatype) {

        String key = SourceManager.getInstance().getSelectedIndexKey();
        if (key != null) {
            HistoricalIndex anIndex = SourceManager.getIndexByName(key);
            if (anIndex != null) {
                mySelection[0] = datatype;
                ArrayList<String> strings = anIndex.getSortedSubTypesForDataType(datatype, true);
                ArrayList<ChoicesTableData> newlist = new ArrayList<ChoicesTableData>();

                for (String s : strings) {
                    ChoicesTableData d = new ChoicesTableData();
                    d.visibleName = s;
                    // Add anything else we need, color, etc...
                    newlist.add(d);
                }
                myChoicesListTableModel.setChoices(newlist);

                // FIXME: rare case of null here? what to do...probably empty
                // subtype list
				/*mySubTypes = strings;
                
                // ------------------------------------------------------------
                // Search the list for latest clicked subtype, we'll stay there
                int oldIndex = Collections.binarySearch(strings, myLatestClickedSubtypeVisible);
                int actualIndex = oldIndex;
                int aSize = mySubTypes.size();
                if (myStarSubtype) {
                aSize += 1;
                actualIndex += 1; // move selection down because of '*'
                if (myLatestClickedSubtypeVisible.equals("*")) {
                oldIndex = 0;
                actualIndex = 0;
                }
                }
                myElevations.setItemCount(aSize);
                myElevations.clearAll();
                if (oldIndex >= 0) {
                myElevations.select(actualIndex);
                myElevations.showSelection();
                // click the subtype
                fillRecordList(myLatestClickedSubtypeVisible);
                } else {
                clearResultsList();
                }*/
            }
        }
    }

    public void fillRecordList(String selectedSubtype) {
        String key = SourceManager.getInstance().getSelectedIndexKey();
        if (key != null) {
            HistoricalIndex anIndex = SourceManager.getIndexByName(key);
            if (anIndex != null) {
                String[] list = new String[2];
                list[0] = mySelection[0];
                list[1] = selectedSubtype;
                mySelection[1] = selectedSubtype;
                setRecordList(key, list);
            }
        }
    }

    /** Ok this is seting the record list with an index and args. */
    public void setRecordList(String indexName, String[] upto) {
        myIndexName = indexName;
        // myDataType = upto[0];
        // mySubType = upto[1];

        HistoricalIndex anIndex = SourceManager.getIndexByName(myIndexName);
        if (anIndex != null) {
            RecordQuery q = anIndex.gatherRecords(upto, false);
            setRecordList(q.matches, indexName, upto[0], upto[1], (q.uniqueSubtypes.size() > 1), (q.uniqueDatatypes.size() > 1));
        } else {
            clearRecords();
        }
    }

    public void clearRecords() {
        myResultsListTableModel.setRecordData(null, false, false);
        jResultsLabel.setText("No record results");

    }

    /** Set our selection of records from a given list of records */
    public void setRecordList(ArrayList<IndexRecord> indexRecords, String indexName, String dataType, String subtype,
            boolean showSubtypes, boolean showDatatypes) {
        myIndexName = indexName;
        //  myDataType = dataType;
        //  mySubType = subtype;

        ArrayList<ResultsTableData> stuffToShow = new ArrayList<ResultsTableData>();
        if (indexRecords != null) {
            Iterator<IndexRecord> iter = indexRecords.iterator();
            while (iter.hasNext()) {
                IndexRecord r = iter.next();
                ResultsTableData data = new ResultsTableData(
                        r.getTime(), r.getTimeStamp(), r.getSubType(),
                        r.getDataType(), "?");
                stuffToShow.add(data);
            }
        }
        this.myResultsListTableModel.setRecordData(stuffToShow, showSubtypes, showDatatypes);
        String shortName = SourceManager.getInstance().getNiceShortName(myIndexName);
        String recordInfo = String.format("Results: %s %s %s (%d found)",
                shortName, dataType, subtype, stuffToShow.size());
        this.jResultsLabel.setText(recordInfo);
    }

    public ProductsView() {
        initComponents();
        initTables();
        
        CommandManager.getInstance().registerView("Products", this);
    }

    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jTopBottomSplitPane = new javax.swing.JSplitPane();

        jSplitPane3 = new javax.swing.JSplitPane();

        // The four scroll panes containing tables...
        jProductsScrollPane = new javax.swing.JScrollPane();
        jChoicesScrollPane = new javax.swing.JScrollPane();
        jSourcesScrollPane = new javax.swing.JScrollPane();
        jResultsScrollPane = new javax.swing.JScrollPane();

        jPanel2 = new javax.swing.JPanel();
        jResultsLabel = new javax.swing.JLabel();
        jResultsLabel = new javax.swing.JLabel("No results");

        setLayout(new MigLayout("fill", "", ""));
        jSelectedSourceLabel = new javax.swing.JLabel("No source");
        add(jSelectedSourceLabel, "dock north");

        jTopBottomSplitPane.setDividerLocation(200);
        jTopBottomSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        add(jTopBottomSplitPane, "growx, growy");

        jSplitPane2 = new javax.swing.JSplitPane();
        jSplitPane2.setDividerLocation(150);
        jSplitPane2.setRightComponent(jSplitPane3);
        jSplitPane2.setLeftComponent(jSourcesScrollPane);
        jTopBottomSplitPane.setTopComponent(jSplitPane2);
        jTopBottomSplitPane.setRightComponent(jPanel2);
        jSplitPane3.setDividerLocation(150);
        jSplitPane3.setLeftComponent(jProductsScrollPane);
        jSplitPane3.setRightComponent(jChoicesScrollPane);

        jPanel2.setLayout(new MigLayout("fill", "", ""));
        jPanel2.add(jResultsLabel, "dock north");
        jPanel2.add(jResultsScrollPane, "growx, growy");


        setPreferredSize(new java.awt.Dimension(300, 349));
    }
}

package org.wdssii.gui.nbm.views;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.ProductManager.ProductDataInfo;
import org.wdssii.gui.SourceManager;
import org.wdssii.gui.SourceManager.SourceCommand;
import org.wdssii.gui.commands.ProductLoadCommand;
import org.wdssii.gui.commands.ProductLoadCommand.ProductLoadCaller;
import org.wdssii.gui.views.ProductPickerView;
import org.wdssii.index.HistoricalIndex;
import org.wdssii.index.HistoricalIndex.RecordQuery;
import org.wdssii.index.IndexRecord;
import org.wdssii.index.IndexWatcher;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//org.wdssii.gui.nbm.views//ProductPicker//EN",
autostore = false)
@TopComponent.Description(preferredID = "ProductPickerTopComponent",
iconBase = "org/wdssii/gui/nbm/views/filter.gif",
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "explorer", openAtStartup = true)
@ActionID(category = "Window", id = "org.wdssii.gui.nbm.views.ProductPickerTopComponent")
@ActionReference(path = "Menu/Window/WDSSII" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_ProductPickerAction",
preferredID = "ProductPickerTopComponent")
/** The Product picker is a view for choosing the data we want to view
 * out of the sources we have.  It's one of the most important views
 * 
 * @author Robert Toomey
 * 
 */
public final class ProductPickerTopComponent extends TopComponent implements ProductPickerView {
    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works

    // For everything BUT delete, just update the parts
    // This could be called by different threads, so use the update method,
    // NOT the thread specific updateParts
    public void SourceCommandUpdate(SourceCommand command) {

        // FIXME: we're not getting these yet, are we?
        InputOutput io = IOProvider.getDefault().getIO("WDSSII", true);
        io.getOut().println("-->Update from Source manager...bleh");
    }
    private String[] mySelection = new String[3];
    private javax.swing.JTable jSourceListTable;
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
    //  protected String myDataType;
    //  protected String mySubType;

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
    private class SourceListTableModel extends AbstractTableModel {

        /** The column headers */
        private final String headers[];
        private ArrayList<SourceTableData> sourcelist = null;

        public SourceListTableModel() {

            // Hardcoded to match bookmarks.
            this.headers = new String[]{
                "Source", "Path"
            };
        }

        @Override
        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public int getRowCount() {
            int size = 0;
            if (sourcelist != null) {
                size = sourcelist.size();
            }
            return size;
        }

        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (sourcelist != null) {
                SourceTableData d = sourcelist.get(row);
                switch (column) {
                    case 0: // name
                        return d.visibleName;
                    case 1:
                        return d.indexLocation;
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

        private void setSourceTableData(ArrayList<SourceTableData> newSourceList) {
            this.sourcelist = newSourceList;
            this.fireTableDataChanged();
        }

        private SourceTableData getSourceTableDataForRow(int row) {
            SourceTableData s = null;
            if (sourcelist != null) {
                if ((row >= 0) && (row < sourcelist.size())) {
                    s = sourcelist.get(row);
                }
            }
            return s;
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

    public ProductPickerTopComponent() {
        initComponents();
        initTables();
        setName(NbBundle.getMessage(ProductPickerTopComponent.class, "CTL_ProductPickerTopComponent"));
        setToolTipText(NbBundle.getMessage(ProductPickerTopComponent.class, "HINT_ProductPickerTopComponent"));
    }

    /** Create the four special tables for the product selection */
    private void initTables() {

        // Could create these with class..
        mySourceListTableModel = new SourceListTableModel();
        myProductListTableModel = new ProductListTableModel();
        myChoicesListTableModel = new ChoicesListTableModel();
        myResultsListTableModel = new ResultsListTableModel();

        jSourceListTable = new javax.swing.JTable();
        jSourceListTable.setModel(mySourceListTableModel);
        jSourceListTable.setFillsViewportHeight(true);
        jSourceListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jSourcesScrollPane.setViewportView(jSourceListTable);

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
            SourceTableData d = mySourceListTableModel.getSourceTableDataForRow(row);
            setSourceIndexSelection(d.indexKey);
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

    protected void setProductSelection(ProductTableData d) {
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

    protected void setChoicesSelection(ChoicesTableData d) {
        if (d != null) {
            fillRecordList(d.visibleName);
        }
    }

    protected void setResultsSelection(ResultsTableData d) {
        if (d != null) {

            // Fire command to load the clicked product
            ProductLoadCommand doIt = new ProductLoadCommand(
                    ProductLoadCaller.FROM_RECORD_PICKER, myIndexName, d.datatype, d.subtype, d.time);
            CommandManager.getInstance().executeCommand(doIt, true);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSelectedSourceLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jTopBottomSplitPane = new javax.swing.JSplitPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        jSplitPane3 = new javax.swing.JSplitPane();
        jProductsScrollPane = new javax.swing.JScrollPane();
        jChoicesScrollPane = new javax.swing.JScrollPane();
        jSourcesScrollPane = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        jResultsLabel = new javax.swing.JLabel();
        jResultsScrollPane = new javax.swing.JScrollPane();

        setPreferredSize(new java.awt.Dimension(300, 349));

        org.openide.awt.Mnemonics.setLocalizedText(jSelectedSourceLabel, org.openide.util.NbBundle.getMessage(ProductPickerTopComponent.class, "ProductPickerTopComponent.jSelectedSourceLabel.text")); // NOI18N

        jTopBottomSplitPane.setDividerLocation(200);
        jTopBottomSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jSplitPane2.setDividerLocation(150);

        jSplitPane3.setDividerLocation(150);
        jSplitPane3.setLeftComponent(jProductsScrollPane);
        jSplitPane3.setRightComponent(jChoicesScrollPane);

        jSplitPane2.setRightComponent(jSplitPane3);
        jSplitPane2.setLeftComponent(jSourcesScrollPane);

        jTopBottomSplitPane.setTopComponent(jSplitPane2);

        org.openide.awt.Mnemonics.setLocalizedText(jResultsLabel, org.openide.util.NbBundle.getMessage(ProductPickerTopComponent.class, "ProductPickerTopComponent.jResultsLabel.text")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jResultsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 364, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jResultsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jResultsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jResultsScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE))
        );

        jTopBottomSplitPane.setRightComponent(jPanel2);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTopBottomSplitPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTopBottomSplitPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSelectedSourceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jSelectedSourceLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
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

    void updateParts() {

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
        mySourceListTableModel.setSourceTableData(newList);
        myProductListTableModel.setDataTypes(null);
        myChoicesListTableModel.setChoices(null);

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
}

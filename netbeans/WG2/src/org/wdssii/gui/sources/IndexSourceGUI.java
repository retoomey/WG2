package org.wdssii.gui.sources;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.commands.ProductLoadCommand;
import org.wdssii.gui.commands.ProductLoadCommand.ProductLoadCaller;
import org.wdssii.gui.swing.RowEntryTable;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.index.HistoricalIndex;
import org.wdssii.index.HistoricalIndex.RecordQuery;
import org.wdssii.index.IndexRecord;

/**
 * The GUI that handles controls for a WDSSII Index. This allows navigating
 * within the index to add ProductFeatures
 *
 * @author Robert Toomey
 */
public class IndexSourceGUI extends javax.swing.JPanel implements SourceGUI {

    private static Logger log = LoggerFactory.getLogger(IndexSourceGUI.class);
    // ------------------------------------------------------------------------
    // Begin Products (three table models, for Products, choices, results)
    //
    /**
     * Storage for displaying the list of products in the Index
     */
    private ProductListTableModel myProductListTableModel;
    private RowEntryTable myProductsTable;
    private String mySelectedProduct;
    
    private static class ProductListTableData {

        public String productName; // Name shown in list
        public String datatype; // The datatype (used to access the record);
        public Color textColor; // The text color for the datatype (null is system
        // default)
        public Color background; // The background color for the datatype (null is
        // system default)
    }

    private class ProductListTableModel extends RowEntryTableModel<ProductListTableData> {

        public static final int PRODUCT_NAME = 0;
        private boolean isRebuilding = false;

        public ProductListTableModel() {
            super(ProductListTableData.class, new String[]{
                        "Products"
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
     * Our custom renderer for drawing the Products table
     */
    private static class ProductListTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof ProductListTableData) {
                ProductListTableData e = (ProductListTableData) value;

                switch (trueCol) {
                    case ProductListTableModel.PRODUCT_NAME:
                        info = e.productName;
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
    // ------------------------------------------------------------------------
    // Begin Choices
    // Pretty much duplicate code, keeping it separate for ease of adding
    // columns...
    /**
     * Storage for displaying the list of products in the Index
     */
    private ChoiceListTableModel myChoiceListTableModel;
    private RowEntryTable myChoicesTable;

    private static class ChoiceListTableData {

        public String choiceName; // Name shown in list
    }

    private class ChoiceListTableModel extends RowEntryTableModel<ChoiceListTableData> {

        public static final int CHOICE_NAME = 0;
        private boolean isRebuilding = false;

        public ChoiceListTableModel() {
            super(ChoiceListTableData.class, new String[]{
                        "Choices"
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
     * Our custom renderer for drawing the Products table
     */
    private static class ChoiceListTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof ChoiceListTableData) {
                ChoiceListTableData e = (ChoiceListTableData) value;

                switch (trueCol) {
                    case ChoiceListTableModel.CHOICE_NAME:
                        info = e.choiceName;
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
    // ------------------------------------------------------------------------
    // Begin Results
    // Pretty much duplicate code, keeping it separate for ease of adding
    // columns...
    private ResultListTableModel myResultListTableModel;
    private RowEntryTable myResultsTable;
    private JLabel jResultsLabel;

    private static class ResultListTableData {

        public ResultListTableData(Date time, String timestamp, String subtype,
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

    private class ResultListTableModel extends RowEntryTableModel<ResultListTableData> {

        public static final int RESULT_NAME = 0;
        private boolean isRebuilding = false;

        public ResultListTableModel() {
            super(ResultListTableData.class, new String[]{
                        "Results"
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
     * Our custom renderer for drawing the Results table
     */
    private static class ResultListTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof ResultListTableData) {
                ResultListTableData e = (ResultListTableData) value;

                switch (trueCol) {
                    case ResultListTableModel.RESULT_NAME:
                        info = e.timestamp;
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
    /**
     * The IndexSource we are using
     */
    private IndexSource mySource;
    /**
     * The selections for picking an IndexRecord
     */
    private String[] mySelection = new String[3];

    /**
     * Creates new form LLHAreaSliceGUI
     */
    public IndexSourceGUI(IndexSource owner) {
        mySource = owner;
        setupComponents();
    }

    /**
     * General update call
     */
    @Override
    public void updateGUI() {
        // Selection changed to us...fill the products list...
        
        updateTables();
    }

    //private JScrollPane myScrollPane;
    @Override
    public void activateGUI(JComponent parent) {
        parent.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        // myScrollPane = new JScrollPane();
        //myScrollPane.setViewportView(this);
        //myScrollPane.setBorder(null);
        // parent.add(myScrollPane, new CC().growX().growY());
        parent.add(this, new CC().growX().growY());
        doLayout();
    }

    @Override
    public void deactivateGUI(JComponent parent) {
        //parent.remove(myScrollPane);
        parent.remove(this);
    }

    
    private void updateTables(){
        if (mySource.getIndex() == null){
            // Clear everything...
            myProductListTableModel.setDataTypes(null);
            myChoiceListTableModel.setDataTypes(null);
            myResultListTableModel.setDataTypes(null);
            myProductListTableModel.fireTableDataChanged();
            myChoiceListTableModel.fireTableDataChanged();
            myResultListTableModel.fireTableDataChanged();
        }else{
            
            // FIXME: need to smart update the tables!!
            fillProductsList();
            myProductListTableModel.fireTableDataChanged();
        }
    }
    private void setupComponents() {

        /**
         * Completely control the layout within the scrollpane. Probably don't
         * want to fill here, let the controls do default sizes
         */
        setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));

        javax.swing.JScrollPane myProductsScrollPane;
        javax.swing.JScrollPane myChoicesScrollPane;
        javax.swing.JScrollPane myResultsScrollPane;

        myProductsScrollPane = new JScrollPane();
        myProductsScrollPane.setBorder(null);
        myChoicesScrollPane = new JScrollPane();
        myChoicesScrollPane.setBorder(null);
        myResultsScrollPane = new JScrollPane();
        myResultsScrollPane.setBorder(null);

        javax.swing.JSplitPane splitProductsChoices = new JSplitPane();
        splitProductsChoices.setOrientation(javax.swing.JSplitPane.HORIZONTAL_SPLIT);
        javax.swing.JSplitPane splitTopBottom = new JSplitPane();
        splitTopBottom.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        myProductListTableModel = new ProductListTableModel();
        myProductsTable = new RowEntryTable();
        myProductsTable.setModel(myProductListTableModel);
        myProductsTable.setFillsViewportHeight(true);
        myProductsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myProductsScrollPane.setViewportView(myProductsTable);
        myProductsTable.setDefaultRenderer(ProductListTableData.class, new ProductListTableCellRenderer());

        myChoiceListTableModel = new ChoiceListTableModel();
        myChoicesTable = new RowEntryTable();
        myChoicesTable.setModel(myChoiceListTableModel);
        myChoicesTable.setFillsViewportHeight(true);
        myChoicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myChoicesScrollPane.setViewportView(myChoicesTable);
        myChoicesTable.setDefaultRenderer(ChoiceListTableData.class, new ChoiceListTableCellRenderer());

        myResultListTableModel = new ResultListTableModel();
        myResultsTable = new RowEntryTable();
        myResultsTable.setModel(myResultListTableModel);
        myResultsTable.setFillsViewportHeight(true);
        myResultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myResultsScrollPane.setViewportView(myResultsTable);
        myResultsTable.setDefaultRenderer(ResultListTableData.class, new ResultListTableCellRenderer());
        
        JPanel resultPanel = new JPanel();
        jResultsLabel = new JLabel("No results");
        resultPanel.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        resultPanel.add(jResultsLabel, new CC().dockNorth());
        resultPanel.add(myResultsScrollPane, new CC().growX().growY());
        
        splitProductsChoices.setLeftComponent(myProductsScrollPane);
        splitProductsChoices.setRightComponent(myChoicesScrollPane);
        splitProductsChoices.setDividerLocation(150);
        splitTopBottom.setTopComponent(splitProductsChoices);
        splitTopBottom.setBottomComponent(resultPanel);
        splitTopBottom.setDividerLocation(150);
      
        add(splitTopBottom, new CC().growX().growY());

        myProductsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jProductsListTableValueChanged(e);
            }
        });

	myProductsTable.addMouseListener(new MouseAdapter(){
		  @Override
		  public void mouseClicked(MouseEvent e){
			 if(e.getClickCount() == 2){
				 JTable target = (JTable)e.getSource();
				 int row = target.getSelectedRow();
        if (myProductListTableModel != null) {
            ProductListTableData d = myProductListTableModel.getDataForRow(row);
	    log.error("BLEH: "+d.datatype);
        HistoricalIndex anIndex = mySource.getIndex();

        if (anIndex != null) {
	IndexRecord r = anIndex.getLastRecordByTime(d.datatype);

	}

        } //
			 } 
		  }
	});

        myChoicesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jChoicesListTableValueChanged(e);
            }
        });

        myResultsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jResultsListTableValueChanged(e);
            }
        });
    }

    private void jProductsListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        int row = myProductsTable.getSelectedRow();
        if (myProductListTableModel != null) {
            ProductListTableData d = myProductListTableModel.getDataForRow(row);
            setProductSelection(d);
        }
    }

    private void jChoicesListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        int row = myChoicesTable.getSelectedRow();
        if (myChoiceListTableModel != null) {
            ChoiceListTableData d = myChoiceListTableModel.getDataForRow(row);
            setChoiceSelection(d);
        }
    }

    private void jResultsListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }
        int row = myResultsTable.getSelectedRow();
        if (myResultListTableModel != null) {
            ResultListTableData d = myResultListTableModel.getDataForRow(row);
            setResultsSelection(d);
        }
    }

    private void setProductSelection(ProductListTableData d) {
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
            myChoiceListTableModel.fireTableDataChanged();
        }
    }

    private void setChoiceSelection(ChoiceListTableData d) {
        if (d != null) {
            fillRecordList(d.choiceName);
        }
    }

    private void setResultsSelection(ResultListTableData d) {
        if (d != null) {
            if (mySource != null) {
                String key = mySource.getKey();

                // Fire command to load the clicked product
                ProductLoadCommand doIt = new ProductLoadCommand(
                        ProductLoadCaller.FROM_RECORD_PICKER, key, d.datatype, d.subtype, d.time);
                CommandManager.getInstance().executeCommand(doIt, true);
            }
        }
    }

    public void fillProductsList() {

        // ----------------------------------------------------------------------
        // Fill in product list, given an index name
        // Copy data types over to a sorted tree set, filtered by
        // visibility and name. Call whenever a datatype is added or removed
        // from index.
        ArrayList<ProductListTableData> sortedList = new ArrayList<ProductListTableData>();

        // Fill first column with available data types, if any
        HistoricalIndex anIndex = mySource.getIndex();

        if (anIndex != null) {

            Set<String> dataList = anIndex.getDataTypes();

            for (String name : dataList) {

                // Look up product data info for this product
                // String name = iter.next();
                ProductManager.ProductDataInfo theInfo = ProductManager.getInstance().getProductDataInfo(name);

                // If it's visible, add it to our list of products, color it
                // etc...
                if (theInfo.getVisibleInList()) {
                    ProductListTableData theData = new ProductListTableData();
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
                    theData.productName = theInfo.getListName();
                    theData.datatype = name; // The raw name such as
                    // 'Reflectivity' the index uses
                    // to look up the data
                    sortedList.add(theData);
                }
            }

            /** Sort products by name */
            Collections.sort(sortedList, new Comparator<ProductListTableData>() {

                @Override
                public int compare(ProductListTableData arg0, ProductListTableData arg1) {
                    return (arg0.productName.compareTo(arg1.productName));
                }
            });
            
            // Clear choice?
            fillChoicesList("");
        } 
        myProductListTableModel.setDataTypes(sortedList);
    }

    public void fillChoicesList(String datatype) {

        mySelectedProduct = datatype;
        HistoricalIndex anIndex = mySource.getIndex();
        if (anIndex != null) {
            mySelection[0] = datatype;
            ArrayList<String> strings = anIndex.getSortedSubTypesForDataType(datatype, true);
            ArrayList<ChoiceListTableData> newlist = new ArrayList<ChoiceListTableData>();

            for (String s : strings) {
                ChoiceListTableData d = new ChoiceListTableData();
                d.choiceName = s;
                // Add anything else we need, color, etc...
                newlist.add(d);
            }
            myChoiceListTableModel.setDataTypes(newlist);
        }
    }

    public void fillRecordList(String selectedSubtype) {
        HistoricalIndex anIndex = mySource.getIndex();
        if (anIndex != null) {
            if (anIndex != null) {
                String[] list = new String[2];
                list[0] = mySelection[0];
                list[1] = selectedSubtype;
                mySelection[1] = selectedSubtype;
                setRecordList(list);
            }
        }
    }

    public void setRecordList(String[] upto) {

        HistoricalIndex anIndex = mySource.getIndex();
        if (anIndex != null) {
            String key = mySource.getKey();
            RecordQuery q = anIndex.gatherRecords(upto, false);
            setRecordList(q.matches, key, upto[0], upto[1], (q.uniqueSubtypes.size() > 1), (q.uniqueDatatypes.size() > 1));
        } else {
            clearRecords();
        }
    }

    /**
     * Set our selection of records from a given list of records
     */
    public void setRecordList(ArrayList<IndexRecord> indexRecords, String indexName, String dataType, String subtype,
            boolean showSubtypes, boolean showDatatypes) {

        ArrayList<ResultListTableData> stuffToShow = new ArrayList<ResultListTableData>();
        if (indexRecords != null) {
            Iterator<IndexRecord> iter = indexRecords.iterator();
            while (iter.hasNext()) {
                IndexRecord r = iter.next();
                ResultListTableData data = new ResultListTableData(
                        r.getTime(), r.getTimeStamp(), r.getSubType(),
                        r.getDataType(), "?");
                stuffToShow.add(data);
            }
        }
        String shortName = mySource.getVisibleName();
        setRecordData(stuffToShow, showSubtypes, showDatatypes);
        String recordInfo = String.format("Results: %s %s %s (%d found)",
                shortName, dataType, subtype, stuffToShow.size());
        jResultsLabel.setText(recordInfo);
    }

    private void setRecordData(ArrayList<ResultListTableData> d,
            boolean showSubtypes, boolean showDatatypes) {
        myResultListTableModel.setDataTypes(d);
        myResultListTableModel.fireTableStructureChanged();
    }

    public void clearRecords() {
        myResultListTableModel.setDataTypes(null);
        jResultsLabel.setText("No record results");

    }
}

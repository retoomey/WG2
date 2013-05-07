package org.wdssii.gui.symbology;

import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.swing.RowEntryTable;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import org.wdssii.xml.iconSetConfig.Symbol;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Panel for editing category unique values....
 *
 * @author Robert Toomey
 */
public class CategoryUniqueValues extends SymbologyGUI {

    private CategoryListTableModel myModel;
    private RowEntryTable myTable;
    private JScrollPane myScrollPane;

    @Override
    public int getType() {
        return Symbology.CATEGORY_UNIQUE_VALUES;
    }

    public CategoryUniqueValues() {
        // setupComponents();
    }

    /**
     * Set up the components. We haven't completely automated this because you
     * never know what little change you need that isn't supported.
     */
    @Override
    public void setupComponents() {

        // Fill the space we have....
        setLayout(new MigLayout("",
                "[grow, fill]",
                "[pref!][grow, fill]"));

        // Create top panel...
        JPanel top = new JPanel();
        add(top, new CC().growX().wrap());

        // Create Category table inside a scrollbar
        JPanel categoryHolder = new JPanel();
        add(categoryHolder, new CC().growX().growY());

        setRootComponent(this);

        // Fill the 'top' part of panel
        ArrayList<String> list = SymbolFactory.getSymbolNameList();
        JComboBox typeList = new JComboBox(list.toArray());
        top.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        top.add(new JLabel("Value Field:"), new CC());
        top.add(typeList, new CC().growX().wrap());

        // Fill the 'bottom' part of panel, the table
        categoryHolder.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        myScrollPane = new JScrollPane();
        initTable();
        categoryHolder.add(myScrollPane, new CC().growX().growY());
 
    }

    /**
     * Storage for displaying the current feature list
     */
    private static class CategoryListTableData {

        public Symbol symbol;  // Symbol shown in list?
        public String value;   // Key for the source lookup...
    }

    private static class CategoryListTableModel extends RowEntryTableModel<CategoryListTableData> {

        public static final int CAT_SYMBOL = 0;
        public static final int CAT_FIELD_VALUE = 1;
        private boolean isRebuilding = false;

        public CategoryListTableModel() {
            super(CategoryListTableData.class, new String[]{
                "Symbol", "Value"
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
    private static class CategoryListTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info;
            int trueCol = table.convertColumnIndexToModel(col);

            // Each row uses a single LayerTableEntry...
            if (value instanceof CategoryListTableData) {
                CategoryListTableData e = (CategoryListTableData) value;

                switch (trueCol) {
                    case CategoryListTableModel.CAT_SYMBOL:
                        String icon = "link_break.png"; // Not connected
                        info = "test";
                        //return getIcon(table, icon, isSelected, cellHasFocus, row, col);
                        break;
                    case CategoryListTableModel.CAT_FIELD_VALUE:
                        info = "field";
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

    private void initTable() {
        myModel = new CategoryListTableModel();
        myTable = new RowEntryTable();
        myTable.setModel(myModel);
        final JTable aTable = myTable;
        final CategoryListTableModel aModel = myModel;

        myTable.setFillsViewportHeight(true);
        myTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myScrollPane.setViewportView(myTable);

    }

    @Override
    public void useSymbology(Symbology symbology) {
        super.useSymbology(symbology);

        // If we're using it, make sure it's set to our mode....
        if (mySymbology.use != Symbology.CATEGORY_UNIQUE_VALUES) {
            mySymbology.use = Symbology.CATEGORY_UNIQUE_VALUES;
            notifyChanged();
        }
    }
}

package org.wdssii.gui.symbology;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.swing.AdvancedListSelectionModel;
import ca.odell.glazedlists.swing.AdvancedTableModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.AttributeTable;
import org.wdssii.gui.renderers.SymbolCellRenderer;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.xml.iconSetConfig.Categories;
import org.wdssii.xml.iconSetConfig.Category;
import org.wdssii.xml.iconSetConfig.Single;
import org.wdssii.xml.iconSetConfig.Symbol;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Panel for editing category unique values.... Experimenting with glazed listed
 * for this one. Good candidate for replacing the similar row based table code
 * I've made and used other places in the display.
 *
 * @author Robert Toomey
 */
public class CategoryUniqueValues extends SymbologyGUI {

    private final static Logger LOG = LoggerFactory.getLogger(CategoryUniqueValues.class);
    private JTable myTable;
    private JScrollPane myScrollPane;
    private JComboBox myColumnChoices;
    private AdvancedTableModel<CategoryListTableData> myTableModel;
    private EventList<CategoryListTableData> myList;
    private AdvancedListSelectionModel<CategoryListTableData> mySelectedModel;
    private JButton myUp, myDown, myRemove, myRemoveAll;
    private Map<String, Integer> mySummerize;

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
        //setBackground(Color.RED);
        // Fill the space we have....
        setLayout(new MigLayout("insets 0",
                "[grow, fill]",
                "[pref!][grow, fill]"));


        //setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        setRootComponent(this);

        // FIXME: Cleanup/reduce miglayout.  Designing right now so
        // easier to think in JPanels

        // Create top panel over table.
        JPanel top = new JPanel();
        add(top, new CC().growX().wrap());

        // Create Category table inside a scrollbar
        JPanel categoryHolder = new JPanel();
        add(categoryHolder, new CC().growX().growY());

        // Create right side
        JPanel rightSide = new JPanel();
        rightSide.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        add(rightSide, new CC().dockEast());

        // Create bottom panel of buttons
        JPanel buttonBox = new JPanel();
        buttonBox.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));

        myColumnChoices = new JComboBox();
        //top.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        top.setLayout(new MigLayout("insets 0",
                "[pref!][grow, fill]",
                "[pref!]"));
        top.add(new JLabel("Value Field:"), new CC());
        top.add(myColumnChoices, new CC().growX().wrap());
        myColumnChoices.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectColumn();
            }
        });
        // Fill the 'bottom' part of panel, the table
        categoryHolder.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        myScrollPane = new JScrollPane();
        initTable();
        categoryHolder.add(myScrollPane, new CC().growX().growY());

        // Create bottom row of buttons...

        JButton addAll = new JButton("Add All Values");  // Add all from current datatable
        buttonBox.add(addAll, new CC().flowX());
        addAll.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAllValues();
            }
        });

        //JButton add1 = new JButton("Add Value...");  // Remove selected
        // buttonBox.add(add1, new CC().flowX());

        JButton rAll = new JButton("Remove All");  // Remove all...
        rAll.setEnabled(false);
        rAll.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAllCategories();
            }
        });
        myRemoveAll = rAll;
        buttonBox.add(rAll, new CC().flowX());

        JButton r = new JButton("Remove");  // Remove selected
        r.setEnabled(false);
        r.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeSelected();
            }
        });
        myRemove = r;
        buttonBox.add(r, new CC().flowX());

        add(buttonBox, new CC().dockSouth());

        JButton up = new JButton(SwingIconFactory.getIconByName("UpArrowIcon"));
        up.setEnabled(false); // no starting selection
        up.setToolTipText("Move category up, later categories are drawn on top of others");
        rightSide.add(up, new CC().wrap());
        up.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpSelected();
            }
        });
        myUp = up;

        JButton down = new JButton(SwingIconFactory.getIconByName("DownArrowIcon"));
        down.setToolTipText("Move category down, later categories are drawn on top of others");
        down.setEnabled(false); // no starting selection
        down.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveDownSelected();
            }
        });
        myDown = down;
        rightSide.add(down, new CC());
        updateGUI();
    }

    /**
     * Storage for displaying the current feature list
     */
    private static class CategoryListTableData {

        public Symbol symbol;  // Symbol shown in list?
        public String value;   // Key for the source lookup...
        public int count;      // Count if known
    }

    private void initTable() {
        JTable aTable = new JTable();
        aTable.setFillsViewportHeight(true);
        aTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myScrollPane.setViewportView(aTable);
        myTable = aTable;

        // This handles the visible list of the table
        EventList<CategoryListTableData> theList = new BasicEventList<CategoryListTableData>();
        myList = theList;
        AdvancedTableModel<CategoryListTableData> theTableModel =
                GlazedListsSwing.eventTableModelWithThreadProxyList(theList, new SymbolTableFormat());
        aTable.setModel(theTableModel);

        // This handles the selection part of the table
        AdvancedListSelectionModel<CategoryListTableData> selectm =
                GlazedListsSwing.eventSelectionModelWithThreadProxyList(myList);
        myTable.setSelectionModel(selectm);
        mySelectedModel = selectm;
        mySelectedModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateSelectionControls();
            }
        });

        SymbolCellRenderer p = new SymbolCellRenderer();
        aTable.setDefaultRenderer(Symbol.class, p);
        myTableModel = theTableModel;
    }

    /**
     * Information on table format
     */
    public class SymbolTableFormat implements AdvancedTableFormat<CategoryListTableData> {

        public static final int CAT_SYMBOL = 0;
        public static final int CAT_FIELD_VALUE = 1;
        public static final int CAT_COUNT = 2;

        public int getColumnCount() {
            return 3;
        }

        public String getColumnName(int column) {
            if (column == 0) {
                return "Symbol";
            } else if (column == 1) {
                return "Value";
            } else {
                return "Count";
            }
        }

        @Override
        public Object getColumnValue(CategoryListTableData e, int i) {
            if (i == 0) {
                if (e.symbol != null) {
                    return e.symbol;
                }
            } else if (i == 1) {
                return e.value;
            } else if (i == 2) {
                return e.count;
            }
            return null;
        }

        @Override
        public Class getColumnClass(int column) {
            if (column == 0) {
                return Symbol.class;
            } else {
                return Object.class;
            }
        }

        @Override
        public Comparator getColumnComparator(int i) {
            return null;
        }
    }

    /**
     * Select attribute column.  If column is different then erase the
     * categories.
     */
    private void selectColumn() {
        String key = (String) myColumnChoices.getSelectedItem();
        String current = mySymbology.getCategories().column;
        if (!key.equals(current)) {
            mySymbology.categories.column = key;
            removeAllCategories();
        }
    }

    /**
     * All all values from current column selection....
     */
    private void addAllValues() {
        if (mySymbology != null) {
            Categories cats = mySymbology.getCategories();
            //String key = (String) myColumnChoices.getSelectedItem();
            String key = cats.column;
            String item = cats.column;
            if (myAttributeTable != null) {
                AttributeTable.AttributeColumn c = myAttributeTable.getAttributeColumn(key);
                if (c != null) {
                    Map<String, Integer> sum = c.summerize();
                    mySummerize = sum;
                    Set<Map.Entry<String, Integer>> e = sum.entrySet();
                    // Single should always exist....
                    Single single = mySymbology.getSingle();
                    Symbol sym = single.getSymbol().copy();

                    // For now, cheap color hack...eventually I'll need color bars
                    // in symbology.  Also doesn't work for black of course...
                    Color max = sym.color;
                    int colorCount = e.size();
                    int stepR = max.getRed()/colorCount;
                    int stepG = max.getGreen()/colorCount;
                    int stepB = max.getBlue()/colorCount;
                    int alpha = 255;
                    int r = 0, g = 0, b = 0;
                   
                    for (Map.Entry<String, Integer> s : e) {
                        LOG.debug("SUM: " + s.getKey() + " == " + s.getValue());
                        sym.color = new Color(r, g, b, alpha);
                        r += stepR;
                        g += stepG;
                        b += stepB;
                        Category cat = new Category(s.getKey(), sym.copy());
                        cats.addCategory(cat);
                    }
                }
            }
        }
        updateTable();
        notifyChanged();
        myTable.repaint();
    }

    /**
     * Completely clear all categories from list and the table
     */
    private void removeAllCategories() {

        mySymbology.getCategories().removeCategories();
        updateTable(); // could just force clear right?
        notifyChanged();
        myTable.repaint();

    }

    private void moveUpSelected() {

        if (myTableModel != null) {
            List<Category> list = mySymbology.getCategories().getCategoryList();
            // Not sure I need lock here...
            myList.getReadWriteLock().readLock().lock();
            int[] rows = myTable.getSelectedRows();
            EventList<CategoryListTableData> select = mySelectedModel.getSelected();
            Categories cats = mySymbology.getCategories();
            int count = 0;
            for (CategoryListTableData a : select) {
                if (cats.moveUpCategory(a.value)) {
                    rows[count] -= 1;
                } else {
                    // if first couldn't move up then none should move up
                    break;
                }
                count++;
            }
            myList.getReadWriteLock().readLock().unlock();
            updateTable();
            notifyChanged();
            myTable.repaint();
            for (int r : rows) {
                myTable.addRowSelectionInterval(r, r);
            }
        }
    }

    private void moveDownSelected() {
        if (myTableModel != null) {
            List<Category> list = mySymbology.getCategories().getCategoryList();
            // Not sure I need lock here...
            myList.getReadWriteLock().readLock().lock();
            int[] rows = myTable.getSelectedRows();
            EventList<CategoryListTableData> select = mySelectedModel.getSelected();

            // Have to go backwards for moving groups to work right...
            ListIterator<CategoryListTableData> i = select.listIterator(select.size());
            Categories cats = mySymbology.getCategories();
            int count = rows.length - 1;
            while (i.hasPrevious()) {
                if (cats.moveDownCategory(i.previous().value)) {
                    rows[count] += 1;
                } else {
                    // Last could move, so none should move
                    break;
                }
                count--;
            }
            myList.getReadWriteLock().readLock().unlock();
            updateTable();
            notifyChanged();
            myTable.repaint();
            for (int r : rows) {
                myTable.addRowSelectionInterval(r, r);
            }
        }
    }

    /**
     * Match button status, etc. to current selection
     */
    private void updateSelectionControls() {
        // FIXME: How to move up multiple discontinuous selections...or
        // is that just too confusing?
        int size = 0;
        if (mySelectedModel != null) {
            size = mySelectedModel.getSelected().size();
        }
        if (myUp != null) {  // If up not create, rest aren't either
            myUp.setEnabled(size > 0);
            myDown.setEnabled(size > 0);
            myRemove.setEnabled(size > 0);
            myRemoveAll.setEnabled(myTableModel.getRowCount() > 0);
        }
    }

    /**
     * Remove selected items from out category list and the table
     */
    private void removeSelected() {
        if (myTableModel != null) {
            List<Category> list = mySymbology.getCategories().getCategoryList();
            // Not sure I need lock here...
            myList.getReadWriteLock().readLock().lock();
            EventList<CategoryListTableData> select = mySelectedModel.getSelected();
            Categories cats = mySymbology.getCategories();
            for (CategoryListTableData a : select) {
                cats.removeCategory(a.value);
            }
            myList.getReadWriteLock().readLock().unlock();
            updateTable();
            notifyChanged();
            myTable.repaint();
        }
    }

    /**
     * Regenerate our table from the category list
     */
    private void updateTable() {

        if (myTableModel != null) {
            Categories cats = mySymbology.getCategories();
            List<Category> list = cats.getCategoryList();
            // Now we can access it directly with glazedlists instead of making a copy
            // and none of that firing model silliness from stock swing.
            // Just have to lock it first.
            myList.getReadWriteLock().writeLock().lock();
            myList.clear();

            for (Category c : list) {
                CategoryListTableData d2 = new CategoryListTableData();
                d2.symbol = c.symbols.get(0);
                d2.value = c.value;
                if (mySummerize != null) {
                    Integer s = mySummerize.get(c.value);
                    if (s != null) {
                        d2.count = s;
                    } else {
                        d2.count = -1;
                    }
                }
                myList.add(d2);
            }

            myList.getReadWriteLock().writeLock().unlock();
        }
        updateSelectionControls();
    }

    public void updateGUI() {
        if (myColumnChoices != null) {
            List<String> list;
            if (myAttributeTable != null) {
                list = myAttributeTable.getAttributeColumns();
            } else {
                list = new ArrayList();
            }
            myColumnChoices.setModel(new DefaultComboBoxModel(list.toArray()));

            if (mySymbology != null) {
                String item = mySymbology.getCategories().column;
                myColumnChoices.setSelectedItem(item);
            }
        }
        updateTable();
    }

    @Override
    public void useSymbology(Symbology symbology) {
        super.useSymbology(symbology);

        // If we're using it, make sure it's set to our mode....
        if (mySymbology.use != Symbology.CATEGORY_UNIQUE_VALUES) {
            mySymbology.use = Symbology.CATEGORY_UNIQUE_VALUES;
            notifyChanged();
        }
        updateGUI();
    }
}

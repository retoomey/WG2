package org.wdssii.gui.views;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableRowSorter;
import org.wdssii.core.RadarInfo;

import org.wdssii.core.SourceBookmarks.*;
import org.wdssii.core.SourceBookmarks;
import org.wdssii.datatypes.builders.Builder;
import org.wdssii.datatypes.builders.NetcdfBuilder;
import org.wdssii.datatypes.builders.NetcdfBuilder.NetcdfFileInfo;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.SourceManager;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.swing.RowEntryTableModel;
import org.wdssii.gui.swing.TableUtil.WG2TableCellRenderer;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.swing.CONUSJPanel;
import org.wdssii.gui.swing.CONUSJPanel.CONUSJPanelListener;

/**
 *
 * SourcesView is a JPanel that handles adding data from various locations
 * to the display.
 * By having a swing panel I can put it into any RCP container I want,
 * such as Eclipse RCP or Netbeans
 * 
 * @author Robert Toomey
 * 
 */
public class SourcesView extends JPanel implements CONUSJPanelListener {

    private ArrayList<BookmarkURLSource> mySourceList;
    private static final String ALLGROUPS = "All";
    private javax.swing.JTable jSourceListTable;
    private final String myDebugList = "Debug List";
    private BookmarkURLDataTableModel myModel;
    private final CONUSJPanel myCONUSPanel;

    @Override
    public void radarClicked(String name) {
        if (jSourceListTable != null) {
            if (myModel != null) {
                Object a = myModel.getDataForKeyField(name);
                if (a != null) {
                    BookmarkURLSource s = (BookmarkURLSource) (a);
                    int index = myModel.getRowIndexOf(s);
                    if (index >= 0) {
                        // We want the event to fire
                        // myBookmarkModel.setRebuilding(true);
                        jSourceListTable.setRowSelectionInterval(index, index);
                        //myBookmarkModel.setRebuilding(false);
                    }
                }
            }
        }

    }

    /** Filter to looks for local data files.  We can make this more 
     * advanced
     */
    private class LocalDataFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
        }

        @Override
        public String getDescription() {
            return "XML Data files";
        }
    }

    /** Filter for local files.
     */
    private class SingleProductFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            String t = f.getName().toLowerCase();
            // FIXME: need to get these from the Builders
            return (t.endsWith(".netcdf") || (t.endsWith(".netcdf.gz"))
                    || (t.endsWith(".xml")) || (t.endsWith(".xml.gz")));
        }

        @Override
        public String getDescription() {
            return "netcdf files or xml files";
        }
    }

    /** A class that uses a BookmarkURLData as its model */
    private class BookmarkURLDataTableModel extends RowEntryTableModel {

        // FIXME: should be an enum class probably...
        public static final int BOOK_NAME = 0;
        public static final int BOOK_LOCATION = 1;
        public static final int BOOK_PATH = 2;
        public static final int BOOK_TIME = 3;
        public static final int BOOK_GROUP = 4;
        /** Group filter */
        private BookmarkRowFilter myRowFilter = new BookmarkRowFilter();

        private void selectLineOf(BookmarkURLSource s) {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        // Set up a row filter for the group combo box
        private class BookmarkRowFilter extends RowFilter<Object, Object> {

            private String myGroupName;
            private boolean myFilterOn = false;

            @Override
            public boolean include(Entry entry) {

                // In our model, every column is the same object.  0 is first column.
                if (myFilterOn) {
                    Object e = entry.getValue(0);
                    if (e instanceof BookmarkURLSource) {
                        BookmarkURLSource s = (BookmarkURLSource) (e);
                        if (s.group.equals(myGroupName)) {
                            return true;
                        }
                        return false;
                    }
                }
                return true;
            }

            public void setGroupToShow(String filter) {
                myGroupName = filter;
                myFilterOn = true;
            }

            public void showAllGroups() {
                myFilterOn = false;
            }
        }

        public BookmarkURLDataTableModel() {
            super(BookmarkURLSource.class, new String[]{
                        "Name", "Location", "Path", "Latest", "Group"});
        }

        public TableRowSorter<BookmarkURLDataTableModel> getGroupModelSorter() {
            TableRowSorter<BookmarkURLDataTableModel> sorter =
                    new TableRowSorter<BookmarkURLDataTableModel>(this);
            sorter.setRowFilter(myRowFilter);

            for (int i = 0; i < BookmarkURLDataTableModel.BOOK_GROUP; i++) {
                Comparator<BookmarkURLSource> c = null;
                switch (i) {

                    case BookmarkURLDataTableModel.BOOK_NAME: {
                        c = new Comparator<BookmarkURLSource>() {

                            @Override
                            public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
                                return (o1.name).compareTo(o2.name);
                            }
                        };
                    }
                    break;
                    case BookmarkURLDataTableModel.BOOK_LOCATION: {
                        c = new Comparator<BookmarkURLSource>() {

                            @Override
                            public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
                                return (o1.location).compareTo(o2.location);
                            }
                        };
                    }
                    break;
                    case BookmarkURLDataTableModel.BOOK_PATH: {
                        c = new Comparator<BookmarkURLSource>() {

                            @Override
                            public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
                                return (o1.path).compareTo(o2.path);
                            }
                        };
                    }
                    break;
                    case BookmarkURLDataTableModel.BOOK_TIME: {
                        c = new Comparator<BookmarkURLSource>() {

                            @Override
                            public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
                                if (o1.date.after(o2.date)) {
                                    return 1;
                                }
                                return -1;
                            }
                        };
                    }
                    break;
                    case BookmarkURLDataTableModel.BOOK_GROUP: {
                        c = new Comparator<BookmarkURLSource>() {

                            @Override
                            public int compare(BookmarkURLSource o1, BookmarkURLSource o2) {
                                return (o1.group).compareTo(o2.group);
                            }
                        };
                        break;
                    }
                }
                sorter.setComparator(i, c);
            }
            return sorter;
        }

        public void setGroupToShow(String filter) {
            myRowFilter.setGroupToShow(filter);
            //this.fireTableStructureChanged();
            this.fireTableDataChanged();
        }

        public void showAllGroups() {
            myRowFilter.showAllGroups();
            // this.fireTableStructureChanged();
            this.fireTableDataChanged();
        }

        @Override
        public String getKeyField(Object data) {
            // From generic, so it better match
            BookmarkURLSource s = (BookmarkURLSource) (data);
            return s.name;
        }
    }

    /** Our custom renderer for our product view table */
    private static class BookmarkTableCellRenderer extends WG2TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean cellHasFocus, int row, int col) {

            // Let super set all the defaults...
            super.getTableCellRendererComponent(table, "",
                    isSelected, cellHasFocus, row, col);

            String info = "";
            int trueCol = table.convertColumnIndexToModel(col);

            if (value instanceof BookmarkURLSource) {
                BookmarkURLSource e = (BookmarkURLSource) value;

                switch (trueCol) {

                    case BookmarkURLDataTableModel.BOOK_NAME:
                        info = e.name;
                        break;
                    case BookmarkURLDataTableModel.BOOK_LOCATION:
                        info = e.location;
                        break;
                    case BookmarkURLDataTableModel.BOOK_PATH:
                        info = e.path;
                        break;
                    case BookmarkURLDataTableModel.BOOK_TIME:
                        info = e.time;
                        break;
                    case BookmarkURLDataTableModel.BOOK_GROUP:
                        info = e.group;
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

    public SourcesView() {

        initComponents();

        myModel = new BookmarkURLDataTableModel();
        jSourceListTable = new javax.swing.JTable();
        jSourceListTable.setModel(myModel);

        jSourceListTable.setFillsViewportHeight(true);
        jSourceListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jSourceTableScrollPane.setViewportView(jSourceListTable);

        BookmarkTableCellRenderer p = new BookmarkTableCellRenderer();
        jSourceListTable.setDefaultRenderer(BookmarkURLSource.class, p);
        jSourceListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jSourceListTableValueChanged(e);
            }
        });

        jSourceListTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                /** On double click, try to add the source */
                if (e.getClickCount() == 2) {
                    addNewSourceFromFields(true, true);
                }
            }
        });
        jSourceListTable.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                /** Have to do enter pressed to snag before the table scroll */
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addNewSourceFromFields(true, true);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        jBookmarkComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[]{
                    "http://tensor.protect.nssl/rindexv2.xml", "file:/D:/testing2.xml", myDebugList}));
        jSourceListTable.setRowSorter(myModel.getGroupModelSorter());

        // Try to fill in table with web image?
        // experimental
        CONUSJPanel ipanel = new CONUSJPanel();
        myCONUSPanel = ipanel;
        javax.swing.JScrollPane holder = new javax.swing.JScrollPane();
        holder.setViewportView(ipanel);
        jBookTabPane.addTab("CONUS", null, holder, "stuff");
        myCONUSPanel.addCONUSJPanelListener(this);

        updateListToCurrent();
        updateCurrentRadarInfo();
    }

    private void initComponents() {

        // Create a tabbed layout for our contents
        setLayout(new MigLayout("fill", "", ""));
        jRootTab = new javax.swing.JTabbedPane();
        add(jRootTab, "grow");

        JPanel p1 = createIndexLoadPanel();
        JPanel p2 = createSingleFilePanel();
        jRootTab.addTab("From index.xml", p1);
        jRootTab.addTab("From single file", p2);
    }
    
    private JPanel createIndexLoadPanel() {

        // The load from index panel ------------------------------------------
        JPanel p = new JPanel();
        p = new JPanel();
        p.setLayout(new MigLayout("fillx, wrap 3", "[pref!][][pref!]", ""));

        p.add(new JLabel("Index URL:"));
        jURLTextField = new javax.swing.JTextField();
        p.add(jURLTextField, "growx");

        JButton b = new javax.swing.JButton("Browse for Index...");
        p.add(b, "right");
        b.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBrowseButtonActionPerformed(evt);
            }
        });
        p.add(new JLabel("Source Name:"));
        jNameTextField = new javax.swing.JTextField();
        p.add(jNameTextField, "growx, spanx 2");

        p.add(new JLabel("History:"));
        jComboBox2 = new javax.swing.JComboBox();
        p.add(jComboBox2, "growx");
        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[]{"All", "30", "60", "90", "120"}));
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });
        b = new JButton("Add this index");
        p.add(b, "right");
        b.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jLoadNewSourceButtonActionPerformed(evt);
            }
        });
        p.add(new JSeparator(), "growx, spanx 3");
        p.add(new JLabel("Bookmarks:"));
        jBookmarkComboBox = new javax.swing.JComboBox();
        p.add(jBookmarkComboBox, "growx, spanx 2");

        jBookmarkComboBox.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBookmarkComboBoxActionPerformed(evt);
            }
        });

        p.add(new JLabel("Group:"));

        jGroupComboBox = new javax.swing.JComboBox();
        p.add(jGroupComboBox, "growx");

        b = new JButton("Refresh");
        p.add(b, "right");
        b.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRefreshButtonActionPerformed(evt);
            }
        });

        jBookTabPane = new javax.swing.JTabbedPane();
        p.add(jBookTabPane, "spanx 3, growx, growy");
        jSourceTableScrollPane = new javax.swing.JScrollPane();
        jBookTabPane.addTab("List", jSourceTableScrollPane);

        // Listener setup ----------------------------------------------------- 
        jURLTextField.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jURLTextFieldActionPerformed(evt);
            }
        });

        jGroupComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[]{"All"}));
        jGroupComboBox.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jGroupComboBoxActionPerformed(evt);
            }
        });

        return p;
    }

    private JPanel createSingleFilePanel() {
        JPanel p = new JPanel();
        p.setLayout(new MigLayout("fillx, wrap 3", "[pref!][][pref!]", ""));

        p.add(new JLabel("URL:"));
        jSingleURLTextField = new javax.swing.JTextField();
        p.add(jSingleURLTextField, "growx");
        JButton b = new JButton("Browse Single File...");
        p.add(b, "right");
        b.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSingleLoadActionPerformed(evt);
            }
        });
        p.add(new JLabel("Product:"));
        jSingleProductTextField = new javax.swing.JTextField();
        p.add(jSingleProductTextField, "growx, spanx 2");
        p.add(new JLabel("Choice:"));
        jSingleChoiceTextField = new javax.swing.JTextField();
        p.add(jSingleChoiceTextField, "growx, spanx 2");
        p.add(new JLabel("Time:"));
        jSingleTimeTextField = new javax.swing.JTextField();
        jSingleTimeTextField.setEditable(false);
        p.add(jSingleTimeTextField, "growx, spanx 2");
        b = new javax.swing.JButton("Add to LOCALFILES");
        p.add(b, "skip 2, right");
        b.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAddLocalButtonActionPerformed(evt);
            }
        });

        return p;
    }

    private void jURLTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {
        String file = doSourceOpenDialog();
        if (file != null) {

            jURLTextField.setText(file + "?p=xml");
        }
    }

    private void jRefreshButtonActionPerformed(java.awt.event.ActionEvent evt) {
        updateListToCurrent();
        updateCurrentRadarInfo();
        myCONUSPanel.repaint();
    }

    private void jLoadNewSourceButtonActionPerformed(java.awt.event.ActionEvent evt) {
        addNewSourceFromFields(false, true);
    }

    private void jBookmarkComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
        // Refresh the list to the new selection...
        updateListToCurrent();

    }

    private void jGroupComboBoxActionPerformed(java.awt.event.ActionEvent evt) {

        // Set the group to the current combo selection.
        String theItem = (String) jGroupComboBox.getSelectedItem();
        setShownGroup(theItem);

    }

    private void jSingleLoadActionPerformed(java.awt.event.ActionEvent evt) {

        URL aURL = doSingleProductOpenDialog();
        if (aURL != null) {
            String text = aURL.toString();
            jSingleURLTextField.setText(text);

            // Now get the file from the URL
            File aFile = Builder.getFileFromURL(aURL);
            String absolutePath = aFile.getAbsolutePath();

            // FIXME: add stuff for xml file
            NetcdfFileInfo info = NetcdfBuilder.getNetcdfFileInfo(absolutePath);
            if (info.success) {
                jSingleProductTextField.setText(info.TypeName);
                jSingleChoiceTextField.setText(info.Choice);
                jSingleTimeTextField.setText(info.Time.toString());
            }
        }
    }

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jAddLocalButtonActionPerformed(java.awt.event.ActionEvent evt) {

        // Check URL format first...
        String text = jSingleURLTextField.getText();
        URL aURL = null;
        try {
            aURL = new URL(text);
        } catch (MalformedURLException ex) {
            String message = "Can't parse the URL.\nNeed something like: 'file:/C:/data.netcdf'\n" + ex.toString();
            JOptionPane.showMessageDialog(null,
                    message,
                    "URL malformed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check product name...
        String product = jSingleProductTextField.getText();

        if (product.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Need a Product name to add this in the products under",
                    "Missing:",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check choice name...
        String choice = jSingleChoiceTextField.getText();
        if (choice.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Need a Choice name to add this in the choices under",
                    "Missing:",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Use the current time for product.  FIXME: could we open it up
        // and get the time?  Maybe
        Calendar cal = Calendar.getInstance();
        Date d = cal.getTime();

        // Ugggh. Params for netcdf differ from params for xml files, so we
        // fudge it for now...
        // Ok we'll get it from the builder I think....
        String[] params = null;
        String s = aURL.toString().toLowerCase();
        if (s.endsWith(".netcdf") || s.endsWith(".netcdf.gz")) {
            params = new String[]{"netcdf", "", product, choice, ""};
        } else if (s.endsWith(".xml") || s.endsWith(".xml.gz")) {
            // Params 0 are of this form for a regular index:
            // 0 - builder name 'W2ALGS'
            // 1 - 'GzippedFile' or some other storage type
            // 2 - Base path such as "http://www/warnings"
            // 3 - 'xmldata' formatter_name
            // 4 - short file such as '1999_ktlx.netcdf.gz'
            params = new String[]{"W2ALGS", "", "", "", ""};
        } else {
            // can't load it...
        }
        // Finally try to add it.
        if (params != null) {
            SourceManager.getInstance().addSingleURL(aURL, product, choice, d, params);
            JOptionPane.showMessageDialog(null, "Added file to local file index",
                    "Add success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    private javax.swing.JTabbedPane jBookTabPane;
    private javax.swing.JComboBox jBookmarkComboBox;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jGroupComboBox;
    private javax.swing.JTextField jNameTextField;
    private javax.swing.JTextField jSingleChoiceTextField;
    private javax.swing.JTextField jSingleProductTextField;
    private javax.swing.JTextField jSingleTimeTextField;
    private javax.swing.JTextField jSingleURLTextField;
    private javax.swing.JScrollPane jSourceTableScrollPane;
    private javax.swing.JTextField jURLTextField;
    private javax.swing.JTabbedPane jRootTab;
    // End of variables declaration                   

    /** Update the table of bookmarks to the current combo box selection */
    private void updateListToCurrent() {
        try {
            // FIXME: should be from GUI, not hardcoded
            String text = (String) jBookmarkComboBox.getSelectedItem();
            BookmarkURLData bookmarks = null;
            if (myDebugList.equals(text)) {
                bookmarks = SourceBookmarks.getFakeBookmarks(20, 5);
            } else {
                URL aURL = new URL(text);
                bookmarks = SourceBookmarks.getBookmarksFromURL(aURL);
            }
            setBookmarks(bookmarks);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(null, "Bookmark must be a valid URL");
        }
    }

    /** Try to update the image/radar info vcp latencies for the CONUS */
    public void updateCurrentRadarInfo() {

        RadarInfo info = new RadarInfo();
        try {
            URL aURL = new URL("http://wdssii.nssl.noaa.gov/web/wdss2/products/radar/systems/conus.png");
            BufferedImage img = ImageIO.read(aURL);
            myCONUSPanel.setImage(img);
            info.gatherRadarInfo();
        } catch (Exception e) {
        }

        // How many did we manage to get successfully?  Note because HTML sucks and isn't XHTML we might
        // have gotta exceptions the entire way...this is ok, we only add DivInfo on success, so worst case
        // we don't have them all.
        myCONUSPanel.setRadarInfo(info);

    }

    public void setShownGroup(String theItem) {
        if (theItem != null) {
            if ("All".equals(theItem)) {
                myModel.showAllGroups();
            } else {
                myModel.setGroupToShow(theItem);
            }
        } else {
            myModel.showAllGroups();
        }
    }

    /** From our manually added table, handle selection of a line by filling
     * in the fields
     */
    private void jSourceListTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }

        BookmarkURLDataTableModel model = (BookmarkURLDataTableModel) jSourceListTable.getModel();
        if (model != null) {
            int row = jSourceListTable.getSelectedRow();

            if (myModel != null) {
                BookmarkURLSource s = (BookmarkURLSource) (myModel.getDataForRow(row));
                if (s != null) {
                    jNameTextField.setText(s.name);
                    jURLTextField.setText(s.path);
                }
            }
        }
    }

    /** Set our visual table list of bookmarks from a given data set of them */
    public void setBookmarks(BookmarkURLData bookmarks) {

        BookmarkURLDataTableModel model = (BookmarkURLDataTableModel) jSourceListTable.getModel();
        mySourceList = bookmarks.data;
        //myVisibleSourceList = mySourceList; // Same for moment
        model.setDataTypes(bookmarks.data);
        model.fireTableDataChanged();

        if (myCONUSPanel != null) {
            myCONUSPanel.setBookmarkList(bookmarks.data);
        }
        // Add each unique group item to list
        jGroupComboBox.removeAllItems();
        jGroupComboBox.addItem(ALLGROUPS);
        Iterator<String> it = bookmarks.groups.iterator();
        int i = 0;
        int select = 0;
        while (it.hasNext()) {
            String current = it.next();
            jGroupComboBox.addItem(current);
            if (current.equalsIgnoreCase("Realtime")) {
                select = i;
            }
            i++;
        }
        jGroupComboBox.setSelectedIndex(select);

        String theItem = (String) jGroupComboBox.getSelectedItem();
        setShownGroup(theItem);
    }

    /** Try to add a source to the display from the currently showing
     * fields
     */
    public void addNewSourceFromFields(boolean confirm, boolean report) {
        String path = jURLTextField.getText();
        String name = jNameTextField.getText();

        // Assuming realtime?  FIXME
        boolean realtime = true;
        boolean connect = true;
        CommandManager.getInstance().executeCommand(
                new SourceAddCommand(name, path, confirm, report, realtime, connect), false);
    }

    public String doSourceOpenDialog() {

        String pickedFile = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new LocalDataFilter());
        chooser.setDialogTitle("Open an Index file");
        // rcp chooiser.setFilterPath("D:/") ?

        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            // pickedFile = chooser.getSelectedFile().getName();
            pickedFile = chooser.getSelectedFile().getAbsolutePath();
            File f = chooser.getSelectedFile();
            try {
                pickedFile = f.toURI().toURL().toString();
            } catch (MalformedURLException ex) {
                pickedFile = "Couldn't parse file location";
            }

        }
        return pickedFile;
    }

    /** Load an individual file into the ManualLoadIndex */
    public URL doSingleProductOpenDialog() {

        URL pickedFile = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new SingleProductFileFilter());
        chooser.setDialogTitle("Add single product");
        // rcp chooiser.setFilterPath("D:/") ?

        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                pickedFile = f.toURI().toURL();
            } catch (MalformedURLException ex) {
                // We assume that chooser knows not to return
                // malformed urls...
            }
        }
        return pickedFile;
    }
}

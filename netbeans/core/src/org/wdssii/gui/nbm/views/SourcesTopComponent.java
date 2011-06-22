package org.wdssii.gui.nbm.views;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.wdssii.core.SourceBookmarks.*;
import org.wdssii.core.SourceBookmarks;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.SourceAddCommand;

@ConvertAsProperties(dtd = "-//org.wdssii.gui.nbm.views//Sources//EN",
autostore = false)
@TopComponent.Description(preferredID = "SourcesTopComponent",
iconBase = "org/wdssii/gui/nbm/views/cart_add.png",
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "explorer", openAtStartup = true)
@ActionID(category = "Window", id = "org.wdssii.gui.nbm.views.SourcesTopComponent")
@ActionReference(path = "Menu/Window/WDSSII" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_SourcesAction",
preferredID = "SourcesTopComponent")
/** The source view is what allows us to browse to various data sources, 
 * by URL, disk location (url too), etc...
 * 
 * @author Robert Toomey
 * 
 */
public final class SourcesTopComponent extends TopComponent {

    private ArrayList<BookmarkURLSource> mySourceList;
    private ArrayList<BookmarkURLSource> myVisibleSourceList;
    private static final String ALLGROUPS = "All";
    private javax.swing.JTable jSourceListTable;
    private final String myDebugList = "Debug List";
    private BookmarkURLDataTableModel myModel;

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

    /** A class that uses a BookmarkURLData as its model */
    private class BookmarkURLDataTableModel extends AbstractTableModel {

        /** The bookmark data structure backing our stuff */
        private BookmarkURLData bookmarks;
        /** The column headers */
        private final String headers[];
        /** Our filter for groups */
        private BookmarkRowFilter myRowFilter = new BookmarkRowFilter();

        // Set up a row filter for the group combo box
        private class BookmarkRowFilter extends RowFilter<Object, Object> {

            private String myGroupName;
            private boolean myFilterOn = false;

            @Override
            public boolean include(Entry entry) {
                String pop = entry.getStringValue(4);
                if (myFilterOn) {
                    return pop.equals(myGroupName);
                } else {
                    return true;
                }
            }

            public void setGroupToShow(String filter) {
                myGroupName = filter;
                myFilterOn = true;
            }

            public void showAllGroups() {
                myFilterOn = false;
            }
        }

        public BookmarkURLDataTableModel(BookmarkURLData b) {
            this.bookmarks = b;

            // Hardcoded to match bookmarks.
            this.headers = new String[]{
                "Name", "Location", "Path", "Latest", "Group"
            };

        }

        public TableRowSorter<BookmarkURLDataTableModel> getGroupModelSorter() {
            TableRowSorter<BookmarkURLDataTableModel> sorter =
                    new TableRowSorter<BookmarkURLDataTableModel>(this);
            sorter.setRowFilter(myRowFilter);
            return sorter;
        }

        public void setBookmarks(BookmarkURLData b) {
            this.bookmarks = b;
            this.fireTableDataChanged();
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
        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public int getRowCount() {
            int size = 0;
            if (bookmarks != null) {
                size = bookmarks.data.size();
            }
            return size;
        }

        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Object getValueAt(int row, int column) {

            if (bookmarks != null) {
                BookmarkURLSource bookmark = bookmarks.data.get(row);
                switch (column) {
                    case 0: // name
                        return bookmark.name;
                    case 1:
                        return bookmark.location;
                    case 2:
                        return bookmark.path;
                    case 3:
                        return bookmark.time;
                    case 4:
                        return bookmark.group;
                    default:
                        return "";
                }
            } else {
                return "";
            }
        }

        public BookmarkURLSource getBookmarkURLSourceForRow(int row) {
            BookmarkURLSource s = null;
            if (bookmarks != null) {
                if ((row >= 0) && (row < bookmarks.data.size())) {
                    s = bookmarks.data.get(row);
                }
            }
            return s;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
        }
    }

    public SourcesTopComponent() {
        initComponents();

        // Have to create our virtual table within the GUI
        jSourceListTable = new javax.swing.JTable();
        myModel = new BookmarkURLDataTableModel(null);
        jSourceListTable.setModel(myModel);
        jSourceListTable.setFillsViewportHeight(true);
        jSourceListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jSourceTableScrollPane.setViewportView(jSourceListTable);
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
                    addNewSourceFromFields();
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
                    addNewSourceFromFields();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        jBookmarkComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[]{
                    "http://tensor.protect.nssl/rindexv2.xml", "file:/Q:/testing.xml", myDebugList}));
        jSourceListTable.setRowSorter(myModel.getGroupModelSorter());
        updateListToCurrent();

        setName(NbBundle.getMessage(SourcesTopComponent.class, "CTL_SourcesTopComponent"));
        setToolTipText(NbBundle.getMessage(SourcesTopComponent.class, "HINT_SourcesTopComponent"));

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jNameTextField = new javax.swing.JTextField();
        jURLTextField = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jGroupComboBox = new javax.swing.JComboBox();
        jBrowseButton = new javax.swing.JButton();
        jRefreshButton = new javax.swing.JButton();
        jComboBox2 = new javax.swing.JComboBox();
        jLoadNewSourceButton = new javax.swing.JButton();
        jBookmarkComboBox = new javax.swing.JComboBox();
        jSourceTableScrollPane = new javax.swing.JScrollPane();

        setToolTipText(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel3.text")); // NOI18N

        jNameTextField.setText(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jNameTextField.text")); // NOI18N

        jURLTextField.setText(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jURLTextField.text")); // NOI18N
        jURLTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jURLTextFieldActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel4.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel5.text")); // NOI18N

        jGroupComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All" }));
        jGroupComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jGroupComboBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jBrowseButton, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jBrowseButton.text")); // NOI18N
        jBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBrowseButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jRefreshButton, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jRefreshButton.text")); // NOI18N
        jRefreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRefreshButtonActionPerformed(evt);
            }
        });

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All", "30", "60", "90", "120" }));

        org.openide.awt.Mnemonics.setLocalizedText(jLoadNewSourceButton, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLoadNewSourceButton.text")); // NOI18N
        jLoadNewSourceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jLoadNewSourceButtonActionPerformed(evt);
            }
        });

        jBookmarkComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBookmarkComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.Alignment.LEADING, 0, 255, Short.MAX_VALUE)
                            .addComponent(jURLTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLoadNewSourceButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jBrowseButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
            .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jGroupComboBox, 0, 355, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jBookmarkComboBox, 0, 276, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jRefreshButton)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jURLTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLoadNewSourceButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jRefreshButton)
                    .addComponent(jBookmarkComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jGroupComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(19, 19, 19))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSourceTableScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSourceTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jURLTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jURLTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jURLTextFieldActionPerformed

    private void jBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBrowseButtonActionPerformed
        String file = doSourceOpenDialog();
        if (file != null) {

            jURLTextField.setText(file + "?p=xml");
        }
    }//GEN-LAST:event_jBrowseButtonActionPerformed

    private void jRefreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRefreshButtonActionPerformed
        updateListToCurrent();
    }//GEN-LAST:event_jRefreshButtonActionPerformed

    private void jLoadNewSourceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jLoadNewSourceButtonActionPerformed
        addNewSourceFromFields();
    }//GEN-LAST:event_jLoadNewSourceButtonActionPerformed

    private void jBookmarkComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBookmarkComboBoxActionPerformed
        // Refresh the list to the new selection...
        updateListToCurrent();

    }//GEN-LAST:event_jBookmarkComboBoxActionPerformed

    private void jGroupComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jGroupComboBoxActionPerformed

        // Set the group to the current combo selection.
        String theItem = (String) jGroupComboBox.getSelectedItem();
        setShownGroup(theItem);

    }//GEN-LAST:event_jGroupComboBoxActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox jBookmarkComboBox;
    private javax.swing.JButton jBrowseButton;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jGroupComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JButton jLoadNewSourceButton;
    private javax.swing.JTextField jNameTextField;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton jRefreshButton;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JScrollPane jSourceTableScrollPane;
    private javax.swing.JTextField jURLTextField;
    // End of variables declaration//GEN-END:variables

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
            if (row > -1) {
                int modelRow = jSourceListTable.convertRowIndexToModel(row);
                if (modelRow > -1) {
                    BookmarkURLSource s = model.getBookmarkURLSourceForRow(modelRow);
                    if (s != null) {
                        jNameTextField.setText(s.name);
                        jURLTextField.setText(s.path);
                    }
                }
            }
        }
    }

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

    /** Set our visual table list of bookmarks from a given data set of them */
    public void setBookmarks(BookmarkURLData bookmarks) {

        BookmarkURLDataTableModel model = (BookmarkURLDataTableModel) jSourceListTable.getModel();
        mySourceList = bookmarks.data;
        myVisibleSourceList = mySourceList; // Same for moment
        model.setBookmarks(bookmarks);

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
    public void addNewSourceFromFields() {
        String path = jURLTextField.getText();
        String name = jNameTextField.getText();

        // Assuming realtime?  FIXME
        boolean realtime = true;
        boolean connect = true;
        CommandManager.getInstance().executeCommand(new SourceAddCommand(name, path, true, realtime, connect), false);
    }

    public String doSourceOpenDialog() {

        String pickedFile = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new LocalDataFilter());
        chooser.setDialogTitle("Load local source");
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
}

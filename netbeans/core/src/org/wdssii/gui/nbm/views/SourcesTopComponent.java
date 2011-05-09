package org.wdssii.gui.nbm.views;

import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.wdssii.core.SourceBookmarks.*;
import org.wdssii.core.SourceBookmarks;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//org.wdssii.gui.nbm.views//Sources//EN",
autostore = false)
@TopComponent.Description(preferredID = "SourcesTopComponent",
iconBase = "org/wdssii/gui/nbm/views/cart_add.png",
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "explorer", openAtStartup = false)
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

    /** A class that uses a BookmarkURLData as its model */
    private class BookmarkURLDataTableModel extends AbstractTableModel {

        /** The bookmark data structure backing our stuff */
        private BookmarkURLData bookmarks;
        /** The column headers */
        private final String headers[];

        public BookmarkURLDataTableModel(BookmarkURLData b) {
            this.bookmarks = b;

            // Hardcoded to match bookmarks.
            this.headers = new String[]{
                "Name", "Location", "Path", "Latest", "Group"
            };
        }

        public void setBookmarks(BookmarkURLData b) {
            this.bookmarks = b;
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
        BookmarkURLDataTableModel model = new BookmarkURLDataTableModel(null);
        jSourceListTable.setModel(model);
        jSourceListTable.setFillsViewportHeight(true);
        jSourceListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jSourceTableScrollPane.setViewportView(jSourceListTable);
        jSourceListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                jSourceListTableValueChanged(e);
            }
        });

        // Enable basic sorting on each column
        TableRowSorter<BookmarkURLDataTableModel> sorter =
                new TableRowSorter<BookmarkURLDataTableModel>(model);
        jSourceListTable.setRowSorter(sorter);

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
        jTextField4 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jGroupComboBox = new javax.swing.JComboBox();
        jBrowseButton = new javax.swing.JButton();
        jRefreshButton = new javax.swing.JButton();
        jComboBox2 = new javax.swing.JComboBox();
        jLoadNewSourceButton = new javax.swing.JButton();
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

        jTextField4.setText(org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jTextField4.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(SourcesTopComponent.class, "SourcesTopComponent.jLabel5.text")); // NOI18N

        jGroupComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All" }));

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
                        .addComponent(jTextField4, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE)
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
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jRefreshButton))
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
        // TODO add your handling code here:
        if (JOptionPane.showConfirmDialog(new JFrame(),
                "Do you want to quit this application ?", "Title",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            System.exit(0);
        }

    }//GEN-LAST:event_jBrowseButtonActionPerformed

    private void jRefreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRefreshButtonActionPerformed
        try {
            // FIXME: should be from GUI, not hardcoded
            URL aURL = new URL("http://tensor.protect.nssl/rindexv2.xml");
            //BookmarkURLData bookmarks = SourceBookmarks.getBookmarksFromURL(aURL);
            BookmarkURLData bookmarks = SourceBookmarks.getFakeBookmarks(20, 5);

            setBookmarks(bookmarks);
        } catch (MalformedURLException e) {
            // FIXME: dialog?
        }
    }//GEN-LAST:event_jRefreshButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
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
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jURLTextField;
    // End of variables declaration//GEN-END:variables

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
        // FIXME: filter by group into sublist...?
        model.setBookmarks(bookmarks);

        // Add each unique group item to list
        jGroupComboBox.removeAllItems();
        jGroupComboBox.addItem(ALLGROUPS);
        Iterator<String> it = bookmarks.groups.iterator();
        int i = 0;
        int select = 0;
        while (it.hasNext()) {
            String current = it.next();
            // RCP  myGroupText.add(current);
            jGroupComboBox.addItem(current);
            if (current.equalsIgnoreCase("Realtime")) {
                select = i;
            }
            i++;
        }
        jGroupComboBox.setSelectedIndex(select);
        //RCP++ setShownGroup("Realtime");
    }
}

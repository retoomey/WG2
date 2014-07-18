package org.wdssii.gui.products;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.W2Config;
import org.wdssii.datatypes.AttributeTable;
import org.wdssii.datatypes.DataType;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.swing.SwingGUIPlugInPanel;
import org.wdssii.gui.symbology.SymbologyFactory;
import org.wdssii.gui.symbology.SymbologyGUI;
import org.wdssii.gui.symbology.SymbologyGUI.SymbologyGUIListener;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * JPanel for changing symbology of our products
 *
 * @author Robert Toomey
 */
public class SymbologyJPanel extends JPanel implements SymbologyGUIListener {

    private final static Logger LOG = LoggerFactory.getLogger(SymbologyJPanel.class);
    private final static String NO_URL = "No file at:";
    private JPanel myPanel = null;
    private JPanel myGUIHolder;
    private SymbologyGUI myCurrentGUI = null;
    private ProductFeature myProductFeature = null;
    private Symbology mySymbology = null;
    private JCheckBox mySecondLatLonButton;
    private JCheckBox myMergeButton;
    private JComboBox myList;
    private JLabel myURLLabel;
    private JButton mySave;
    private JButton myRevert;
    private boolean mySymbologyDirty = false;

    public SymbologyJPanel(ProductFeature prod, Component location, String myMessage) {
        init(prod, location, myMessage);
    }

    /**
     * Create the GUI controls for symbology
     */
    private void init(ProductFeature prod, Component location, String myMessage) {
        myProductFeature = prod;
        Symbology orgSymbology = prod.getProduct().getSymbology();
        // Deep copy it to avoid having to synchronize
        mySymbology = new Symbology(orgSymbology);

        setLayout(new MigLayout(new LC().fill().insetsAll("2"), null, null));

        JLabel urlPath = new JLabel("");
        myURLLabel = urlPath;

        JButton save = new JButton("Save");
        save.setToolTipText("Save symbology to the shown default path for this product");
        mySave = save;
        mySave.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveCurrentSymbology();
            }
        });
        JButton revert = new JButton("Revert");
        revert.setToolTipText("Revert symbology to last saved symbology for this product");
        myRevert = revert;
        myRevert.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                revertCurrentSymbology();
            }
        });
        JSeparator sep1 = new JSeparator();
        // Top combo box allowing change in symbology classification
        final String listData[] = {
            "Single Symbol",
            "Categories:Unique Values",};
        final JComboBox theList = new JComboBox();
        JPanel topDock = new JPanel();
        topDock.setLayout(new MigLayout(new LC().fill().insetsAll("2"), null, null));
        topDock.setLayout(new MigLayout("wrap 4", "[pref!][grow][pref!][align right]", // 3 columns, 
                "[pref!|pref!]")); // 2 rows pref height
        //topDock.add(new JLabel("Symbology:"), new CC().flowX());
        add(topDock, new CC().dockNorth());
        theList.setModel(new DefaultComboBoxModel(listData));
        myList = theList;
        theList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox b = (JComboBox) e.getSource();
                String s = (String) b.getSelectedItem();
                SymbologyGUI newControls = SymbologyFactory.getSymbologyByName(s);
                changeOutSymbologyGUI(newControls);
            }
        });


        // The extra information panel...
        myGUIHolder = new JPanel();
        add(myGUIHolder, new CC().growX().growY());

        // Create the south button panel

        mySecondLatLonButton = new JCheckBox("Use 2nd LatLon");
        mySecondLatLonButton.setToolTipText("Use attributes Latitude2/Longitude2 for direction line");
        mySecondLatLonButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (mySecondLatLonButton.isSelected()) {
                    mySymbology.use2ndlatlon = Symbology.USE_2ND_LAT_LON;
                } else {
                    mySymbology.use2ndlatlon = Symbology.DONT_USE_2ND_LAT_LON;
                }
                symbologyChanged();
            }
        });

        myMergeButton = new JCheckBox("Merge Points");
        myMergeButton.setToolTipText("Level of detail merge common points on screen into one.");
        myMergeButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (myMergeButton.isSelected()) {
                    mySymbology.merge = Symbology.MERGE_CATEGORIES;
                } else {
                    mySymbology.merge = Symbology.MERGE_NONE;
                }
                LOG.debug("Symbology " + mySymbology + ", set to " + mySymbology.merge);
                symbologyChanged();
            }
        });
        topDock.add(urlPath, new CC().span(2));
        topDock.add(save, new CC());
        topDock.add(revert, new CC().wrap());

        topDock.add(new JLabel("Type:"), new CC());
        topDock.add(theList, new CC());

        // FIXME: Make drop down menu of options?
        topDock.add(mySecondLatLonButton, new CC().span(1));
        topDock.add(myMergeButton, new CC().span(1));
        sep1.setBackground(Color.BLACK);
        sep1.setForeground(Color.WHITE);
        topDock.add(sep1, new CC().growX().span(4));
        mySave.setEnabled(false);
        myRevert.setEnabled(false);

        initGUIToSymbology();

        setVisible(true);
    }

    public void initGUIToSymbology() {
        // Initial symbology GUI based on Symbology.use value...
        // Up to the GUI to parse Symbology properly.
        // Since they all modify a single Symbology object, doesn't matter
        // if user switches, we keep each 'state' of full symbology...kinda
        // overkill but this allows for instance having a single symbol, and
        // keeping your category settings to 'switch' back to instead of them
        // just going away with a change in use type.
        SymbologyGUI newControls = SymbologyFactory.getSymbologyGUIFor(mySymbology);
        changeOutSymbologyGUI(newControls);
    }

    public void changeOutSymbologyGUI(SymbologyGUI gui) {

        // Get the data table for this product, if any...
        AttributeTable current = null;
        if (myProductFeature != null) {
            DataType d = myProductFeature.getProduct().getRawDataType();
            if (d != null) {
                if (d instanceof AttributeTable) {
                    current = (AttributeTable) (d);
                }
            }
        }
        gui.addListener(this);
        gui.useSymbology(mySymbology);  // eh?  same thing...
        gui.useAttributeTable(current);

        if (myList != null) {
            // Select the string matching the symbology display name...
            String name = gui.getDisplayName();
            myList.setSelectedItem(name);
        }
        gui.setupComponents();

        SwingGUIPlugInPanel.install(myGUIHolder, gui);
        myCurrentGUI = gui;

        updateControls();
    }

    @Override
    public void symbologyChanged() {
        mySymbologyDirty = true;
        setProductSymbology();
    }

    /**
     * Make product use a copy of our current symbology
     */
    public void setProductSymbology() {

        // Have product use a copy of the symbology we are editing...
        final Symbology symbology = new Symbology(mySymbology);
        myProductFeature.getProduct().setSymbology(symbology);
        updateControls();
        AnimateManager.updateDuringRender(); // bleh 
    }

    public void updateControls() {
        if (mySymbology != null) {
            if (myURLLabel != null) {
                URL theURL = mySymbology.getURL();
                String out;
                if (theURL == null) {
                    String dataName = mySymbology.getDataName();
                    URL pref = W2Config.getPreferredDir(ProductManager.SYMBOLOGY);
                    out = NO_URL + pref.getFile() + dataName + ".xml";
                } else {
                    out = theURL.toExternalForm();
                }
                // Replace center of string with "..." if length too much to 
                // save screen space.
                int s = out.length();
                final int max = 43;
                if (s > max) {
                    out = out.substring(0, 20) + "..." + out.substring(s - 20, s);
                }
                myURLLabel.setText(out);
            }
            if (myMergeButton != null) {
                myMergeButton.setSelected(mySymbology.merge == Symbology.MERGE_CATEGORIES);
            }
            if (mySecondLatLonButton != null) {
                mySecondLatLonButton.setSelected(mySymbology.use2ndlatlon == Symbology.USE_2ND_LAT_LON);
            }
            if (mySave != null) {
                mySave.setEnabled(mySymbologyDirty);
            }
            if (myRevert != null) {
                myRevert.setEnabled(mySymbologyDirty && (mySymbology.getURL() != null));
            }
        } else {
            LOG.error("Null symbology in GUI...how?  Fix this");
        }
    }

    /**
     * Check for actual product in feature to have changed and update
     * accordingly.
     */
    public void updateProduct() {

        // Update the attribute table of the GUI subpanel...
        if (myCurrentGUI != null) {
            // Get the data table for this product, if any...
            AttributeTable current = null;
            if (myProductFeature != null) {
                DataType d = myProductFeature.getProduct().getRawDataType();
                if (d != null) {
                    if (d instanceof AttributeTable) {
                        current = (AttributeTable) (d);
                    }
                }
            }
            myCurrentGUI.useAttributeTable(current);
        }
    }

    public void saveCurrentSymbology() {
        String error = myProductFeature.getProduct().saveSymbology(mySymbology);
        if (!error.isEmpty()) {
            LOG.debug("ERROR RETURNED IS " + error);
        }
        mySymbologyDirty = (!error.isEmpty());
        updateControls();
    }

    public void revertCurrentSymbology() {
        LOG.debug("REVERTING SYMBOLOGY");
        if (mySymbology != null) {
            String oldName = mySymbology.getDataName();
            URL aURL = mySymbology.getURL();
            if (aURL != null) {
                Symbology s = ProductManager.loadSymbology(mySymbology.getURL());
                if (s != null) {
                    s.setDataName(oldName);
                    mySymbology = s;
                    mySymbologyDirty = false;

                    // Make product use this symbology and update our GUI
                    setProductSymbology();
                    initGUIToSymbology();
                }
            }
        }
    }
}

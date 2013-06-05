package org.wdssii.gui.products;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.AttributeTable;
import org.wdssii.datatypes.DataType;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.SwingGUIPlugInPanel;
import org.wdssii.gui.symbology.SymbolGUI;
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
    private JPanel myPanel = null;
    private JPanel myGUIHolder;
    private SymbologyGUI myCurrentGUI = null;
    private ProductFeature myProductFeature = null;
    private Symbology mySymbology = null;
    private JCheckBox myMergeButton;
    private JComboBox myList;

    public SymbologyJPanel(ProductFeature prod, Component location, String myMessage) {
        init(prod, location, myMessage);
    }

    private void init(ProductFeature prod, Component location, String myMessage) {
        //setBackground(Color.BLUE);
        myProductFeature = prod;
        Symbology orgSymbology = prod.getProduct().getSymbology();
        // Deep copy it to avoid having to synchronize
        mySymbology = new Symbology(orgSymbology);

        setLayout(new MigLayout(new LC().fill().insetsAll("2"), null, null));

        // Top combo box allowing change in symbology classification
        final String listData[] = {
            "Single Symbol",
            "Categories:Unique Values",};
        final JComboBox theList = new JComboBox();
        JPanel topDock = new JPanel();
        topDock.setLayout(new MigLayout(new LC().fill().insetsAll("2"), null, null));
        topDock.add(theList, new CC().flowX());
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

        // Initial symbology GUI based on Symbology.use value...
        // Up to the GUI to parse Symbology properly.
        // Since they all modify a single Symbology object, doesn't matter
        // if user switches, we keep each 'state' of full symbology...kinda
        // overkill but this allows for instance having a single symbol, and
        // keeping your category settings to 'switch' back to instead of them
        // just going away with a change in use type.
        SymbologyGUI newControls = SymbologyFactory.getSymbologyGUIFor(mySymbology);

        // The extra information panel...
        myGUIHolder = new JPanel();
        add(myGUIHolder, new CC().growX().growY());

        // Create the south button panel
        //JPanel buttonPanel = new JPanel();
        //buttonPanel.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
        //  JCheckBox button = new JCheckBox("Live");  // This will be global I think...
        //buttonPanel.add(button, new CC().flowX());
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
        //buttonPanel.add(myMergeButton, new CC().flowX());
        //add(buttonPanel, new CC().dockSouth());
        topDock.add(myMergeButton, new CC().flowX());
        //topDock.add(button, new CC().flowX());
        changeOutSymbologyGUI(newControls);

        setVisible(true);
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
        if (myMergeButton != null) {
            myMergeButton.setSelected(mySymbology.merge == Symbology.MERGE_CATEGORIES);
        }
        if (myList != null) {
            // Select the string matching the symbology display name...
            String name = gui.getDisplayName();
            myList.setSelectedItem(name);
        }
        gui.setupComponents();

        SwingGUIPlugInPanel.install(myGUIHolder, gui);
        myCurrentGUI = gui;
    }

    @Override
    public void symbologyChanged() {
        final Symbology symbology = new Symbology(mySymbology);  //Copy it AGAIN
        myProductFeature.getProduct().setSymbology(symbology);
        if (myMergeButton != null) {
            myMergeButton.setSelected(mySymbology.merge == Symbology.MERGE_CATEGORIES);
        }
        AnimateManager.updateDuringRender(); // bleh
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
}

package org.wdssii.gui.products;

import com.jidesoft.swing.JideSplitPane;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.SwingGUIPlugInPanel;
import org.wdssii.gui.renderers.CategoryUniqueValues;
import org.wdssii.gui.renderers.SingleSymbol;
import org.wdssii.gui.renderers.SymbolGUI;
import org.wdssii.gui.renderers.SymbologyFactory;
import org.wdssii.gui.renderers.SymbologyGUI;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Dialog for changing symbology of our products Not sure where this belongs.
 *
 * (alpha)
 *
 * @author Robert Toomey
 */
public class SymbologyDialog extends JDialog {

    private static Logger log = LoggerFactory.getLogger(SymbologyDialog.class);
    private JPanel myPanel = null;
    private JButton myOKButton;
    private JPanel myGUIHolder;
    private SymbolGUI myCurrentGUI = null;
    private JButton mySymbolButton;
    private Product myProduct = null;

    // Because Java is brain-dead with JDialog/JFrame silliness
    public SymbologyDialog(Product prod, JFrame owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(prod, location, myMessage);
    }

    public SymbologyDialog(Product prod, JDialog owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(prod, location, myMessage);
    }

    private void init(Product prod, Component location, String myMessage) {

        myProduct = prod;
        setTitle("Symbology");
        Container content = getContentPane();

        // Root panel, containing split pane top and buttons bottom
        JPanel p;
        myPanel = p = new JPanel();
        p.setLayout(new MigLayout(new LC().fill().insetsAll("10"), null, null));

        // The extra information panel...
        myGUIHolder = new JPanel();
        //myGUIHolder.setSize(200, 50);
        //p.add(myGUIHolder, new CC().growX().growY().pushY().span().wrap());

        JPanel buttonPanel = new JPanel();

        // FIXME: Get list from the Datatype right?
        // DataType will have to tell us the symbology types allowed...
        // FIXME: How to generalize to generic shapefile attribute tables
        // such as point maps....
        String listData[] = {
            "Single Symbol",
            "Categories:Unique Values",};

        // Which way do we link Symbology to DataType?
        // Do we ask product for valid symbology types..what if symbology is
        // WRONG for product?  Seems like Symbology should be 'highter' than
        // the DataType. Or do we ask Symbology if it can handle a given
        // Datatype?
        ArrayList<String> list = SymbologyFactory.getSymbologyNameList();

        Symbology symbology = prod.getSymbology();

        final JList theList = new JList(listData);
        mySymbolButton = new JButton("Edit Symbol");

        JideSplitPane s = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        s.setProportionalLayout(true);
        s.setShowGripper(true);
        s.add(new JScrollPane(theList));
        s.add(new JScrollPane(myGUIHolder));

        // Initial symbology GUI based on Symbology.use value...
        // Up to the GUI to parse Symbology properly.
        // Since they all modify a single Symbology object, doesn't matter
        // if user switches, we keep each 'state' of full symbology...kinda
        // overkill but this allows for instance having a single symbol, and
        // keeping your category settings to 'switch' back to instead of them
        // just going away with a change in use type.
        SymbologyGUI newControls = SymbologyFactory.getSymbologyGUIFor(symbology);
        SwingGUIPlugInPanel.install(myGUIHolder, newControls);

        // Select the string matching the symbology display name...
        String name = newControls.getDisplayName();
        int foundIt = 0;
        ListModel model = theList.getModel();
        int aSize = model.getSize();
        for (int i = 0; i < aSize; i++) {
            String current = (String) model.getElementAt(i);
            if (current.equalsIgnoreCase(name)) {
                foundIt = i;
                break;
            }
        }
        if (foundIt > -1) {
            theList.setSelectedIndex(foundIt);
        }


        p.add(s, new CC().growX().growY()); // Fill with split pane

        // The OK button...we allow GUIPlugInPanels to hook into this
        myOKButton = new JButton("OK");
        buttonPanel.add(myOKButton, new CC());

        // The cancel button
        //myCancelButton = new JButton("Cancel");
        //buttonPanel.add(myCancelButton);
        p.add(buttonPanel, new CC().dockSouth());

        content.setLayout(new MigLayout(new LC().fill().insetsAll("10"), null, null));
        content.add(myPanel, new CC().growX().growY());
        pack();
        setLocationRelativeTo(this);

        mySymbolButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SymbolDialog myDialog = new SymbolDialog(myProduct, SymbologyDialog.this, SymbologyDialog.this, true, "Symbology");
            }
        });
        // myCancelButton.addActionListener(new java.awt.event.ActionListener() {
        //     @Override
        //     public void actionPerformed(java.awt.event.ActionEvent evt) {
        //         dispose();
        //     }
        // });
        myOKButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dispose();
            }
        });
        ListSelectionModel listSelectionModel = theList.getSelectionModel();
        listSelectionModel.addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        boolean adjust = e.getValueIsAdjusting();
                        if (!adjust) {
                            ListModel model = theList.getModel();
                            if (!theList.isSelectionEmpty()) {
                                String name = (String) model.getElementAt(theList.getSelectedIndex());

                                // log.debug("Selection item " + firstIndex);
                                SymbologyGUI newControls = SymbologyFactory.getSymbologyByName(name);
                                SwingGUIPlugInPanel.install(myGUIHolder, newControls);
                            }
                        }
                    }
                });
        setLocationRelativeTo(location);

        setVisible(true);
    }
}

package org.wdssii.gui.products;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.products.renderers.DataTableRenderer;
import org.wdssii.gui.renderers.SymbolFactory;
import org.wdssii.gui.renderers.SymbolGUI;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Dialog for editing a single symbol
 *
 * @author Robert Toomey
 */
public class SymbolDialog extends JDialog {

    private JPanel myPanel = null;
    private JButton myOKButton;
    private JPanel myGUIHolder;
    private SymbolGUI myCurrentGUI = null;

    // Because Java is brain-dead with JDialog/JFrame silliness
    public SymbolDialog(Product prod, JFrame owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(prod, location, myMessage);
    }

    public SymbolDialog(Product prod, JDialog owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(prod, location, myMessage);
    }

    private void init(Product prod, Component location, String myMessage) {

        setTitle("Edit Symbol");
        Container content = getContentPane();
        JPanel p;
        myPanel = p = new JPanel();
        p.setLayout(new MigLayout("",
                "[pref!][grow, fill]",
                "[][]"));

        // FIXME: All the symbology layer stuff from product...
        ArrayList<String> list = SymbolFactory.getSymbolNameList();
        JComboBox typeList = new JComboBox(list.toArray());
        p.add(new JLabel("Symbol Type:"), new CC());
        p.add(typeList, new CC().growX().wrap());

        // The extra information panel...
        myGUIHolder = new JPanel();
        p.add(myGUIHolder, new CC().growX().growY().pushY().span().wrap());

        // Soon to be from the product symbology data
        Symbol theSymbol = DataTableRenderer.getHackMe();

        SymbolGUI first = SymbolFactory.getSymbolGUI(theSymbol);
        myCurrentGUI = first;
        String symbolName = SymbolFactory.getSymbolTypeString(theSymbol);

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(symbolName)) {
                typeList.setSelectedIndex(i);
                break;
            }
        }
        first.activateGUI(myGUIHolder);

        JPanel buttonPanel = new JPanel();

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

        /**
         * Add listener for changing symbol type
         */
        typeList.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JComboBox comboBox = (JComboBox) evt.getSource();
                Object selected = comboBox.getSelectedItem();
                String text = selected.toString();
                Symbol oldOne = null;
                if (myCurrentGUI != null) {
                    oldOne = myCurrentGUI.getSymbol();
                }
                Symbol newOne = SymbolFactory.getSymbolByName(text, oldOne);
                if (newOne != null) {
                    SymbolGUI gui = SymbolFactory.getSymbolGUI(newOne);

                    if (gui != null) {
                        myGUIHolder.removeAll();
                        gui.activateGUI(myGUIHolder);
                        myGUIHolder.validate();
                        myGUIHolder.repaint();
                        myCurrentGUI = gui;
                        // Replace hacked symbol
                        DataTableRenderer.setHackMe(newOne);
                        AnimateManager.updateDuringRender();
                    }
                }

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

        setLocationRelativeTo(location);
        setVisible(true);
    }
}

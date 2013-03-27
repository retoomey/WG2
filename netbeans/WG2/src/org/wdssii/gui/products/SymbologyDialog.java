package org.wdssii.gui.products;

import java.awt.Container;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.AnimateManager;
import org.wdssii.gui.products.renderers.DataTableRenderer;
import org.wdssii.gui.renderers.StarSymbolGUI;
import org.wdssii.gui.renderers.SymbolFactory;
import org.wdssii.gui.renderers.SymbolGUI;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Dialog for changing symbology of our products Not sure where this belongs.
 *
 * (alpha)
 *
 * @author Robert Toomey
 */
public class SymbologyDialog extends JDialog {

    private JPanel myPanel = null;
    private JTextField myURLTextField;
    private JTextField myNameTextField;
    private JLabel myTypeTextField;
    private JPanel mySubPanel;
    private JButton myValidateURLButton;
    private JButton myCancelButton;
    private JButton myOKButton;
    private JPanel myGUIHolder;

    // Because Java is brain-dead with JDialog/JFrame silliness
    public SymbologyDialog(Product prod, JFrame owner, JComponent location, boolean modal, String myMessage) {

        super(owner, modal);
        init(prod, location, myMessage);
    }

    public SymbologyDialog(Product prod, JDialog owner, JComponent location, boolean modal, String myMessage) {

        super(owner, modal);
        init(prod, location, myMessage);
    }

    private void init(Product prod, JComponent location, String myMessage) {
        // Currently only two...Datatypes that are not DataTables are all
        // drawn with color map file...or number to bin lookup...
        // Datatables can do this:
        // number (attribute) --> bin lookup (colormap)
        // string (attribute) --> colordef lookup (colordatabase)

        setTitle("Experimental Symbol Edit");
        Container content = getContentPane();

        //DataType d = prod.getRawDataType();
        //String type = "unknown";
        //if (d != null) {
        //    type = d.getTypeName();
        //}
        JPanel p;
        myPanel = p = new JPanel();
        p.setLayout(new MigLayout("",
                "[pref!][grow, fill]",
                "[][]"));

        // String colorkey = prod.getColorKey();
        // p.add(new JLabel("Instance is " + d.getClass().getSimpleName()));
        // p.add(new JLabel("DataType is " + type));
        // p.add(new JLabel("Colorkey is " + colorkey));

        // FIXME: All the symbology layer stuff from product...
        ArrayList<String> list = SymbolFactory.getSymbolNameList();
        JComboBox typeList = new JComboBox(list.toArray());
        p.add(new JLabel("Symbol Type:"), new CC());
        p.add(typeList, new CC().growX().wrap());

        // The extra information panel...
        myGUIHolder = new JPanel();
        //myGUIHolder.setSize(200, 50);
        p.add(myGUIHolder, new CC().growX().growY().pushY().span().wrap());

        // Soon to be from the product symbology data
        Symbol theSymbol = DataTableRenderer.getHackMe();

        SymbolGUI first = SymbolFactory.getSymbolGUI(theSymbol);
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

        content.add(myPanel);
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
                Symbol newOne = SymbolFactory.getSymbolByName(text);
                if (newOne != null) {
                    SymbolGUI gui = SymbolFactory.getSymbolGUI(newOne);
                    if (gui != null) {
                        myGUIHolder.removeAll();
                        gui.activateGUI(myGUIHolder);
                        myGUIHolder.validate();
                        myGUIHolder.repaint();
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

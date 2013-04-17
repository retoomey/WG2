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
import org.wdssii.xml.iconSetConfig.StarSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Dialog for editing a single symbol
 * FIXME: the 'meat' of this should become SymbolPanel, allowing it
 * to be placed in another container....
 * 
 * @author Robert Toomey
 */
public class SymbolDialog extends JDialog {

    private JPanel myPanel = null;
    private JButton myOKButton;
    private JPanel myGUIHolder;
    private SymbolGUI myCurrentGUI = null;
    private SymbologyDialog myCallback = null;
    
    // Because Java is brain-dead with JDialog/JFrame silliness
    public SymbolDialog(SymbologyDialog callback, Product prod, JFrame owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(callback, prod, location, myMessage);
    }

    public SymbolDialog(SymbologyDialog callback, Product prod, JDialog owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(callback, prod, location, myMessage);
    }

    private void init(SymbologyDialog callback, Product prod, Component location, String myMessage) {

        myCallback = callback;
        setTitle("Edit Symbol");
        //Container content = getContentPane();

        SymbolPanel p = new SymbolPanel(new StarSymbol(), null, null);
       // p.addListener(this);
        
        JPanel buttonPanel = new JPanel();

        // The OK button...we allow GUIPlugInPanels to hook into this
        myOKButton = new JButton("OK");
        buttonPanel.add(myOKButton, new CC());

        // The cancel button
        //myCancelButton = new JButton("Cancel");
        //buttonPanel.add(myCancelButton);
     

       // content.setLayout(new MigLayout(new LC().fill().insetsAll("10"), null, null));
       // content.add(p, new CC().growX().growY());
       // content.add(buttonPanel, new CC().dockSouth());
        pack();

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

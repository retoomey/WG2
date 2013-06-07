package org.wdssii.gui.products;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.products.SymbolPanel.SymbolPanelListener;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Dialog for editing a single symbol FIXME: the 'meat' of this should become
 * SymbolPanel, allowing it to be placed in another container....
 *
 * @author Robert Toomey
 */
public class SymbolDialog extends JDialog implements SymbolPanelListener {

    private final static Logger LOG = LoggerFactory.getLogger(SymbolDialog.class);
    private JButton myOKButton;
    private JButton myCancelButton;
    private Symbol myOrgSymbol;
    private Symbol myWorkingSymbol;
    private SymbolPanel mySymbolPanel;
    private SymbolPanelListener myListener;

    // Because Java is brain-dead with JDialog/JFrame silliness
    public SymbolDialog(SymbolPanelListener l, Symbol aSymbol, JFrame owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(l, aSymbol, location, myMessage);
    }

    public SymbolDialog(SymbolPanelListener l, Symbol aSymbol, JDialog owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(l, aSymbol, location, myMessage);
    }

    private void init(SymbolPanelListener l, Symbol aSymbol, Component location, String myMessage) {

        myListener = l;
        // This is the original direct symbol.  We want to keep this for cancel
        myOrgSymbol = aSymbol;
        LOG.debug("Original symbol in is " + myOrgSymbol);
        // This is a copy to be live changed by symbol panel.  Even that could
        // change if the 'type' is changed by user
        myWorkingSymbol = aSymbol.copy();

        setTitle("Edit Symbol");
        Container content = getContentPane();
        content.setLayout(new MigLayout("insets 0",
                "[grow, fill]",
                "[pref!][grow, fill]"));

        JPanel typeArea = new JPanel();
        content.add(typeArea, new CC().growX().wrap());

        JPanel symbolHolder = new JPanel();
        content.add(symbolHolder, new CC().growX().growY());

        SymbolPanel p = new SymbolPanel(myWorkingSymbol, typeArea, symbolHolder);
        p.addListener(this);
        mySymbolPanel = p;
        // p.addListener(this);

        // The OK button...we allow GUIPlugInPanels to hook into this
        
        JPanel buttonPanel = new JPanel();
        JButton myOKButton = new JButton("OK");
        buttonPanel.add(myOKButton, new CC().tag("ok"));
        myOKButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dispose();
            }
        });

        // The cancel button
        myCancelButton = new JButton("Cancel");
        buttonPanel.add(myCancelButton, new CC().tag("cancel"));
        myCancelButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                myWorkingSymbol = myOrgSymbol;
                dispose();
            }
        });

        content.add(buttonPanel, new CC().dockSouth());
        pack();

        setLocationRelativeTo(location);
        // Locks thread we want the object before we show it
        // setVisible(true);
    }

    /**
     * Return the working symbol, the one edited
     */
    public Symbol getFinalSymbol() {
        return myWorkingSymbol;
    }

    @Override
    public void symbolChanged(Symbol s) {
        myWorkingSymbol = mySymbolPanel.getSymbol();
        if (myListener != null) {
            myListener.symbolChanged(s);
        }
    }
}

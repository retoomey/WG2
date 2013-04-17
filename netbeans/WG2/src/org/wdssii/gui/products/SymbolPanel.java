package org.wdssii.gui.products;

import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.renderers.SymbolFactory;
import org.wdssii.gui.renderers.SymbolGUI;
import org.wdssii.gui.renderers.SymbolGUI.SymbolGUIListener;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * A panel provider for editing a single symbol
 *
 * This contains a drop down menu for choosing a SymbolGUI, these are
 * swapped out as type changes.
 * 
 * This isn't a JPanel itself since it contains two areas...
 *
 * @author Robert Toomey
 */
public class SymbolPanel implements SymbolGUIListener {

    private JPanel myGUIHolder;
    private SymbolGUI myCurrentGUI = null;
    private SymbolPanelListener myListener = null;
    private Symbol mySymbol;
    private JPanel myTypeArea = null;

    @Override
    public void symbolChanged() {
        if (myListener != null) {
            myListener.symbolChanged(mySymbol);
        }
    }

    public static interface SymbolPanelListener {

        public void symbolChanged(Symbol s);
    }

    public SymbolPanel(Symbol first, JPanel typeArea, JPanel contentArea) {
        mySymbol = first;
        myTypeArea = typeArea;
        myGUIHolder = contentArea;
        setupComponents();
    }

    public void addListener(SymbolPanelListener l) {
        myListener = l;
    }

    private void setupComponents() {
        
        // The type chooser bar gets it's own area...
        ArrayList<String> list = SymbolFactory.getSymbolNameList();
        JComboBox typeList = new JComboBox(list.toArray());
        if (myTypeArea != null) {
            // Use provided space for our type list....
            myTypeArea.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
            myTypeArea.add(new JLabel("Symbol Type:"), new CC());
            myTypeArea.add(typeList, new CC().growX().wrap());
        }
        
        // The panel for symbols to use...
        SymbolGUI first = SymbolFactory.getSymbolGUI(mySymbol);
        first.addListener(this);
        myCurrentGUI = first;
        String symbolName = SymbolFactory.getSymbolTypeString(mySymbol);

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(symbolName)) {
                typeList.setSelectedIndex(i);
                break;
            }
        }
        first.activateGUI(myGUIHolder);

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
                        myCurrentGUI.addListener(SymbolPanel.this);
                        mySymbol = newOne;
                        if (myListener != null) {
                            myListener.symbolChanged(mySymbol);
                        }
                    }
                }

            }
        });
    }
}

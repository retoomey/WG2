package org.wdssii.gui.products;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Dialog for changing symbology of our products.
 * @author Robert Toomey
 */
public class SymbologyDialog extends JDialog {

    private final static Logger LOG = LoggerFactory.getLogger(SymbologyDialog.class);
    private JPanel myPanel = null;
    private JButton myOKButton;

    // Because Java is brain-dead with JDialog/JFrame silliness
    public SymbologyDialog(ProductFeature prod, JFrame owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(prod, location, myMessage);
    }

    public SymbologyDialog(ProductFeature prod, JDialog owner, Component location, boolean modal, String myMessage) {

        super(owner, modal);
        init(prod, location, myMessage);
    }

    private void init(ProductFeature prod, Component location, String myMessage) {

        setTitle("Symbology");
        Container content = getContentPane();

        // Symbology panel
        JPanel p;
        myPanel = p = new SymbologyJPanel(prod,  location, myMessage);

        // Button panel
        JPanel buttonPanel = new JPanel();
        myOKButton = new JButton("OK");
        buttonPanel.add(myOKButton, new CC());
        
        content.setLayout(new MigLayout(new LC().fill().insetsAll("10"), null, null));
        content.add(myPanel, new CC().growX().growY());
        content.add(buttonPanel, new CC().dockSouth());
        pack();

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

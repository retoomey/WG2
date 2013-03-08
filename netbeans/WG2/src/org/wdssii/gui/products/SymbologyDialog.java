package org.wdssii.gui.products;

import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.datatypes.DataType;

/**
 * Dialog for changing symbology of our products
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

        setTitle("Symbology");
        Container content = getContentPane();
        DataType d = prod.getRawDataType();
        String type = "unknown";
        if (d != null) {
            type = d.getTypeName();
        }
        JPanel p;
        myPanel = p = new JPanel();
        p.setLayout(new MigLayout("",
                "[pref!][grow, fill]",
                "[][]"));

        String colorkey = prod.getColorKey();
        p.add(new JLabel("DataType is " + type));
        p.add(new JLabel("Colorkey is " + colorkey));


        // The extra information panel...
        myGUIHolder = new JPanel();
        myGUIHolder.setSize(200, 50);
        p.add(myGUIHolder, new CC().growX().span().wrap());

        // The OK button...we allow GUIPlugInPanels to hook into this
        myOKButton = new JButton("OK");
        p.add(myOKButton, new CC().skip(1));

        // The cancel button
        myCancelButton = new JButton("Cancel");
        p.add(myCancelButton);

        content.add(myPanel);
        pack();

        myCancelButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dispose();
            }
        });
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

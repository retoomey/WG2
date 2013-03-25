package org.wdssii.gui.properties;

import com.jidesoft.swing.JideMenu;
import com.jidesoft.swing.JideSplitButton;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;

/**
 * Combo-box for choosing among a list of String.
 *
 * FIXME: Would be better with a JComboBox, but dynamic lists in there
 * are annoying.  Maybe create a custom model figure out when to regenerate.
 * 
 * Requires key for selected item FIXME: generalize for other types?
 *
 * @author Robert Toomey
 */
public class ComboStringGUI extends PropertyGUI {

    private ArrayListProvider myArrayListProvider;

    public abstract static class ArrayListProvider {

        public abstract ArrayList<String> getList();
    }

    public ComboStringGUI(Mementor f, String property, String plabel, JComponent dialogRoot,
            ArrayListProvider a) {
        super(f, property);

        myArrayListProvider = a;

        // Use a split button as a dynamic combobox...
        final JideSplitButton button = new JideSplitButton("");
        //button.setToolTipText("Set the chart that draws the 3D part of this");
        button.setButtonStyle(JideSplitButton.TOOLBOX_STYLE);
        button.setAlwaysDropdown(true);
        final Mementor myF = f;
        final String myP = property;
        button.setPopupMenuCustomizer(new JideMenu.PopupMenuCustomizer() {
            @Override
            public void customize(JPopupMenu menu) {
                menu.removeAll();
                ArrayList<String> list = myArrayListProvider.getList();
                for (String c : list) {
                    JMenuItem item = new JMenuItem(c);
                    item.addActionListener(new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                            JMenuItem item = (JMenuItem) evt.getSource();
                            String setting = item.getText();
                            Memento m = myF.getNewMemento();
                            m.setProperty(myP, setting);
                            myF.propertySetByGUI(myP, m);
                           // button.setText(setting);  set in update
                        }
                    });
                    menu.add(item);
                }
            }
        });

        setTriple(new JLabel(plabel), button, new JLabel(""));
    }

    @Override
    public void update(Memento use) {
        //value.setBackground((Color) use.getPropertyValue(property));
        JideSplitButton button = (JideSplitButton) (value);
        button.setText((String) use.getPropertyValue(property));
    }
}

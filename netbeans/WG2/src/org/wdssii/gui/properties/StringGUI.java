package org.wdssii.gui.properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;

public class StringGUI extends PropertyGUI {

    private JTextField myTextField;
    private JButton myButton;
    
    public StringGUI(Mementor f, Object property, String plabel, JComponent dialogRoot) {
        super(f, property);
        
        // Humm is this ok?
        final JComponent myRoot = dialogRoot;
        final Mementor myF = f;
        final Object myP = property;
        
        // Create gui objects
        JTextField b = new JTextField(50);
        JButton bt = new JButton("Enter");
        myTextField = b;
        myButton = bt;
        
        b.setText(f.getMemento().get(property, "?"));       
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                jTextFieldUpdated(StringGUI.this, myButton, myTextField, myRoot, myF, myP, ae, false);
            }
        });
        
        // Button for enter
        bt.setToolTipText("Click me or hit enter in the text box to update");
        bt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                jTextFieldUpdated(StringGUI.this, myButton, myTextField, myRoot, myF, myP, ae, true);
            }
        });

        setTriple(new JLabel(plabel), b, bt);
    }

    @Override
    public void update(Memento use) {
    	JTextField v = (JTextField) (value);
    	v.setText(use.get(property, "?"));
    }

    /**
     * Handle a color button change by changing its property value to the new
     * color
     */
    private static void jTextFieldUpdated(StringGUI source, JButton b1, JTextField b2, JComponent root, Mementor f, Object property, ActionEvent evt, boolean button) {


        if (!button){
        	b1.doClick();
        	//jTextFieldUpdated(source, b1, b2, root, f, property, evt, false);
        }else{
           // JComponent j = (JComponent) evt.getSource();
           // if (j instanceof JTextField) {
            	//JTextField b = (JTextField) (j);
            	String text = b2.getText();
               // Memento m = f.getNewMemento();
            	Memento m = f.getUpdateMemento(); // blank memento...
                m.setProperty(property, text);
                m.setEventSource(source);
                            
                f.propertySetByGUI(property, m);
            //}
        }
    }
}
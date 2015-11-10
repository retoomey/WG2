package org.wdssii.gui.properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;

public class DirectoryGUI extends PropertyGUI {
	private final static Logger LOG = LoggerFactory.getLogger(DirectoryGUI.class);

	private JTextField myTextField;
	
	private Object myFileProperty;
	private String myLastDirectory = "";
	
	public DirectoryGUI(Mementor f, Object property, Object fileProperty,
			String plabel, String bLabel, String title, JComponent dialogRoot) {
		super(f, property);

		// Create textbox/ label or what...
		JTextField b = new JTextField(50);
		b.setText(f.getMemento().get(property, ""));
		b.setEditable(false);
		b.setAutoscrolls(false);
		myTextField = b;
		
		myFileProperty = fileProperty;
		
		// Humm is this ok?
		final JComponent myRoot = dialogRoot;
		final Mementor myF = f;
		final Object myP = property;

		final JButton d = new JButton(bLabel);
		final String myTitle = title;

		// Dialog 
		d.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				jChooseDirectory(DirectoryGUI.this, d, myRoot, myTitle, myF, myP, ae);
			}
		});

		setTriple(new JLabel(plabel), b, d);
	}

	@Override
	public void update(Memento use) {
		String dir = "";
		String file = "";
		
		dir = use.get(property, dir);
		file = use.get(myFileProperty, file);
		setText(dir, file);
	}
	
	public void setText(String dir, String file) {	
		myLastDirectory = dir;
		myTextField.setText(file+" @ ["+dir+"]");
	}
	
	
	public String getLastDirectory(){
		return myLastDirectory;
	}

	/**
	 * Handle a color button change by changing its property value to the new
	 * color
	 */
	private static void jChooseDirectory(DirectoryGUI source, JButton b, JComponent root, String title, Mementor f,
			Object property, ActionEvent evt) {

		JComponent j = (JComponent) evt.getSource();

		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(title);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setAcceptAllFileFilterUsed(false);
		fc.setCurrentDirectory(new File(source.getLastDirectory()));
		if (fc.showSaveDialog(root) == JFileChooser.APPROVE_OPTION){
			String dir = fc.getSelectedFile().toString();
			//Memento m = f.getNewMemento();
            Memento m = f.getUpdateMemento(); // blank memento
			m.setProperty(property, dir);
			 m.setEventSource(source);
			f.propertySetByGUI(property, m);
		}
	}
}
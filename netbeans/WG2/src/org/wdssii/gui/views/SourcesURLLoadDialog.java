package org.wdssii.gui.views;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.GUIPlugInPanel;
import org.wdssii.gui.sources.Source;
import org.wdssii.gui.sources.SourceFactory;
import org.wdssii.gui.sources.SourceList;

/**
 * The dialog for loading an individual source.
 *
 * @author Robert Toomey
 */
public class SourcesURLLoadDialog extends JDialog implements ActionListener {

	private JPanel myPanel = null;
	private JTextField myURLTextField;
	private JTextField myNameTextField;
	private JLabel myTypeTextField;
	private JPanel mySubPanel;
	private JButton myValidateURLButton;
	private JButton myCancelButton;
	private JButton myOKButton;
	private JPanel myGUIHolder;
	private SourceFactory myFactory;
	private GUIPlugInPanel myExtrasPanel;

	/**
	 * Get a default name of the form 'Source#', making sure that name isn't
	 * already being used
	 *
	 * @return a default source name
	 */
	private String getDefaultName() {
		int counter = 0;
		boolean done = false;
		List<Source> list = SourceList.theSources.getSources();
		String candidateName = "Source";
		while (!done) {
			candidateName = "Source" + (++counter);
			// See if candidate matches something there....
			boolean alreadyHaveThatName = false;
			for (Source S : list) {
				if (S.getVisibleName().equals(candidateName)) {
					counter++;
					alreadyHaveThatName = true;
					break;
				}
			}
			if (!alreadyHaveThatName) {
				done = true;
			}
		}
		return candidateName;
	}

	public SourcesURLLoadDialog(JFrame frame, boolean modal, String myMessage) {
		super(frame, modal);

		setTitle("Open Source by URL Location");
		Container content = getContentPane();

		JPanel p;
		myPanel = p = new JPanel();
	//	p.setLayout(new MigLayout("fillx, wrap 3", "[pref!][pref!][pref!]", ""));
		p.setLayout(new MigLayout("", 
			"[pref!][grow, fill]",
		        "[][]"));

		// Source name
		p.add(new JLabel("Name:"));
		myNameTextField = new javax.swing.JTextField();
		myNameTextField.setText(getDefaultName());
		p.add(myNameTextField, new CC().growX().span());
		
		// The URL field...
		p.add(new JLabel("URL:"));
		myURLTextField = new JTextField();
		p.add(myURLTextField, new CC().growX());

		// Validate button...
		myValidateURLButton = new JButton("Validate");
		p.add(myValidateURLButton, new CC().alignX("right").wrap());

		// Where
		p.add(new JLabel("From:"));
		// The browse local file button...
		JButton b = new JButton("Local file...");
		p.add(b, new CC().wrap());
		//JButton b2 = new JButton("Bookmark...");
		//p.add(b2, new CC().wrap());


		// The type information....
		p.add(new JLabel("Type:"));
		myTypeTextField = new JLabel("Unknown");
		p.add(myTypeTextField, new CC().growX().wrap());

		// The extra information panel...
		myGUIHolder = new JPanel();
		myGUIHolder.setSize(200, 50);
		setUpBadSource("No URL");
		p.add(myGUIHolder, new CC().growX().span().wrap());

		// The OK button...we allow GUIPlugInPanels to hook into this
		myOKButton = new JButton("OK");
		p.add(myOKButton, new CC().skip(1));

		// The cancel button
		myCancelButton = new JButton("Cancel");
		p.add(myCancelButton);

		content.add(myPanel);
		pack();
		setLocationRelativeTo(frame);

		myURLTextField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent e) {
				invalidate();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				invalidate();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				invalidate();
			}

			public void invalidate() {
				validateURLString(null);
			}
		});


		b.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				URL aURL = doSourceOpenDialog();
				if (aURL != null) {
					myURLTextField.setText(aURL.toString());
				}
				validateURL(aURL);
			}
		});

		myCancelButton.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				dispose();
			}
		});

		myValidateURLButton.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {

				validateURLString(myURLTextField.getText());
			}
		});

		setVisible(true);
	}

	/**
	 * Get the current URL showing in the dialog
	 */
	public URL getURL() {
		URL aURL;
		try {
			String u = myURLTextField.getText();
			aURL = new URL(u);
		} catch (Exception ex) {
			aURL = null;
		}
		return aURL;
	}

	/**
	 * Get the current URL showing in the dialog
	 */
	public String getSourceName() {
		String n = myNameTextField.getText();
		return n;
	}

	/**
	 * Get the OK button.  Used by GUI panels to add/remove listener
	 */
	public JButton getOKButton() {
		return myOKButton;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
	}

	public void validateURLString(String urlText) {
		URL aURL;
		try {
			aURL = new URL(urlText);
		} catch (MalformedURLException ex) {
			aURL = null;
		}
		validateURL(aURL);
	}

	public void validateURL(URL aURL) {
		boolean showButton;
		myFactory = null;
		if (aURL == null) {
			setUpBadSource("Bad URL");
			showButton = true;
		} else {
			// Try to find a factory for this URL
			SourceFactory factory = SourceFactory.getFactoryForURL(aURL);
			if (factory != null) {
				clearParamPanel();
				GUIPlugInPanel p = factory.createParamsGUI(this);
				p.activateGUI(myGUIHolder);
				myExtrasPanel = p;
				myTypeTextField.setText(factory.getDialogDescription());
				showButton = false;
				myFactory = factory;
				pack();
			} else {
				// valid URL but can't handle it....
				setUpBadSource("Unrecognized data format at URL");
				showButton = true;
			}
		}

		// If valid, hide the button...
		myValidateURLButton.setVisible(showButton);
	}

	;

	private void clearParamPanel() {
		if (myExtrasPanel != null) {
			myExtrasPanel.deactivateGUI(myGUIHolder);
			myExtrasPanel = null;
		}
		myGUIHolder.removeAll();
		myGUIHolder.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
	}

	private void setUpBadSource(String message) {
		clearParamPanel();

		JPanel myStuff = new JPanel();
		myStuff.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
		myStuff.add(new JLabel("invalid URL"), new CC().growX());
		myGUIHolder.add(myStuff, new CC().growX().growY());

		myTypeTextField.setText("Unknown, click Validate to check");
	}

	// Source type.
	/**
	 * Filter for local files.
	 */
	private static class SourceFileFilter extends FileFilter {

		@Override
		public boolean accept(File f) {
			String t = f.getName().toLowerCase();
			// FIXME: need to get these from the Sources, some sort of
			// Factory
			boolean canBeHandled = SourceFactory.canAnyHandleFileType(f);
			return (f.isDirectory() || canBeHandled);

//							|| t.endsWith(".netcdf") || t.endsWith(".nc") || t.endsWith(".netcdf.gz")
//							|| t.endsWith(".xml") || t.endsWith(".xml.gz"));
		}

		@Override
		public String getDescription() {
			Set<String> types = SourceFactory.getAllHandledFileDescriptions();
			String d = "";
			for (String s : types) {
				d += s;
				d += " ";
			}
			d += "Files";
			return d;
		}
	}

	void setUpGUIForURL(JComponent holder, URL aURL) {
	}

	/**
	 * For the moment a simple file dialog
	 */
	public URL doSourceOpenDialog() {

		URL pickedFile = null;
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new SourceFileFilter());
		chooser.setDialogTitle("Open local file or FAM directory");
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File f = chooser.getSelectedFile();
			try {
				pickedFile = f.toURI().toURL();
			} catch (MalformedURLException ex) {
				pickedFile = null;
			}

		}

		return pickedFile;
	}

	/**
	 * Sent by GUIPlugInPanel when it's handled the user clicking OK,
	 * usually by creating/loading the source
	 */
	public void handledOKButton() {
		dispose();
	}
}

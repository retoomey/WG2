package org.wdssii.gui.sources;

import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.*;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.GUIPlugInPanel;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.commands.SourceAddCommand.SourceAddParams;
import org.wdssii.gui.views.SourcesURLLoadDialog;

/**
 * Create the params for an IndexSource. We know it's an index URL, but we need
 * extra information such as 'History' to load it. This plugs into the add
 * source dialog
 *
 * @author Robert Toomey
 */
public class IndexSourceParamsGUI extends JPanel implements GUIPlugInPanel {

	private JComboBox myHistoryComboBox;
	private SourcesURLLoadDialog myOwner;
	private ActionListener myOKListener;

	public IndexSourceParamsGUI(SourcesURLLoadDialog d) {
		myOwner = d;
	}

	@Override
	public void updateGUI() {
	}

	@Override
	public void activateGUI(JComponent parent) {
		parent.setLayout(new MigLayout(new LC().fill().insetsAll("0"), null, null));
		parent.add(this, new CC().growX().growY());

		setLayout(new MigLayout("fillx, filly, wrap 3", "[pref!][][pref!]", ""));
		add(new JLabel("History:"));
		myHistoryComboBox = new javax.swing.JComboBox();
		add(myHistoryComboBox, new CC().growX().spanX(2).wrap());
		myHistoryComboBox.setModel(new DefaultComboBoxModel(new String[]{"All", "30", "60", "90", "120"}));
		myHistoryComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				// Nada yet
			}
		});

		JButton ok = myOwner.getOKButton();
		myOKListener = new ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				handleOK();
			}
		};
		ok.addActionListener(myOKListener);
	}

	@Override
	public void deactivateGUI(JComponent parent) {
		parent.remove(this);
		if (myOKListener != null) {
			JButton ok = myOwner.getOKButton();
			ok.removeActionListener(myOKListener);
		}
	}

	/**
	 * Handle ok button picked, so we load a new IndexSource
	 */
	private void handleOK() {

		// Do the work of creating a new IndexSource....
		URL url = myOwner.getURL();
		String sourceName = myOwner.getSourceName();

		SourceAddParams p = new SourceAddParams();
		p.connect = true;
		p.niceName = sourceName;
		p.realTime = false; // ???? need from type
		p.rootWindow = null;
		p.sourceURL = url;

		myOwner.handledOKButton();

		SourceAddCommand add = new SourceAddCommand(p);
		CommandManager.getInstance().executeCommand(add, true);
		
	}
}

package org.wdssii.gui.sources;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.*;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.CommandManager;
import org.wdssii.gui.GUIPlugInPanel;
import org.wdssii.gui.commands.SourceAddCommand;
import org.wdssii.gui.commands.SourceAddCommand.IndexSourceAddParams;
import org.wdssii.gui.views.SourcesURLLoadDialog;
import org.wdssii.index.HistoricalIndex;

/**
 * Create the params for an IndexSource. We know it's an index URL, but we need
 * extra information such as 'History' to load it. This plugs into the add
 * source dialog
 *
 * @author Robert Toomey
 */
public class IndexSourceParamsGUI extends JPanel implements GUIPlugInPanel {

	private final static Logger LOG = LoggerFactory.getLogger(IndexSourceParamsGUI.class);
	private JComboBox myHistoryComboBox;
	private SourcesURLLoadDialog myOwner;
	private ActionListener myOKListener;
	private int myHistoryValue = 1000;
	private historyControl myHistoryControl;

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

		ActionListener l = new ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (myHistoryComboBox != null) {
					myHistoryValue = myHistoryControl.getHistory();
				}
			}
		};

		historyControl h = createHistoryControl(l);
		myHistoryControl = h;
		setLayout(new MigLayout("fillx, filly, wrap 3", "[pref!][][pref!]", ""));
		add(h.getLabel());
		myHistoryComboBox = h.getDropDown();
		myHistoryValue = h.getHistory();
		add(myHistoryComboBox, new CC().growX().spanX(2).wrap());

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
	public void deactivateGUI() {
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

                // FIXME: Realtime?
                IndexSourceAddParams p = new IndexSourceAddParams(sourceName, url, false, true, myHistoryValue);		
		p.rootWindow = null;		
		myOwner.handledOKButton();

		SourceAddCommand add = new SourceAddCommand(p);
		CommandManager.getInstance().executeCommand(add, true);

	}

	/**
	 * FIXME: merge into property library
	 */
	public static class historyControl {

		private JLabel label = new JLabel("History");
		private JComboBox box = new javax.swing.JComboBox();
		private int history = 1000; // match first box item
		private final String[] listNames = new String[]{
			"Realtime: Latest 1000 records",
			"Realtime: Latest 5000 records",
			"Realtime: Latest 10000 records",
			"Archive: All Records"};
		private final int[] historyValues = new int[]{
			1000,
			5000,
			10000,
			HistoricalIndex.HISTORY_ARCHIVE};

		public historyControl(ActionListener notify) {
			final ActionListener secondNotify = notify;

			box.setModel(new DefaultComboBoxModel(listNames));

			box.addActionListener(new java.awt.event.ActionListener() {

				@Override
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					historyActionPerformed(evt);
					// Piggyback since Java ordering of firing is internal to it
					if (secondNotify != null) {
						secondNotify.actionPerformed(evt);
					}
				}
			});
		}

		private void historyActionPerformed(java.awt.event.ActionEvent e) {
			JComboBox cb = (JComboBox) e.getSource();
			int item = cb.getSelectedIndex();
			if (item < historyValues.length) {
				history = historyValues[item];
			} else {
				LOG.error("Have no history number value for " + cb.getSelectedItem());
			}
		}

		public Component getLabel() {
			return label;
		}

		public JComboBox getDropDown() {
			return box;
		}

		public int getHistory() {
			return history;
		}
	}

	/**
	 * Return a history control for an IndexSource
	 */
	public static historyControl createHistoryControl(ActionListener notify) {
		historyControl h = new historyControl(notify);
		return h;
	}
}

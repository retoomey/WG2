package org.wdssii.gui.symbology;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.wdssii.gui.properties.BooleanGUI;
import org.wdssii.gui.properties.ColorGUI;
import org.wdssii.gui.properties.IntegerGUI;
import org.wdssii.gui.renderers.StarSymbolRenderer;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;
import org.wdssii.xml.iconSetConfig.StarSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

import com.jidesoft.swing.JideButton;

import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

/**
 * GUI for editing a StarSymbol FIXME: Need to work on properties/memento,
 * etc...make it simpler and better, seems to be working decently.
 *
 * @author Robert Toomey
 */
public class StarSymbolGUI extends PointSymbolGUI {

	/**
	 * The StarSymbol we are using
	 */
	private StarSymbolMementor myMementor;

	/**
	 * Holds all the flags of StarSymbol
	 */
	public static class StarSymbolMemento extends PointSymbolMemento {

		// Properties
		public static final int NUMPOINTS = PointSymbolMemento.LAST;
		public static final int LINESIZE = NUMPOINTS + 1;
		public static final int COLOR = LINESIZE + 1;
		public static final int USEOUTLINE = COLOR + 1;
		public static final int OCOLOR = USEOUTLINE + 1;
		public static final int LAST = OCOLOR + 1;

		public StarSymbolMemento(StarSymbolMemento m) {
			super(m);
		}

		public StarSymbolMemento() {
			super();
			initProperty(NUMPOINTS, 4);
			initProperty(LINESIZE, 1);
			initProperty(COLOR, Color.BLUE);
			initProperty(USEOUTLINE, true);
			initProperty(OCOLOR, Color.BLACK);
		}
	}

	/**
	 * Provides the properties for a StarSymbol
	 */
	public class StarSymbolMementor extends PointSymbolMementor {

		private StarSymbol myStarSymbol;

		public StarSymbolMementor(StarSymbol data) {
			super(data);
			myStarSymbol = data;
		}

		public void toX() {
			myStarSymbol.toX();
		}

		public void toAsterisk() {
			myStarSymbol.toAsterisk();
		}

		@Override
		public void propertySetByGUI(Object name, Memento m) {

			myStarSymbol.numpoints = m.get(StarSymbolMemento.NUMPOINTS, myStarSymbol.numpoints);
			myStarSymbol.lsize = m.get(StarSymbolMemento.LINESIZE, myStarSymbol.lsize);
			myStarSymbol.color = m.get(StarSymbolMemento.COLOR, myStarSymbol.color);
			myStarSymbol.useOutline = m.get(StarSymbolMemento.USEOUTLINE, myStarSymbol.useOutline);
			myStarSymbol.ocolor = m.get(StarSymbolMemento.OCOLOR, myStarSymbol.ocolor);

			super.propertySetByGUI(name, m);
		}

		@Override
		public Memento getNewMemento() {
			// Get brand new mementor with default settings
			StarSymbolMemento m = new StarSymbolMemento((StarSymbolMemento) getMemento());
			return m;
		}

		@Override
		public void setMemento(Memento m) {
			super.setMemento(m);
			if (m instanceof StarSymbolMemento) {
				StarSymbolMemento m2 = (StarSymbolMemento) m;
				m2.setProperty(StarSymbolMemento.NUMPOINTS, myStarSymbol.numpoints);
				m2.setProperty(StarSymbolMemento.LINESIZE, myStarSymbol.lsize);
				m2.setProperty(StarSymbolMemento.COLOR, myStarSymbol.color);
				m2.setProperty(StarSymbolMemento.USEOUTLINE, myStarSymbol.useOutline);
				m2.setProperty(StarSymbolMemento.OCOLOR, myStarSymbol.ocolor);
			}
		}

		@Override
		public Memento getMemento() {
			// Get the current settings...patch from StarSymbol...
			StarSymbolMemento m = new StarSymbolMemento();
			setMemento(m);
			return m;
		}
	}

	/**
	 * Creates new LegendGUI
	 */
	public StarSymbolGUI(StarSymbol owner) {
		myMementor = new StarSymbolMementor(owner);
		setupComponents();
	}

	@Override
	public Symbol getSymbol() {
		return myMementor.myStarSymbol;
	}

	/**
	 * General update call
	 */
	@Override
	public void updateGUI() {
		updateToMemento(myMementor.getNewMemento());
	}

	public StarSymbol toolbarSymbol() {
		StarSymbol p = new StarSymbol();
		p.color = Color.WHITE;
		p.ocolor = Color.RED;
		p.osize = 1;
		p.useOutline = false;
		return p;
	}

	public final void addStarSymbolComponents(Mementor m) {
		add(new IntegerGUI(myMementor, StarSymbolMemento.NUMPOINTS, "Count", this, 2, 16, 2, "points"));

		JPanel h = new JPanel();
		h.setLayout(new MigLayout(new LC().fill().insetsAll("2"), null, null));
		h.setBackground(Color.BLACK);

		// Quick selects
		JideButton b;
		StarSymbolRenderer icon = new StarSymbolRenderer();
		StarSymbol p = toolbarSymbol();
		p.toAsterisk();
		icon.setSymbol(p);
		b = new JideButton(icon);
		b.setToolTipText("Asterisk");
		b.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				myMementor.toAsterisk();
				updateGUI();
			}
		});
		h.add(b, new CC());

		icon = new StarSymbolRenderer();
		p = toolbarSymbol();
		p.toX();
		icon.setSymbol(p);
		b = new JideButton(icon);
		b.setToolTipText("Big X");
		b.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				myMementor.toX();
				updateGUI();
			}
		});
		h.add(b, new CC());

		add(h, new CC().span(3).wrap());

		add(new IntegerGUI(myMementor, StarSymbolMemento.LINESIZE, "Linewidth", this, 1, 10, 1, "points"));
		add(new ColorGUI(myMementor, StarSymbolMemento.COLOR, "Base Color", this));
		add(new BooleanGUI(myMementor, StarSymbolMemento.USEOUTLINE, "Use outline", this));
		add(new ColorGUI(myMementor, StarSymbolMemento.OCOLOR, "Outline Color", this));

		// Get the stock Symbol controls
		super.addPointSymbolComponents(myMementor);
	}

	/**
	 * Set up the components. We haven't completely automated this because you
	 * never know what little change you need that isn't supported.
	 */
	private void setupComponents() {
		JScrollPane s = new JScrollPane();
		s.setViewportView(this);
		setRootComponent(s);
		setLayout(new MigLayout(new LC(), null, null));

		addStarSymbolComponents(myMementor);
	}
}

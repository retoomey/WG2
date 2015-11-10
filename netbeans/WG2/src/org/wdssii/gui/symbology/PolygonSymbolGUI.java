package org.wdssii.gui.symbology;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.wdssii.gui.properties.BooleanGUI;
import org.wdssii.gui.properties.ColorGUI;
import org.wdssii.gui.properties.IntegerGUI;
import org.wdssii.gui.renderers.PolygonSymbolRenderer;
import org.wdssii.properties.Memento;
import org.wdssii.properties.Mementor;
import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

import com.jidesoft.swing.JideButton;

import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

/**
 * GUI for editing a PolygonSymbol
 *
 * @author Robert Toomey
 */
public class PolygonSymbolGUI extends PointSymbolGUI {

	/**
	 * The PolygonSymbol we are using
	 */
	private PolygonSymbolMementor myMementor;

	public static class PolygonSymbolMemento extends PointSymbolMemento {

		// Properties
		public static final int NUMPOINTS = PointSymbolMemento.LAST;
		public static final int COLOR = NUMPOINTS + 1;
		public static final int USEOUTLINE = COLOR + 1;
		public static final int OCOLOR = USEOUTLINE + 1;
		public static final int LAST = OCOLOR + 1;

		public PolygonSymbolMemento(PolygonSymbolMemento m) {
			super(m);
		}

		public PolygonSymbolMemento() {
			super();
			initProperty(NUMPOINTS, 4);
			initProperty(COLOR, Color.BLUE);
			initProperty(USEOUTLINE, true);
			initProperty(OCOLOR, Color.BLACK);
		}
	}

	/**
	 * Provides the properties for a StarSymbol
	 */
	private class PolygonSymbolMementor extends PointSymbolMementor {

		private PolygonSymbol mySymbol;

		public PolygonSymbolMementor(PolygonSymbol data) {
			super(data);
			mySymbol = data;
		}

		public void toSquare() {
			mySymbol.toSquare();
		}

		public void toCircle() {
			mySymbol.toCircle();
		}

		public void toDiamond() {
			mySymbol.toDiamond();
		}

		public void toTriangle() {
			mySymbol.toTriangle();
		}

		@Override
		public void propertySetByGUI(Object name, Memento m) {

			// Directly modify the StarSymbol object
			mySymbol.numpoints = m.get(PolygonSymbolMemento.NUMPOINTS, mySymbol.numpoints);
			mySymbol.color = m.get(PolygonSymbolMemento.COLOR, mySymbol.color);
			mySymbol.useOutline = m.get(PolygonSymbolMemento.USEOUTLINE, mySymbol.useOutline);
			mySymbol.ocolor = m.get(PolygonSymbolMemento.OCOLOR, mySymbol.color);

			super.propertySetByGUI(name, m);
		}

		@Override
		public Memento getNewMemento() {
			// Get brand new mementor with default settings
			PolygonSymbolMemento m = new PolygonSymbolMemento((PolygonSymbolMemento) getMemento());
			return m;
		}

		@Override
		public void setMemento(Memento m2) {
			super.setMemento(m2);
			if (m2 instanceof PolygonSymbolMemento) {
				PolygonSymbolMemento m = (PolygonSymbolMemento) (m2);
				m.setProperty(PolygonSymbolMemento.NUMPOINTS, mySymbol.numpoints);
				m.setProperty(PolygonSymbolMemento.COLOR, mySymbol.color);
				m.setProperty(PolygonSymbolMemento.USEOUTLINE, mySymbol.useOutline);
				m.setProperty(PolygonSymbolMemento.OCOLOR, mySymbol.ocolor);
			}
		}

		@Override
		public Memento getMemento() {
			// Get the current settings...patch from StarSymbol...
			PolygonSymbolMemento m = new PolygonSymbolMemento();
			setMemento(m);
			return m;
		}
	}

	/**
	 * Creates new LegendGUI
	 */
	public PolygonSymbolGUI(PolygonSymbol owner) {
		myMementor = new PolygonSymbolMementor(owner);
		setupComponents();
	}

	@Override
	public Symbol getSymbol() {
		return myMementor.mySymbol;
	}

	/**
	 * General update call
	 */
	@Override
	public void updateGUI() {
		updateToMemento(myMementor.getNewMemento());
	}

	public PolygonSymbol toolbarSymbol() {
		PolygonSymbol p = new PolygonSymbol();
		p.color = Color.WHITE;
		p.ocolor = Color.RED;
		p.osize = 1;
		p.useOutline = false;
		return p;
	}

	public final void addPolygonSymbolComponents(Mementor m) {
		add(new IntegerGUI(myMementor, PolygonSymbolMemento.NUMPOINTS, "Sides", this, 3, 20, 1, "points"));

		JPanel h = new JPanel();
		h.setLayout(new MigLayout(new LC().fill().insetsAll("2"), null, null));
		h.setBackground(Color.BLACK);

		// Quick selects
		JideButton b;
		PolygonSymbolRenderer icon = new PolygonSymbolRenderer();
		PolygonSymbol p = toolbarSymbol();
		p.toSquare();
		icon.setSymbol(p);
		b = new JideButton(icon);
		b.setToolTipText("Square");
		b.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				myMementor.toSquare();
				updateGUI();
			}
		});
		h.add(b, new CC());

		icon = new PolygonSymbolRenderer();
		p = toolbarSymbol();
		p.toCircle();
		icon.setSymbol(p);
		b = new JideButton(icon);
		b.setToolTipText("Circle");
		b.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				myMementor.toCircle();
				updateGUI();
			}
		});
		h.add(b, new CC());

		icon = new PolygonSymbolRenderer();
		p = toolbarSymbol();
		p.toDiamond();
		icon.setSymbol(p);
		// b = new JideButton("Diamond", icon);
		b = new JideButton(icon);
		b.setToolTipText("Diamond");
		b.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				myMementor.toDiamond();
				updateGUI();
			}
		});
		h.add(b, new CC());

		icon = new PolygonSymbolRenderer();
		p = toolbarSymbol();
		p.toTriangle();
		icon.setSymbol(p);
		b = new JideButton(icon);
		b.setToolTipText("Triangle");
		b.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				myMementor.toTriangle();
				updateGUI();
			}
		});
		h.add(b, new CC());

		add(h, new CC().span(3).wrap());

		add(new ColorGUI(myMementor, PolygonSymbolMemento.COLOR, "Base Color", this));
		add(new BooleanGUI(myMementor, PolygonSymbolMemento.USEOUTLINE, "Use outline", this));
		add(new ColorGUI(myMementor, PolygonSymbolMemento.OCOLOR, "Outline Color", this));

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

		addPolygonSymbolComponents(myMementor);
	}
}

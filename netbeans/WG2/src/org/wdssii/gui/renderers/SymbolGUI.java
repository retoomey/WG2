package org.wdssii.gui.renderers;

import org.wdssii.gui.SwingGUIPlugInPanel;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Root GUI for editing a Symbol
 *
 * @author Robert Toomey
 */
public abstract class SymbolGUI extends SwingGUIPlugInPanel {

    public abstract Symbol getSymbol();
}

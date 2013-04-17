package org.wdssii.gui.renderers;

import java.awt.Component;
import java.awt.Graphics;
import javax.media.opengl.GL;
import javax.swing.Icon;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Base class for symbol renderers.
 *
 * @author Robert Toomey
 */
public abstract class SymbolRenderer implements Icon {

    // OpenGL methods...
    public abstract void setSymbol(Symbol symbol);

    public abstract void render(GL gl);

    // Java Icon methods...
    @Override
    public abstract void paintIcon(Component c, Graphics g, int x, int y);

    @Override
    public  int getIconWidth(){ return 16; }

    @Override
    public  int getIconHeight() { return 16; }
}

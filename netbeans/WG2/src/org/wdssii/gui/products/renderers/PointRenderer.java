package org.wdssii.gui.products.renderers;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Color;
import java.util.List;
import javax.media.opengl.GL;
import org.wdssii.datatypes.DataTable.Column;
import org.wdssii.gui.GLUtil;
import org.wdssii.gui.renderers.SymbolFactory;
import org.wdssii.gui.renderers.SymbolRenderer;
import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.StarSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Renders points in lat/lon using our symbology and attribute table
 *
 * For now this will do: SingleSymbol Category:UniqueValues Might break up more
 * later
 *
 * @author Robert Toomey
 */
public class PointRenderer {

    private List<SymbolRenderer> myRenderers = null;
    private SymbolRenderer myDefault = null;
    private Column myColumn = null; // Column if category, null if single
    private final Object myRefreshLock = new Object();
    private boolean myRefresh = false;
    private Symbology mySymbology = null;
    /**
     * Toggle from single for all to categories...
     */
    private boolean tryCategories = false;

    public void refreshRenderers() {
        synchronized (myRefreshLock) {
            myRefresh = true;
        }
    }

    /**
     * Given DrawContext and collection of points and symbology, draw these
     * points
     */
    public void draw(DrawContext dc, Symbology s, List<Vec4> points, Column theColumn) {

        // This is set with a copy from GUI on change, so sync shouldn't be an issue...
        boolean changed = (s != mySymbology);

        // How do we draw without symbology or do we put in something?
        if (s == null) {
            return;
        }

        if (changed) {

            mySymbology = s;
            // Check symbology type...
            if (s.use == Symbology.SINGLE) {
                tryCategories = false;
            } else if (s.use == Symbology.CATEGORY_UNIQUE_VALUES) {
                tryCategories = true;
            } else {
                return;  // Can't handle other categories.. :(
                // FIXME: all this should really be checked already
            }

            // Create a new symbol and default symbol renderer if
            // there isn't one in the symbology....
            List<Symbol> aList = mySymbology.getSingle().symbols;
            if ((aList != null) && (!aList.isEmpty())) {
                Symbol sym = aList.get(0);
                SymbolRenderer renderer = SymbolFactory.getSymbolRenderer(sym);
                renderer.setSymbol(sym); // FIXME shouldn't be here probably
                myDefault = renderer;
            } else {
                // Stick in something...
                Symbol p;
                if (tryCategories) {
                    PolygonSymbol ps = new PolygonSymbol();
                    ps.pointsize = 12;
                    ps.toCircle();
                    p = ps;
                } else {
                    StarSymbol ss = new StarSymbol();
                    ss.toX();
                    ss.pointsize = 35;
                    p = ss;
                }
                SymbolRenderer renderer = SymbolFactory.getSymbolRenderer(p);
                renderer.setSymbol(p); // FIXME shouldn't be here probably
                myDefault = renderer;
            }
        }

        // Draw using the symbols...
        GL gl = dc.getGL();
        View v = dc.getView();
        GLUtil.pushOrtho2D(dc);
        gl.glDisable(GL.GL_DEPTH_TEST);
        int row = 0;
        for (Vec4 at3D : points) {
            // Project 3D world coordinates to 2D view (whenever eye changes)
            // Could maybe cache these...check for view changing somehow...
            // FIXME: Possible speed up here
            Vec4 at2D = v.project(at3D);

            // LOG.debug("translate "+at2D.x, ", "+at2D.y);
            gl.glTranslated(at2D.x, at2D.y, 0);
            SymbolRenderer item = getRenderer(row);
            item.render(gl);
            gl.glTranslated(-at2D.x, -at2D.y, 0);
            row++;

        }
        GLUtil.popOrtho2D(dc);
    }

    public SymbolRenderer getRenderer(int row) {
        SymbolRenderer rr = null;

        // Categories Unique Values
        if (tryCategories) {
            // Creating every single time....bleh....
            if (myColumn != null) {
                String text = myColumn.getValue(row);
                if (text.equalsIgnoreCase("ds")) {   // Snow
                    StarSymbol newOne = new StarSymbol();
                    newOne.color = Color.WHITE;
                    newOne.ocolor = Color.BLACK;
                    newOne.osize = 3;
                    rr = SymbolFactory.getSymbolRenderer(newOne);
                    rr.setSymbol(newOne);
                } else if (text.equalsIgnoreCase("ws")) {  // wet snow
                    StarSymbol newOne = new StarSymbol();
                    newOne.color = Color.BLUE;
                    newOne.ocolor = Color.BLACK;
                    newOne.osize = 3;
                    rr = SymbolFactory.getSymbolRenderer(newOne);
                    rr.setSymbol(newOne);
                    newOne.toAsterisk();
                    newOne.pointsize = 20;
                } else if (text.equalsIgnoreCase("dz")) { // Drizzle
                    PolygonSymbol newOne = new PolygonSymbol();
                    newOne.color = Color.GREEN;
                    newOne.ocolor = Color.BLACK;
                    rr = SymbolFactory.getSymbolRenderer(newOne);
                    rr.setSymbol(newOne);
                    newOne.toCircle();
                    newOne.pointsize = 20;
                }

            }
        } else {
        }
        if (rr == null) {
            rr = myDefault; // Fall back renderer
        }
        return rr;
    }
}

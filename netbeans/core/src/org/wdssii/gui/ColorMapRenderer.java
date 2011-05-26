package org.wdssii.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import javax.swing.JComponent;
import org.wdssii.gui.ColorMap.ColorMapOutput;

/**
 * Renders a ColorMap to a graphic2D
 * FIXME: merge the 'logic' from opengl renderer and this renderer so that
 * changes to one sync to the other.
 * 
 * @author Robert Toomey
 */
public class ColorMapRenderer extends JComponent {

    ColorMap myColorMap;

    public void setColorMap(ColorMap c) {
        myColorMap = c;
    }

    @Override
    public void paintComponent(Graphics g) {

        CommandManager man = CommandManager.getInstance();
        myColorMap = man.getCurrentColorMap();

        if (myColorMap != null) {
            int w = getWidth();  // Get height and width
            int h = getHeight();
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            FontRenderContext frc = g2.getFontRenderContext();
            Font f = Font.decode("Arial-PLAIN-12"); // shared with opengl code

            FontMetrics metrics = g2.getFontMetrics(f);
            int padding = 10;
            int textHeight = metrics.getMaxAscent() + metrics.getMaxDescent();
            int renderY = metrics.getMaxAscent() + (padding / 2);
            int cellHeight = textHeight + padding;

            ColorMapOutput hi = new ColorMapOutput();
            ColorMapOutput lo = new ColorMapOutput();

            // Width of unit text
            int wtxt = 0;
            String unitName = myColorMap.getUnits();
            if ((unitName != null) && (unitName.length() > 0)) {
                wtxt = metrics.stringWidth(unitName) + 2;
            } else {
                wtxt = 0;
            }

            // Calculate height
            int barwidth = Math.max(w - wtxt, 1);
            int aSize = myColorMap.getNumberOfBins();
            int cellWidth = barwidth / aSize;
            barwidth = cellWidth * aSize;

            int currentX = 0;
            int top = 0;

            // Erase square of colormap
            g2.setColor(Color.BLACK);
            g2.fillRect(0, top, w, cellHeight);

            // Draw the boxes of the color map....
            for (int i = 0; i < aSize; i++) {

                myColorMap.getUpperBoundColor(hi, i);
                myColorMap.getLowerBoundColor(lo, i);

                Color loC = new Color(lo.redI(), lo.greenI(), lo.blueI());
                Color hiC = new Color(hi.redI(), hi.greenI(), hi.blueI());
                int curInt = (int) (currentX);

                GradientPaint p = new GradientPaint(curInt, top, loC,
                        curInt + cellWidth, top, hiC);
                g2.setPaint(p);
                g2.fillRect(curInt, top, cellWidth, cellHeight);

                currentX += cellWidth;
            }

            // Draw the text labels for bins
            boolean drawText = (barwidth >= 100);
            int viewx = 0;
            if (drawText) {
                currentX = viewx;
                int extraXGap = 7; // Force at least these pixels
                // between labels
                int drawnToX = viewx;
                for (int i = 0; i < aSize; i++) {
                    String label = myColorMap.getBinLabel(i);
                    wtxt = metrics.stringWidth(label);

                    // Sparse draw, skipping when text overlaps
                    if (currentX >= drawnToX) {

                        // Don't draw if text sticks outside box
                        if (currentX + wtxt < (viewx + barwidth)) {

                            // Ok, render and remember how far it drew
                            TextLayout t2 = new TextLayout(label, f, frc);
                            // Shape outline = t2.getOutline(null);

                            cheezyOutline(g2, currentX + 2, renderY, t2);
                            drawnToX = (int) (currentX + wtxt + extraXGap);
                        }
                    }

                    currentX += cellWidth;
                }
            }
            // Draw the units
            if (unitName.length() > 0) {
                wtxt = metrics.stringWidth(unitName);
                int start = (viewx + w - wtxt);
                TextLayout t2 = new TextLayout(unitName, f, frc);
                cheezyOutline(g2, start, renderY, t2);
            }
        }
    }

    /** A cheezy outline behind the text that doesn't require an outline
     * font to render.
     */
    public void cheezyOutline(Graphics2D g, int x, int y, TextLayout t) {

        // Draw a 'grid' of background to shadow the character....
        // We can get away with this because there aren't that many labels
        // in a color key really. Draw 8 labels shifted to get outline.
        g.setColor(Color.black);
        t.draw(g, x + 1, y + 1);
        t.draw(g, x, y + 1);
        t.draw(g, x - 1, y + 1);
        t.draw(g, x - 1, y);
        t.draw(g, x - 1, y - 1);
        t.draw(g, x, y - 1);
        t.draw(g, x + 1, y - 1);
        t.draw(g, x + 1, y);

        g.setColor(Color.white);
        t.draw(g, x, y);
    }

    public void render() {
        CommandManager man = CommandManager.getInstance();
        ColorMap aColorMap = man.getCurrentColorMap();
        // Created text renderer once for bin labels
        // Resize in netbeans causing TextRenderer to get messed up
        // somehow.  This fixes it for now at least until I can investigate
        // further..might be gl state.
        // aText = null;
        // if (aText == null) {  // Only create once for speed.  We draw a LOT
        //     aText = new TextRenderer(Font.decode("Arial-PLAIN-12"), true, true);
        //  }
/*
        // Colorkey covers entire width of current viewport
        java.awt.Rectangle viewport = dc.getView().getViewport();
        this.iconWidth = viewport.width - (2 * this.borderWidth);
        this.iconHeight = 20; // 20 pixels for icon scale of '1'
        
        // percentage the size based on the scale number..not really needed
        /// for colorkey I think...
        double width = this.getScaledIconWidth();
        double height = this.getScaledIconHeight();
        
        // Created text renderer once for bin labels
        // Resize in netbeans causing TextRenderer to get messed up
        // somehow.  This fixes it for now at least until I can investigate
        // further..might be gl state.
        aText = null;
        if (aText == null) {  // Only create once for speed.  We draw a LOT
        aText = new TextRenderer(Font.decode("Arial-PLAIN-12"), true, true);
        }
        
        // Bounds calculations
        final int fontYOffset = 5;
        final Rectangle2D maxText = aText.getBounds("gW"); // a guess of
        // size
        // (FIXME:
        // better
        // guess?)
        final int textHeight = (int) (maxText.getHeight());
        final int bheight = textHeight + fontYOffset + fontYOffset;
        int top = viewport.y + bheight - 1;
        int bottom = top - bheight;
        
        GL gl = dc.getGL();
        try {
        
        // System.out.println("Drawing color key layer");
        gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT
        | GL.GL_ENABLE_BIT | GL.GL_TEXTURE_BIT
        | GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT
        | GL.GL_CURRENT_BIT);
        attribsPushed = true;
        
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDisable(GL.GL_DEPTH_TEST);
        
        // Load a parallel projection with xy dimensions (viewportWidth,
        // viewportHeight)
        // into the GL projection matrix.
        gl.glMatrixMode(javax.media.opengl.GL.GL_PROJECTION);
        gl.glPushMatrix();
        projectionPushed = true;
        
        gl.glLoadIdentity();
        double maxwh = width > height ? width : height;
        gl.glOrtho(0d, viewport.width, 0d, viewport.height, -0.6 * maxwh,
        0.6 * maxwh);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        modelviewPushed = true;
        
        gl.glLoadIdentity();
        // Translate and scale
        double scale = this.computeScale(viewport);
        Vec4 locationSW = this.computeLocation(viewport, scale);
        
        // Scale to 0..1 space
        gl.glTranslated(locationSW.x(), locationSW.y(), locationSW.z());
        gl.glScaled(scale, scale, 1);
        gl.glScaled(width, height, 1d);
        
        if (!dc.isPickingMode()) {
        
        if (aColorMap != null) {
        ColorMapOutput hi = new ColorMapOutput();
        ColorMapOutput lo = new ColorMapOutput();
        // Width of unit text
        int wtxt = 0;
        String unitName = aColorMap.getUnits();
        if ((unitName != null) && (unitName.length() > 0)) {
        Rectangle2D boundsUnits = aText.getBounds(unitName);
        wtxt = (int) (boundsUnits.getWidth() + 2.0d);
        } else {
        wtxt = 0;
        }
        
        // Calculate height
        int barwidth = Math.max(viewport.width - wtxt, 1);
        int aSize = aColorMap.getNumberOfBins();
        int cellWidth = barwidth / aSize;
        barwidth = cellWidth * aSize;
        
        float[] colorRGB = this.color.getRGBColorComponents(null);
        
        gl.glDisable(GL.GL_TEXTURE_2D); // no textures
        
        // Draw background square color
        gl.glColor4ub((byte) this.backColor.getRed(),
        (byte) this.backColor.getGreen(), (byte) this.backColor.getBlue(),
        (byte) (this.backColor.getAlpha() * this.getOpacity()));
        gl.glBegin(GL.GL_POLYGON);
        gl.glVertex3d(0, 0, 0);
        gl.glVertex3d(1, 0, 0);
        gl.glVertex3d(1, 1, 0);
        gl.glVertex3d(0, 1, 0);
        gl.glVertex3d(0, 0, 0);
        gl.glEnd();
        
        gl.glLoadIdentity();
        // Interesting, remove this and is goes above the status
        // line..
        gl.glTranslated(locationSW.x(), locationSW.y(), locationSW.z());
        // Scale to width x height space
        gl.glScaled(scale, scale, 1);
        gl.glShadeModel(GL.GL_SMOOTH); // FIXME: pop attrib
        double currentX = 0.0;
        
        gl.glBegin(GL.GL_QUADS);
        
        for (int i = 0; i < aSize; i++) {
        
        aColorMap.getUpperBoundColor(hi, i);  // FIXME: let bin draw itself for possible future classes?
        aColorMap.getLowerBoundColor(lo, i);
        gl.glColor4f(lo.redF(), lo.greenF(), lo.blueF(),
        (float) this.getOpacity());
        gl.glVertex2d(currentX, top);
        gl.glVertex2d(currentX, bottom);
        gl.glColor4f(hi.redF(), hi.greenF(), hi.blueF(),
        (float) this.getOpacity());
        gl.glVertex2d(currentX + cellWidth, bottom);
        gl.glVertex2d(currentX + cellWidth, top);
        currentX += cellWidth;
        }
        //
        //gl.glColor4f(1.0f,1.0f,1.0f,1.0f);
        // gl.glBegin(GL.GL_LINES); for(int i = 0; i<aSize;i++){
        // gl.glVertex3d(currentX, 0, 0); // Draw a vertical line
        // (at percentage space) gl.glVertex3d(currentX, 1, 0);
        // currentX += xoffset; }
        //
        
        gl.glEnd();
        
        
        // Draw the text labels for bins
        aText.begin3DRendering();
        boolean drawText = (barwidth >= 100);
        if (drawText) {
        currentX = viewport.x;
        int extraXGap = 7; // Force at least these pixels
        // between labels
        int drawnToX = viewport.x;
        for (int i = 0; i < aSize; i++) {
        String label = aColorMap.getBinLabel(i);
        // System.out.println("Label is "+label);
        // if (aColorMap.myColorBins.get(i) == null){
        // System.out.println("---NULL");
        // }
        Rectangle2D boundsLabel = aText.getBounds(label);
        wtxt = (int) (boundsLabel.getWidth());
        // Sparse draw, skipping when text overlaps
        if (currentX >= drawnToX) {
        
        // Don't draw if text sticks outside box
        if (currentX + wtxt < (viewport.x + barwidth)) {
        
        // Ok, render and remember how far it drew
        aText.draw(label, (int) (currentX + 2),
        bottom + fontYOffset);
        drawnToX = (int) (currentX + wtxt + extraXGap);
        }
        }
        
        currentX += cellWidth;
        // Color lower =
        // aColorMap.myColorBins.get(i).getLowerBoundColor();
        }
        }
        //aText.end3DRendering();
        
        // Draw 1px border around and inside the map
        //gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], this.getOpacity());
        gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], 1.0);
        gl.glBegin(GL.GL_LINE_STRIP);
        gl.glVertex3d(viewport.x, top, 0.0);
        gl.glVertex3d(currentX, top, 0.0);
        gl.glVertex3d(currentX, bottom, 0.0);
        gl.glVertex3d(viewport.x, bottom, 0.0);
        gl.glEnd();
        
        // Draw the units
        if (unitName.length() > 0) {
        int start = (viewport.x + viewport.width - wtxt);
        //aText.begin3DRendering();
        aText.draw(unitName, start, bottom + fontYOffset);
        
        }
        aText.end3DRendering();
        
        
        }
        
        
        } else {
        /* Pick mode stuff
        // Hack. CellWidth should be a function, instead we stick it in
        // a variable for moment FIXME
        // Point at = dc.getPickPoint();
        // int cell = (at.x)/cellWidth;
        // myCellWidth = cell;
        
        // World wind has a global pick object list. Add ourselves so we
        // can capture the click
        // This is a unique colored rectangle of our 'click' space
        this.pickSupport.clearPickList();
        this.pickSupport.beginPicking(dc);
        // Draw unique color across the map
        Color color = dc.getUniquePickColor();
        int colorCode = color.getRGB();
        // Add our object(s) to the pickable list
        this.pickSupport
        .addPickableObject(colorCode, this, null, false);
        gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(),
        (byte) color.getBlue());
        gl.glBegin(GL.GL_POLYGON);
        gl.glVertex3d(0, 0, 0);
        gl.glVertex3d(1, 0, 0);
        gl.glVertex3d(1, 1, 0);
        gl.glVertex3d(0, 1, 0);
        gl.glVertex3d(0, 0, 0);
        gl.glEnd();
        // Done picking
        this.pickSupport.endPicking(dc);
        this.pickSupport.resolvePick(dc, dc.getPickPoint(), this);
        
        }
        
        //Draw time simulation window
        gl.glLoadIdentity();
        // Interesting, remove this and is goes above the status
        // line..
        gl.glTranslated(locationSW.x(), locationSW.y(), locationSW.z());
        // Scale to width x height space
        gl.glScaled(scale, scale, 1);
        //gl.glShadeModel(GL.GL_SMOOTH); // FIXME: pop attrib
        String time = CommandManager.getInstance().getProductOrderedSet().getSimulationTimeStamp();
        if (time == null) {
        time = "No products yet";
        }
        // Hack for moment..simulation time for debugging
        aText.begin3DRendering();
        //aText.beginRendering(100,100);
        aText.draw("Time:" + time, 0, bottom - textHeight
        - fontYOffset - fontYOffset - 2);
        //System.out.println("Drawing "+time);
        //aText.endRendering();
        aText.end3DRendering();
         */
    }
}

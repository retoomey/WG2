package org.wdssii.gui.products.renderers.icons;

import java.nio.DoubleBuffer;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.products.renderers.DataTableRenderer;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Color;
import java.awt.Insets;
import java.util.ArrayList;
import javax.media.opengl.GL;
import org.wdssii.geom.Location;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.datatypes.DataTable;
import org.wdssii.datatypes.DataTable.Column;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.products.ProductTextFormatter;
import org.wdssii.xml.iconSetConfig.Tag_polygonTextConfig;

/**
 * First pass use ww annotation object.  Since we have so many icons
 * we'll probably need to make it flyweight
 * @author Robert Toomey
 */
public class PolygonIcon extends BaseIconAnnotation {

    /** Factory for creating MesonetIcon objects */
    public static class PolygonIconFactory {

        public static void create(WdssiiJobMonitor monitor, DataTable aDataTable, Tag_polygonTextConfig polygonTextConfig, ArrayList<BaseIconAnnotation> list) {
            if ((polygonTextConfig != null) && (polygonTextConfig.wasRead())) {
                //if (myTextColorMap == null) {
                    ColorMap textCM = new ColorMap();
                    textCM.initFromTag(polygonTextConfig.textConfig.colorMap, ProductTextFormatter.DEFAULT_FORMATTER);
                 //   myTextColorMap = t;
                //}
                //if (myPolygonColorMap == null) {
                    ColorMap polyCM = new ColorMap();
                    polyCM.initFromTag(polygonTextConfig.polygonConfig.colorMap, ProductTextFormatter.DEFAULT_FORMATTER);
                   // myPolygonColorMap = t;
                //}

                // Color lookup is based upon the dcColumn
                String tColorField = polygonTextConfig.textConfig.dcColumn;
                Column tColumn = aDataTable.getColumnByName(tColorField);

                // Polygon color lookup is based upon the dcColumn
                String pColorField = polygonTextConfig.polygonConfig.dcColumn;
                Column pColumn = aDataTable.getColumnByName(pColorField);

                // Do we have a column with name.  Nulls are ok here
                // textField is the actual TEXT shown in the icon....
                String m = polygonTextConfig.textConfig.textField;
                if (m == null) {
                    m = "?";
                }
                Column aColumn = aDataTable.getColumnByName(m);

                // Create an icon per row in table..using the icon configuration
                ArrayList<Location> loc = aDataTable.getLocations();
                int i = 0;
                for (Location l : loc) {
                    int pValue = 0;
                    int tValue = 0;
                    if (tColumn != null) {
                        String t = tColumn.getValue(i);

                        // FIXME: ok upperbound should be a float, so Age can be a float
                        // as well...so we really should parse knowing the column type?
                        // crap.  I need to have column types somehow..at least, int,
                        // and float, string...
                        tValue = (int) (Float.parseFloat(t));   // SOOOO breakable..      
                    }
                    if (pColumn != null) {
                        String t = pColumn.getValue(i);
                        pValue = (int) (Float.parseFloat(t));   // SOOOO breakable..   
                    }

                    // Add it
                    if (aColumn != null) {
                        String s = aColumn.getValue(i);
                        addIcon(list, l, s, tValue, pValue, polygonTextConfig, textCM, polyCM);
                    } else {
                        addIcon(list, l, m, tValue, pValue, polygonTextConfig, textCM, polyCM);
                    }
                    monitor.subTask("Icon " + ++i);
                }
            }
        }

        public static void addIcon(ArrayList<BaseIconAnnotation> list, Location loc, String text, int cText, int cPolygon, Tag_polygonTextConfig tag,
                ColorMap textCM, ColorMap polyCM) {

            // try to add something....
            AnnotationAttributes eqAttributes;

            // Init default attributes for all eq
            eqAttributes = new AnnotationAttributes();
            eqAttributes.setLeader(AVKey.SHAPE_NONE);

            // Extra space around text...
            eqAttributes.setInsets(new Insets(0, 0, 0, 0));

            // eqAttributes.setDrawOffset(new Point(0, -16));
            //  eqAttributes.setSize(new Dimension(32, 32));
            //   eqAttributes.setBorderWidth(5);
            //   eqAttributes.setCornerRadius(0);
            eqAttributes.setTextColor(Color.WHITE);
            eqAttributes.setBackgroundColor(new Color(0, 0, 255, 255));
            // ea.getAttributes().setImageSource(eqIcons[Math.min(days, eqIcons.length - 1)]);
            //    ea.getAttributes().setTextColor(eqColors[Math.min(days, eqColors.length - 1)]);
            //    ea.getAttributes().setScale(earthquake.magnitude / 10);
            // eqAttributes.setScale(5);
            //   eqAttributes.setTextColor(new Color(255, 0, 0, 0));
            Position p = new Position(new LatLon(
                    Angle.fromDegrees(loc.getLatitude()),
                    Angle.fromDegrees(loc.getLongitude())), 0);
            // loc.getHeightKms());
            PolygonIcon ea = new PolygonIcon(text, p, cText, cPolygon, eqAttributes, tag, textCM, polyCM);
            list.add(ea);
            //  myProducts.addRenderable(ea);
        }
    }
    /** This is the value of the icon for colormap lookup.. 
    FIXME: Note when we add colorDatabase support this won't work since
    it will need a string...hummm..
     */
    private int textColorValue;
    /** This is the value of the polygon for colormap lookup... */
    private int polygonColorValue;
    private Color textColor;
    private Tag_polygonTextConfig tag;
    private ColorMap myTextColorMap = null;
    private ColorMap myPolygonColorMap = null;

    //     public Position position;
    public PolygonIcon(String text, Position p,
            int textValue,
            int polygonValue,
            AnnotationAttributes defaults,
            Tag_polygonTextConfig tag,
            ColorMap textC,
            ColorMap polygonC) {
        super(text, p, defaults);
        this.tag = tag;
        this.textColorValue = textValue;
        this.polygonColorValue = polygonValue;
        this.myTextColorMap = textC;
        this.myPolygonColorMap = polygonC;
        //         this.position = p;
    }

    public void updateColors(ColorMap textColorMap, ColorMap polygonColorMap) {
        // Update the icon text color.
        Color textColor = Color.BLACK;
        try {
            //int value = Integer.parseInt(text);
            if (myTextColorMap != null) {
                ColorMapOutput out = new ColorMapOutput();
                myTextColorMap.fillColor(out, textColorValue);
                textColor = new Color(out.redI(), out.greenI(), out.blueI(), out.alphaI());
                this.getAttributes().setTextColor(textColor);
                // gl.glColor4f(out.redF(), out.greenF(), out.blueF(), out.alphaF());
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void applyScreenTransform(DrawContext dc, int x, int y, int width, int height, double scale) {
        /** This for all purposes sticks the icon on the location without any
         * extra. Worldwind default icon has a 'leader' from icon to the position.
         */
        double finalScale = scale * this.computeScale(dc);
        GL gl = dc.getGL();
        gl.glTranslated(x, y, 0);

        // Not sure we even need this...billboarding using '2d coordinates'
        gl.glScaled(finalScale, finalScale, 1);

    }
    // Override annotation drawing for a simple circle
    private DoubleBuffer shapeBuffer;

    /** Draw our IconAnnotation.  Kinda stuck on how I do this, since
     * I have to read old files/data from the old c++ display :(
     * @param dc
     * @param width
     * @param height
     * @param opacity
     * @param pickPosition 
     */
    @Override
    protected void doDraw(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
        // Draw colored circle around screen point - use annotation's text color
        //       super.doDraw(dc, width, width, opacity, position);  
        if (dc.isPickingMode()) {
            //      if (pickPosition != null){
            //          this.bindPickableObject(dc, pickPosition);
            //      }
        }

        boolean pick = dc.isPickingMode();
        if (!pick && (this == DataTableRenderer.hovered)) {
            width += 6;
            height += 6;
        }
        // FIXME: not sure exactly how to handle missing data yet..
        // right now it always creates a subtag so the defaults are there
        int v = tag.polygonConfig.numVertices;
        int p = tag.polygonConfig.phaseAngle;

        // this.applyColor(dc, new Color(255, 0, 0, 255), 1.0, true);

        GL gl = dc.getGL();

        // Draw the background polygon ---------------------------------

        // Calculate the radius of a bounding circle around the text...
        double cw = width / 2.0;
        double ch = height / 2.0;
        double polyRadius = Math.sqrt(cw * cw + ch * ch);

        // This could be done once per icon, or only when polygon color
        // changes...
        try {
            // int value = Integer.parseInt(text);
            if (!pick) {
                if (myPolygonColorMap != null) {
                    ColorMapOutput out = new ColorMapOutput();
                    myPolygonColorMap.fillColor(out, polygonColorValue);
                    gl.glColor4f(out.redF(), out.greenF(), out.blueF(), out.alphaF());
                }
            }
        } catch (Exception e) {
        }
        if (v > 2) {
            // Background color
            //  gl.glColor3f(0.0f, 0.0f, 1.0f);

            polyRadius /= Math.cos(Math.PI / v);
            gl.glBegin(GL.GL_POLYGON);
            for (int i = 0; i < v; i++) {
                double angle = Math.toRadians(p) + i * 2.0 * Math.PI / v;
                gl.glVertex2d(polyRadius * Math.cos(angle), polyRadius * Math.sin(angle));
            }
            gl.glEnd();

        } else {
            // Doing it this way to avoid extra memory (we can have 1000s of icons)
            // vs buffer which would be faster.  Might change later
            // Background color
            //  gl.glColor3f(0.0f, 0.0f, 1.0f);

            double x = -cw;
            double y = -ch;
            double x2 = x + width;
            double y2 = y + height;

            gl.glBegin(GL.GL_QUADS);
            gl.glVertex2d(x, y);
            gl.glVertex2d(x2, y);
            gl.glVertex2d(x2, y2);
            gl.glVertex2d(x, y2);
            gl.glEnd();

            gl.glColor3f(1.0f, 1.0f, 1.0f);
            gl.glBegin(GL.GL_LINE_LOOP);
            gl.glVertex2d(x, y);
            gl.glVertex2d(x2, y);
            gl.glVertex2d(x2, y2);
            gl.glVertex2d(x, y2);
            gl.glEnd();
        }
        // Think I will eventually make my own in order to outline
        // the text...
        // This 'centers' the text around the lat/lon location...
        dc.getGL().glTranslated(-width / 2, -height / 2, 0);

        if (!pick) {
            // Total sloppy hacking for moment
            // This could be done only when column text changes or color map changes
            Color textColor = Color.BLACK;
            try {
                //int value = Integer.parseInt(text);
                if (myTextColorMap != null) {
                    ColorMapOutput out = new ColorMapOutput();
                    myTextColorMap.fillColor(out, textColorValue);
                    textColor = new Color(out.redI(), out.greenI(), out.blueI(), out.alphaI());
                    this.getAttributes().setTextColor(textColor);
                    // gl.glColor4f(out.redF(), out.greenF(), out.blueF(), out.alphaF());
                }
            } catch (Exception e) {
            }

            drawText(dc, width, height, opacity, pickPosition);
        }
    }
}

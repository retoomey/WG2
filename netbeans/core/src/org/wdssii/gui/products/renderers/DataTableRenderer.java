package org.wdssii.gui.products.renderers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.BasicAnnotationRenderer;
import java.awt.Point;

import gov.nasa.worldwind.render.DrawContext;

import gov.nasa.worldwind.render.GlobeAnnotation;
import java.awt.Color;
import java.awt.Insets;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import javax.media.opengl.GL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.wdssii.geom.Location;
import org.wdssii.gui.CommandManager;

import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataTable;
import org.wdssii.datatypes.DataTable.Column;
import org.wdssii.datatypes.DataType;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.ProductManager.ProductDataInfo;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductReadout;
import org.wdssii.gui.products.ProductTextFormatter;
import org.wdssii.gui.products.RadialSetReadout;
import org.wdssii.xml.iconSetConfig.Tag_dataColumn;
import org.wdssii.xml.iconSetConfig.Tag_iconSetConfig;
import org.wdssii.xml.iconSetConfig.Tag_mesonetConfig;
import org.wdssii.xml.iconSetConfig.Tag_polygonTextConfig;

/** Renders a DataTable in a worldwind window
 * 
 * Lots of mess here, will need cleanup, redesign, etc....just trying to get
 * it to work at moment...
 * 
 * @author Robert Toomey
 *
 */
public class DataTableRenderer extends ProductRenderer {

    private ArrayList<Annotation> myIcons = new ArrayList<Annotation>();
    private static Log log = LogFactory.getLog(DataTableRenderer.class);
    private static BasicAnnotationRenderer myRenderer = new BasicAnnotationRenderer();
    private ColorMap myTextColorMap = null;
    private ColorMap myPolygonColorMap = null;

    public DataTableRenderer() {
        super(true);
    }

    @Override
    public WdssiiJobStatus createForDatatype(DrawContext dc, Product aProduct, WdssiiJobMonitor monitor) {

        // Make sure and always start monitor
        DataTable aDataTable = (DataTable) aProduct.getRawDataType();
        monitor.beginTask("DataTableRenderer:", aDataTable.getNumRows());
        ProductDataInfo info = ProductManager.getInstance().getProductDataInfo(aProduct.getDataType());
        Tag_iconSetConfig tag = info.getIconSetConfig();

        Tag_polygonTextConfig polygonTextConfig = tag.polygonTextConfig;
        Tag_mesonetConfig mesonetConfig = tag.mesonetConfig;

        // For the moment, keep polygonTextConfig and mesonet separate....
        if (mesonetConfig != null) {
            
            Tag_dataColumn dataCol = mesonetConfig.dataColumn;

            // Maybe we could create an array of these....
            // FIXME: lots of string --> columnName --> parse row (i) stuff
            // this should be generized some way.
            
            // Try to get the 'Direction' column.
            Column dirColumn = null;
            if (dataCol != null){
                dirColumn = aDataTable.getColumnByName(dataCol.directionCol);
            }
            
            // Try to get the 'Speed' column.
            Column speedColumn = null;
            if (dataCol != null){
                speedColumn = aDataTable.getColumnByName(dataCol.speedCol);
            }
            
            //String atColumnS = mesonetConfig.dataColumn.airTemperatureCol; 
            // Column atColumn = aDataTable.getColumnByName(atColumnS);
            // Create an icon per row in table..using the icon configuration
            ArrayList<Location> loc = aDataTable.getLocations();
            int i = 0;
            for (Location l : loc) {
                float speed = DataType.MissingData;
                float direction = DataType.MissingData;
                if (dirColumn != null){
                    direction = dirColumn.getFloat(i);
                }
                if (speedColumn != null){
                    speed = speedColumn.getFloat(i);
                }
                
                addMesonet(l, direction, speed);
                monitor.subTask("Icon " + ++i);
            }
        } else if (polygonTextConfig != null) {
            // FIXME: These are all special cases because of legacy data/code...
            // need to generalize it....currently all this code is a giant mess...
            if (myTextColorMap == null) {
                ColorMap t = new ColorMap();
                t.initFromTag(tag.polygonTextConfig.textConfig.colorMap, ProductTextFormatter.DEFAULT_FORMATTER);
                myTextColorMap = t;
            }
            if (myPolygonColorMap == null) {
                ColorMap t = new ColorMap();
                t.initFromTag(tag.polygonTextConfig.polygonConfig.colorMap, ProductTextFormatter.DEFAULT_FORMATTER);
                myPolygonColorMap = t;
            }

            // Color lookup is based upon the dcColumn
            String tColorField = tag.polygonTextConfig.textConfig.dcColumn;
            Column tColumn = aDataTable.getColumnByName(tColorField);

            // Polygon color lookup is based upon the dcColumn
            String pColorField = tag.polygonTextConfig.polygonConfig.dcColumn;
            Column pColumn = aDataTable.getColumnByName(pColorField);

            // Do we have a column with name.  Nulls are ok here
            // textField is the actual TEXT shown in the icon....
            String m = tag.polygonTextConfig.textConfig.textField;
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
                    addIcon(l, s, tValue, pValue, tag);
                } else {
                    addIcon(l, m, tValue, pValue, tag);
                }
                monitor.subTask("Icon " + ++i);
            }
        }

        CommandManager.getInstance().updateDuringRender();  // Humm..different thread...
        monitor.done();
        setIsCreated();
        return WdssiiJobStatus.OK_STATUS;
    }

    /** Experimental readout using drawing to get it..lol 
     * FIXME: generalize this ability for all products
     */
    @Override
    public ProductReadout getProductReadout(Point p, DrawContext dc) {
        RadialSetReadout out = new RadialSetReadout();
        return out;
    }

    /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    public void drawData(DrawContext dc, boolean readoutMode) {
        // for (GlobeAnnotation a : myIcons) {
        //     a.render(dc);
        // }

        // myRenderer.render(dc, this, null, dc.getCurrentLayer());
        DrawContext a = dc;
        Iterable<Annotation> b = myIcons;
        Layer c = dc.getCurrentLayer();

        // Render many, but this doesn't order I think...
        myRenderer.render(a, myIcons, c);

        //  myRenderer.render
    }

    public void addIcon(Location loc, String text, int cText, int cPolygon, Tag_iconSetConfig tag) {

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
        IconAnnotation ea = new IconAnnotation(text, p, cText, cPolygon, eqAttributes, tag);
        myIcons.add(ea);
        //  myProducts.addRenderable(ea);
    }

    public void addMesonet(Location loc, float direction, float speed) {

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
        MesonetIcon ea = new MesonetIcon(p, direction, speed, eqAttributes, null);
        myIcons.add(ea);
        //  myProducts.addRenderable(ea);
    }

    /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(DrawContext dc) {
        drawData(dc, false);
    }

    /**
     * First pass use ww annotation object.  Since we have so many icons
     * we'll probably need to make it flyweight
     */
    private class IconAnnotation extends GlobeAnnotation {
        // public Earthquake earthquake;

        /** This is the value of the icon for colormap lookup.. 
        FIXME: Note when we add colorDatabase support this won't work since
        it will need a string...hummm..
         */
        private int textColorValue;
        /** This is the value of the polygon for colormap lookup... */
        private int polygonColorValue;
        private Color textColor;
        private Tag_iconSetConfig tag;

        //     public Position position;
        public IconAnnotation(String text, Position p,
                int textValue,
                int polygonValue,
                AnnotationAttributes defaults,
                Tag_iconSetConfig tag) {
            super(text, p, defaults);
            this.tag = tag;
            this.textColorValue = textValue;
            this.polygonColorValue = polygonValue;
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

            /*double finalScale = scale * this.computeScale(dc);
            java.awt.Point offset = this.getAttributes().getDrawOffset();
            
            GL gl = dc.getGL();
            gl.glTranslated(x, y, 0);
            gl.glScaled(finalScale, finalScale, 1);
            gl.glTranslated(offset.x, offset.y, 0);
            gl.glTranslated(-width / 2, 0, 0);*/
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
                this.bindPickableObject(dc, pickPosition);

                // FIXME: just draw filled outline for picking, it's quicker
            }

            // FIXME: not sure exactly how to handle missing data yet..
            // right now it always creates a subtag so the defaults are there
            int v = tag.polygonTextConfig.polygonConfig.numVertices;
            int p = tag.polygonTextConfig.polygonConfig.phaseAngle;

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
                if (myPolygonColorMap != null) {
                    ColorMapOutput out = new ColorMapOutput();
                    myPolygonColorMap.fillColor(out, polygonColorValue);
                    gl.glColor4f(out.redF(), out.greenF(), out.blueF(), out.alphaF());
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
        /**
         * Render the annotation. Called as a Renderable.
         *
         * @param dc the current DrawContext.
         */
        /*   @Override
        public void render(DrawContext dc) {
        if (dc == null) {
        // String message = Logging.getMessage("nullValue.DrawContextIsNull");
        // Logging.logger().severe(message);
        throw new IllegalArgumentException("bleh");
        }
        
        if (!this.getAttributes().isVisible()) {
        return;
        }
        myRenderer.render(dc, this, null, dc.getCurrentLayer());
        //  AnnotationRenderer z = dc.getAnnotationRenderer();
        // dc.addOrderedRenderable();
        
        // I don't want the product part of any other annotation stuff...
        // so we have our own annotation renderer here....
        // dc.getAnnotationRenderer().render(dc, this, null, dc.getCurrentLayer());
        }
         * 
         */
    }

    /**
     * First hack of mesonet icons
     */
    private class MesonetIcon extends GlobeAnnotation {

        private Tag_mesonetConfig tag;
        private float myDirection = DataType.MissingData;
        private float mySpeed = DataType.MissingData;
        
        int barbRadius = 30; // windbarb tag stuff
        double superUnit = 50;
        double superTolerance = 10;
        double baseUnit = 10;
        double baseTolerance = 2;
        double halfUnit = 5;
        double halfTolerance = 5;
        int myCrossHairRadius = 5;
        
        //     public Position position;
        public MesonetIcon(Position p,
                float direction,
                float speed,
                AnnotationAttributes defaults,
                Tag_mesonetConfig tag) {
            super("", p, defaults);
            this.tag = tag;
            this.myDirection = direction;
            this.mySpeed = speed;
            try { // sloppy for now, clean up
                superUnit = tag.windBarb.superUnit.value;
                superTolerance = tag.windBarb.superUnit.tolerance;
                baseUnit = tag.windBarb.baseUnit.value;
                baseTolerance = tag.windBarb.baseUnit.tolerance;
                halfUnit = tag.windBarb.halfUnit.value;
                halfTolerance = tag.windBarb.halfUnit.tolerance;
                myCrossHairRadius = tag.output.windBarb.crossHairRadius;
                barbRadius = tag.windBarb.barbRadius;
                // units? speedUnit..
            }catch(Exception e){
                
            }
        }

        /** Simple billboard in 2D */
        @Override
        protected void applyScreenTransform(DrawContext dc, int x, int y, int width, int height, double scale) {
            double finalScale = scale * this.computeScale(dc);
            GL gl = dc.getGL();
            gl.glTranslated(x, y, 0);

            // Not sure we even need this...billboarding using '2d coordinates'
            gl.glScaled(finalScale, finalScale, 1);
        }
        
        // Override annotation drawing for a simple circle
        private DoubleBuffer shapeBuffer;

        /** True modulus */
        private float mod(float x, float y) {
            float result = x % y;
            if (result < 0) {
                result += y;
            }
            return result;
        }

        /** Draw our IconAnnotation.  Kinda stuck on how I do this, since
         * I have to read old files/data from the old c++ display :(
         * @param dc
         * @param width
         * @param height
         * @param opacity
         * @param pickPosition 
         * 
         */
        @Override
        protected void doDraw(DrawContext dc, int width, int height, double opacity, Position pickPosition) {
            // Draw colored circle around screen point - use annotation's text color
            //       super.doDraw(dc, width, width, opacity, position);
            if (dc.isPickingMode()) {
                // this.bindPickableObject(dc, pickPosition);
                return;
            }
            drawWindBarb(dc.getGL());
        }
        
        /** At the moment, draw a wind barb kinda sloppy.  Should cache
         * this stuff.
         * @param gl 
         */
        public void drawWindBarb(GL gl){
            float direction = myDirection; // This is from direction column, Not missing...
            float speed = mySpeed; // speed from data column (not missing)
            
            // temp crosshair radius
            int cr = myCrossHairRadius;
            
            if ((direction == DataType.MissingData) || (speed == DataType.MissingData)){
                // Can't do windbarb, do a box instead...
               float h = cr/2.0f;
               gl.glRectd(-h, -h, cr, cr);
               return;
            }
            
            // Stuff from configuration...hardcoded at moment
            //Color windBarColor = Color.WHITE;
            

            // interval between two parallel lines: we assume the # of such lines
            // are less than 10.  Dividing gives us 'step' per line along the
            // axis
            double step = barbRadius / 10.0f;
            double rlen = step * 4;  // length of a super unit/base unit line
            double hlen = step * 2;  // length of a half unit line
            double fval1, fval2, wspd, wdir1, wdir2, cs1, cs2, sn1, sn2;
            int ibarb, iflag, ihalf, k, n;

            float w1 = mod(90.0f - direction + 260.0f, 360.0f);
            float w2 = mod(90.0f - 60 - direction + 360.0f, 360.0f);

            // This is getting the angle from the direction value, creating
            // a line from (0,0) to that point on the circle
            wdir1 = Math.toRadians(w1);
            wdir2 = Math.toRadians(w2);
            cs1 = Math.cos(wdir1);
            cs2 = Math.cos(wdir2);
            sn1 = Math.sin(wdir1);
            sn2 = Math.sin(wdir2);

            // main axis.  0,0 is center of icon....
            double x0 = 0, y0 = 0, x1, x2, y1, y2;
            x2 = x0 + (barbRadius * cs1);
            y2 = y0 + (barbRadius * sn1);
            n = 1; // Count first line
            iflag = ibarb = ihalf = 0;
            wspd = speed;

            // Number of superunits: no more than 5
            for (k = 0; k < 5; k++) {
                if (wspd > superUnit - superTolerance) {
                    wspd = wspd - superUnit;
                    iflag++;
                }
            }

            // Number of baseunits: no more than 5
            for (k = 0; k < 5; k++) {
                if (wspd > baseUnit - baseTolerance) {
                    wspd = wspd - baseUnit;
                    ibarb++;
                }
            }

            // whether there is a halfunit
            if (wspd > halfUnit - halfTolerance) {
                ihalf = 1;
            }


            gl.glBegin(GL.GL_LINES);

            // Draw a cross hair.
            gl.glVertex2d(-cr, 0);
            gl.glVertex2d(cr, 0);
            gl.glVertex2d(0, cr);
            gl.glVertex2d(0, -cr);

            // Draw first line of barb
            gl.glVertex2d(x0, y0);
            gl.glVertex2d(x2, y2);

            // Draw super unit triangles...
            for (k = 0; k < iflag; k++) {

                // First line...
                fval1 = barbRadius - ((double) (n - 1) * step);
                x1 = x0 + (fval1 * cs1);
                y1 = y0 + (fval1 * sn1);
                x2 = x1 + (rlen * cs2);
                y2 = y1 + (rlen * sn2);
                gl.glVertex2d(x1, y1);
                gl.glVertex2d(x2, y2);
                // Second line...
                fval2 = fval1 - step;
                x1 = x0 + (fval2 * cs1);
                y1 = y0 + (fval2 * sn1);
                gl.glVertex2d(x1, y1);
                gl.glVertex2d(x2, y2);
                n += 2;
            }

            // Base unit: lines
            for (k = 0; k < ibarb; k++) {
                fval1 = barbRadius - ((double) (n - 1) * step);
                x1 = x0 + (fval1 * cs1);
                y1 = y0 + (fval1 * sn1);
                x2 = x1 + (rlen * cs2);
                y2 = y1 + (rlen * sn2);
                gl.glVertex2d(x1, y1);
                gl.glVertex2d(x2, y2);
                n++;
            }

            // halfUnits: actually only one
            if (ihalf > 0) {
                fval1 = barbRadius - ((double) (n - 1) * step);
                // if we haven't drawn any lines except main axis,
                // then back two steps to draw the first line
                // the only line for the half unit
                if (n == 1 && wspd < (halfUnit * 2.0)) {
                    fval1 = fval1 - step * 2;

                }
                x1 = x0 + (fval1 * cs1);
                y1 = y0 + (fval1 * sn1);
                x2 = x1 + (hlen * cs2);
                y2 = y1 + (hlen * sn2);
                gl.glVertex2d(x1, y1);
                gl.glVertex2d(x2, y2);
            }
            gl.glEnd();
        }
    }
}

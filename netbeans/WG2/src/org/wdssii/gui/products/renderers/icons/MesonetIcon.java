package org.wdssii.gui.products.renderers.icons;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.OGLStackHandler;
import java.awt.Color;
import java.awt.Insets;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import javax.media.opengl.GL;
import org.wdssii.geom.Location;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.datatypes.DataTable;
import org.wdssii.datatypes.DataTable.Column;
import org.wdssii.datatypes.DataType;
import org.wdssii.xml.iconSetConfig.DataColumn;
import org.wdssii.xml.iconSetConfig.MesonetConfig;

/**
 *  First hack of mesonet icons
 * @author Robert Toomey
 */
public class MesonetIcon extends BaseIconAnnotation {

    /** Factory for creating MesonetIcon objects */
    public static class MesonetIconFactory {

        public static void create(WdssiiJobMonitor m, DataTable aDataTable, MesonetConfig mesonetConfig, ArrayList<BaseIconAnnotation> list) {

            if ((mesonetConfig != null)) {
                DataColumn dataCol = mesonetConfig.dataColumn;

                // Try to get the 'Direction' column.
                Column dirColumn = null;
                if (dataCol != null) {
                    dirColumn = aDataTable.getColumnByName(dataCol.directionCol);
                }

                // Try to get the 'Speed' column.
                Column speedColumn = null;
                if (dataCol != null) {
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
                    if (dirColumn != null) {
                        direction = dirColumn.getFloat(i);
                    }
                    if (speedColumn != null) {
                        speed = speedColumn.getFloat(i);
                    }

                    addMesonet(list, l, direction, speed);
                    m.subTask("Icon " + ++i);
                }
            }
        }

        public static void addMesonet(ArrayList<BaseIconAnnotation> list, Location loc, float direction, float speed) {

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
            list.add(ea);
            //  myProducts.addRenderable(ea);
        }
    }
    private MesonetConfig tag;
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
            MesonetConfig tag) {
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
        } catch (Exception e) {
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

    protected double getPixelSizeAtLocation(DrawContext dc, Position p) {
        Globe globe = dc.getGlobe();
        Vec4 locationPoint = globe.computePointFromPosition(p);
        //Vec4 locationPoint = globe.computePointFromPosition(location.getLatitude(), location.getLongitude(),
        //     globe.getElevation(location.getLatitude(), location.getLongitude()));
        double distance = dc.getView().getEyePoint().distanceTo3(locationPoint);
        return dc.getView().computePixelSizeAtDistance(distance);
    }

    @Override
    public void do3DDraw(DrawContext dc) {

        Vec4 point = this.getAnnotationDrawPoint(dc);
        if (point == null) {
            return;
        }
        if (dc.getView().getFrustumInModelCoordinates().getNear().distanceTo(point) < 0) {
            return;
        }
        Vec4 screenPoint = dc.getView().project(point);
        if (screenPoint == null) {
            return;
        }

        // java.awt.Dimension size = this.getPreferredSize(dc);
        // Position pos = dc.getGlobe().computePositionFromPoint(point);
        this.setDepthFunc(dc, screenPoint);

        Position p1 = getPosition();

        /** Bigger the further so it stays 'same' size in 2D */
        double finalScale = getPixelSizeAtLocation(dc, p1);

        /** North becomes Y, East x */
        Matrix m = dc.getGlobe().computeModelCoordinateOriginTransform(p1);

        OGLStackHandler h = new OGLStackHandler();
        GL gl = dc.getGL();
        h.pushModelview(dc.getGL());
        Matrix modelview = dc.getView().getModelviewMatrix();
        modelview = modelview.multiply(m);

        double[] compArray = new double[16];
        Matrix transform = Matrix.IDENTITY;
        transform = transform.multiply(modelview);
        transform = transform.multiply(Matrix.fromScale(finalScale));
        transform.toArray(compArray, 0, false);
        gl.glLoadMatrixd(compArray, 0);

        // Poor way of outlining lol
        if (!dc.isPickingMode()) {
            gl.glColor3f(0, 0, 0);
        }
        gl.glLineWidth(4);
        drawWindBarb3D(dc.getGL());
        gl.glLineWidth(1);
        if (!dc.isPickingMode()) {
            gl.glColor3f(1.0f, 1.0f, 1.0f);
        }
        drawWindBarb3D(dc.getGL());

        h.pop(gl);
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

        GL gl = dc.getGL();
        // Poor way of outlining lol
        gl.glColor3f(0, 0, 0);
        gl.glLineWidth(4);
        drawWindBarb2D(dc.getGL());
        gl.glLineWidth(1);
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        drawWindBarb2D(dc.getGL());
        // drawWindBarb(dc.getGL());
    }

    public void drawWindBarb2D(GL gl) {
        int cr = myCrossHairRadius;
        float direction = myDirection; // This is from direction column, Not missing...
        float speed = mySpeed; // speed from data column (not missing)

        // Draw a billboard box on missing...
        if ((direction == DataType.MissingData) || (speed == DataType.MissingData)) {
            float h = cr / 2.0f;
            gl.glRectd(-h, -h, cr, cr);
            return;
        }

        // Draw a cross hair.
        gl.glBegin(GL.GL_LINES);
        gl.glVertex2d(-cr, 0);
        gl.glVertex2d(cr, 0);
        gl.glVertex2d(0, cr);
        gl.glVertex2d(0, -cr);
        gl.glEnd();
    }

    /** At the moment, draw a wind barb kinda sloppy.  Should cache
     * this stuff.
     * 
     * Draw the flat surface projection part of the windbarb
     * @param gl 
     */
    public void drawWindBarb3D(GL gl) {
        float direction = myDirection; // This is from direction column, Not missing...
        float speed = mySpeed; // speed from data column (not missing)

        // temp crosshair radius
        // int cr = myCrossHairRadius;

        if ((direction == DataType.MissingData) || (speed == DataType.MissingData)) {
            // Can't do windbarb, do a box instead...
            // float h = cr / 2.0f;
            // gl.glRectd(-h, -h, cr, cr);
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
        double fval1, fval2, wspd, cs1, cs2, sn1, sn2;
        int ibarb, iflag, ihalf, k, n;

        // Angle for the MAIN line of the mesonet. 0 north, 90 east
        float w1 = mod(direction, 360.0f);

        // Angle for the 'barbs' sticking off.  This is relative to the
        // base barb line, so 90 would make the barbs perpendicular to the
        // main line of the windbarb.  Positive sticks away from the barb
        // in the clockwise direction.
        float w2 = w1 + 60.0f;

        // This is getting the angle from the direction value, creating
        // a line from (0,0) to that point on the circle
        double wdir1 = Math.toRadians(w1);
        double wdir2 = Math.toRadians(w2);
        cs1 = Math.cos(wdir1);
        sn1 = Math.sin(wdir1);

        cs2 = Math.cos(wdir2);
        sn2 = Math.sin(wdir2);

        // main axis.  0,0 is center of icon....
        double x0 = 0, y0 = 0, x1, x2, y1, y2;
        x2 = x0 + (barbRadius * sn1);
        y2 = y0 + (barbRadius * cs1);
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
        // gl.glVertex2d(-cr, 0);
        /// gl.glVertex2d(cr, 0);
        // gl.glVertex2d(0, cr);
        // gl.glVertex2d(0, -cr);

        // Draw first line of barb
        gl.glVertex2d(x0, y0);
        gl.glVertex2d(x2, y2);

        // Draw super unit triangles...
        for (k = 0; k < iflag; k++) {

            // First line...
            fval1 = barbRadius - ((double) (n - 1) * step);
            x1 = x0 + (fval1 * sn1);
            y1 = y0 + (fval1 * cs1);
            x2 = x1 + (rlen * sn2);
            y2 = y1 + (rlen * cs2);
            gl.glVertex2d(x1, y1);
            gl.glVertex2d(x2, y2);
            // Second line...
            fval2 = fval1 - step;
            x1 = x0 + (fval2 * sn1);
            y1 = y0 + (fval2 * cs1);
            gl.glVertex2d(x1, y1);
            gl.glVertex2d(x2, y2);
            n += 2;
        }

        // Base unit: lines
        for (k = 0; k < ibarb; k++) {
            fval1 = barbRadius - ((double) (n - 1) * step);
            x1 = x0 + (fval1 * sn1);
            y1 = y0 + (fval1 * cs1);
            x2 = x1 + (rlen * sn2);
            y2 = y1 + (rlen * cs2);
            gl.glVertex2d(x1, y1);
            gl.glVertex2d(x2, y2);
            n++;
        }

        // halfUnits: actually only one
        if (ihalf > 0) {
            fval1 = barbRadius - ((double) (n - 1) * step);
            // if we haven't drawn any lines except main axis,
            // then back two steps to draw the first line
            // the only line for the half 
            if (n == 1 && wspd < (halfUnit * 2.0)) {
                fval1 = fval1 - step * 2;

            }
            x1 = x0 + (fval1 * sn1);
            y1 = y0 + (fval1 * cs1);
            x2 = x1 + (hlen * sn2);
            y2 = y1 + (hlen * cs2);
            gl.glVertex2d(x1, y1);
            gl.glVertex2d(x2, y2);
        }
        gl.glEnd();
    }
}
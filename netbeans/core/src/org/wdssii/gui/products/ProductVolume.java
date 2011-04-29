package org.wdssii.gui.products;

// I want to remove these dependencies
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.wdssii.datatypes.DataType;
import org.wdssii.geom.CPoint;
import org.wdssii.geom.CVector;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.products.filters.DataFilter.DataValueRecord;

/** The volume product is a collection of regular products that is 3D in the data dimension. 
 * It has extra abilities, such as vertical slice
 * or isosurfaces.  Typically collections of products have a 'merged' 3d created by interpolating data
 * of multiple single products.  For example, a RadialSet is '3d' on the screen, but is '2d' in data..that is,
 * it has a range and angle.
 * 
 * @author Robert Toomey
 */
public class ProductVolume {

    /** The key that uniquely defines this volume (for caching/updating purposes) */
    protected String myKey = "";

    /** Called on every access to VolumeProduct.  Allows lazy checking of state changes, initialization */
    public void initVirtual(Product p, boolean virtual) {
    }

    /*  Get the record list displayed for this volume in the volume viewer,
     * assuming they are available for this type of volume product
     */
    // Sync errors
    //public ArrayList<IndexRecord> getRecordList(){
    //	return null;
    //}
    /** The data value record used to run filters.  Subclasses can extend this for their own type,
     * and add more information for filters to use. */
    public DataValueRecord getNewDataValueRecord() {
        return new DataValueRecord();
    }

    // filler function, will have to be more advanced..
    public boolean getValueAt(double lat, double lon, double height, ColorMapOutput output, DataValueRecord out,
            FilterList list, boolean useFilters) {
        //ArrayList<DataFilter> list){

        // Get value as fast as possible...
        if (height < 0) {
            output.setColor(0, 0, 0, 0);
            //output.red = output.green = output.blue = output.alpha = 0;
            return true;
        }
        // Sphere test for vslice/isosurface.  Return a blue color based on distance from the center.
        // If past the radius, return 0/black for value...
        Location l = new Location(lat, lon, 0);  // Newing this here slllows us down
        Location b = new Location(35.33, -97.27, 0.0);

        CPoint c1 = l.getCPoint();  // Really need to get rid of these cpoint thingies..because we have multiple projections in the new display.
        CPoint c2 = b.getCPoint();
        CVector dv = c1.minus(c2);
        double distance = dv.norm();

        //System.out.println("Distance "+distance);
        if (distance > 10000.0) {
            output.setColor(0, 0, 0, 0);
            //output.red = output.green = output.blue = output.alpha = 0;
        } else {
            output.setColor(0, 0, 255, 255);
            //	output.red = 0;
            //output.green = 0;
            //	output.blue = output.alpha = 255;
        }
        return true;
    }

    /** Generate a key that uniquely determines this volume based on product data */
    public String getKey() {
        return myKey;
    }

    public void setKey(String k) {
        myKey = k;
    }

    /** Generate a 3D slice of this volume's data.  Putting it here because things like
     * windfield will probably want a different way of generating. 
     */
    public void generateSlice3D(
            VolumeSliceInput g, // All input data for creating slice
            VolumeSlice3DOutput dest, // output object, subclasses can override to add different data
            Globe gb, // Nasa globe for projection... not sure I want to directly depend on this class.
            FilterList list,
            boolean useFilters) // ArrayList<DataFilter> list /* Data filter list to use*/)
    {

        // Generates a 2D array of colors as well as a 3D index/vertex array at once.
        // Have code together to ensure consistency.
        DataValueRecord rec = getNewDataValueRecord();

        // Grid indices with elements show a 5 col, 2 row quad.  Note overlapping points, top row
        // of quads is done differently
        // 0   3   5  7   9   11
        // 1   2   4  6   8   10
        // 12  13  14  15  16  17
        // The color in this gl mode is the last point of the quad.  Since we draw counterclockwise, the color
        // for the first quad is the color at index 3, for the second 5, etc.
        int vsliceIndexCount = (g.rows) * (g.cols) * 4;  			// The overlapping quads        
        int vsliceVertexCount = 3 * (g.rows + 1) * (g.cols + 1);  // Without indices, this would be numRows*numCols*4 (slower)

        // Buffers are reused from our geometry object
        FloatBuffer vertexBuffer = dest.getNewVertexBuffer(3 * vsliceVertexCount);
        IntBuffer indexBuffer = dest.getNewIndexBuffer(vsliceIndexCount);
        FloatBuffer colorBuffer = dest.getNewColorBuffer(3 * vsliceVertexCount);

        double startHeight = g.topHeight;
        double deltaHeight = (g.topHeight - g.bottomHeight) / (1.0 * g.rows);
        double deltaLat = (g.endLat - g.startLat) / g.cols;
        double deltaLon = (g.endLon - g.startLon) / g.cols;
        ColorMapOutput data = new ColorMapOutput();

        double currentHeight = startHeight;
        double currentLat = g.startLat;
        double currentLon = g.startLon;
        boolean useTerrain = false;

        Vec4 v;

        for (int row = 0; row < g.rows; row++) {
            currentLat = g.startLat;
            currentLon = g.startLon;
            for (int col = 0; col < g.cols; col++) {

                // -----------------------------------------------------------------------------------------
                // Overlapping quad graph paper.  Think of graph paper, we only want a single point per corner of each square
                // First, decide which of the four corners we need to generate...
                boolean addTopLeft = false;
                boolean addBottomLeft = false;
                boolean addBottomRight = false;
                boolean addTopRight = false;
                if (row == 0) {
                    if (col == 0) {
                        addTopLeft = addBottomLeft = addBottomRight = addTopRight = true;
                    } else {
                        addBottomRight = addTopRight = true;
                    }
                } else {
                    if (col == 0) { // row > 0
                        addBottomLeft = addBottomRight = true;
                    } else { // col > 0, row > 0
                        addBottomRight = true;
                    }
                }

                // At row > 1, the pattern repeats.  The first point is the number of quads points in the first top row
                if (row > 1) {
                    int tl = row * (g.cols + 1) + col;
                    indexBuffer.put(tl);
                    indexBuffer.put(tl + g.cols + 1);
                    indexBuffer.put(tl + g.cols + 2);
                    indexBuffer.put(tl + 1);
                } else {
                    if (row == 0) {
                        if (col == 0) { // row = 0, col = 0
                            indexBuffer.put(0);
                            indexBuffer.put(1);
                            indexBuffer.put(2);
                            indexBuffer.put(3);
                        } else { // row = 0, col > 0  Share the left side from a previous quad...
                            int tl = 1 + (2 * col);
                            int bl = 2 * col;
                            indexBuffer.put(tl);
                            indexBuffer.put(bl);
                            indexBuffer.put(bl + 2);
                            indexBuffer.put(bl + 3);
                        }
                    } else { // row = 1, col =0
                        if (col == 0) {
                            indexBuffer.put(1);
                            indexBuffer.put(2 * (g.cols + 1));
                            indexBuffer.put(2 * (g.cols + 1) + 1);
                            indexBuffer.put(2);
                        } else { // row = 1, col > 0
                            int tl = 2 * col;
                            int bl = 2 * (g.cols + 1) + col;
                            indexBuffer.put(tl);
                            indexBuffer.put(bl);
                            indexBuffer.put(bl + 1);
                            indexBuffer.put(tl + 2);
                        }
                    }
                }

                // Only add topleft and top right points on top row, otherwise they overlap....
                if (addTopLeft) {
                    LatLon topLeft = new LatLon(Angle.fromDegrees(currentLat), Angle.fromDegrees(currentLon));
                    v = g.computePoint(gb, topLeft.getLatitude(), topLeft.getLongitude(), currentHeight,
                            useTerrain);
                    vertexBuffer.put((float) (v.x));
                    vertexBuffer.put((float) (v.y));
                    vertexBuffer.put((float) (v.z));
                    colorBuffer.position(colorBuffer.position() + 3);
                }

                if (addBottomLeft) {

                    LatLon bottomLeft = new LatLon(Angle.fromDegrees(currentLat), Angle.fromDegrees(currentLon));
                    v = g.computePoint(gb, bottomLeft.getLatitude(), bottomLeft.getLongitude(), currentHeight - deltaHeight,
                            useTerrain);
                    vertexBuffer.put((float) (v.x));
                    vertexBuffer.put((float) (v.y));
                    vertexBuffer.put((float) (v.z));
                    colorBuffer.position(colorBuffer.position() + 3);
                }

                if (addBottomRight) {
                    LatLon bottomRight = new LatLon(Angle.fromDegrees(currentLat + deltaLat), Angle.fromDegrees(currentLon + deltaLon));
                    v = g.computePoint(gb, bottomRight.getLatitude(), bottomRight.getLongitude(), currentHeight - deltaHeight,
                            useTerrain);

                    vertexBuffer.put((float) (v.x));
                    vertexBuffer.put((float) (v.y));
                    vertexBuffer.put((float) (v.z));
                    // This is the color of (row+1, col) data point
                    // The bottom right point of this quad, is the last counter clockwise point of the quad below it.
                    // Get the color in the center of the quad below us...
                    try {
                        //	   double h = currentHeight-deltaHeight-(deltaHeight/2.0);
                        getValueAt(currentLat + (deltaLat / 2.0),
                                currentLon + (deltaLon / 2.0), currentHeight - deltaHeight - (deltaHeight / 2.0),
                                data, rec, list, useFilters);
                    } catch (Exception e) {
                    }
                    // data.red = data.green = data.blue = 1.0;
                    colorBuffer.put(data.redF());
                    colorBuffer.put(data.greenF());
                    colorBuffer.put(data.blueF());
                }

                if (addTopRight) {  // Only on top row

                    LatLon topRight = new LatLon(Angle.fromDegrees(currentLat + deltaLat), Angle.fromDegrees(currentLon + deltaLon));
                    v = g.computePoint(gb, topRight.getLatitude(), topRight.getLongitude(), currentHeight,
                            useTerrain);
                    vertexBuffer.put((float) (v.x));
                    vertexBuffer.put((float) (v.y));
                    vertexBuffer.put((float) (v.z));

                    // This is the data color of (row, col)  This is the last counterclockwise point of the quad, so it's the color        			
                    try {
                        getValueAt(currentLat + (deltaLat / 2.0), currentLon + (deltaLon / 2.0),
                                currentHeight - (deltaHeight / 2.0), data, rec, list, useFilters);
                    } catch (Exception e) {
                    }
                    // data.red = data.green = data.blue = 1.0;

                    colorBuffer.put(data.redF());
                    colorBuffer.put(data.greenF());
                    colorBuffer.put(data.blueF());
                }
                currentLat += deltaLat;
                currentLon += deltaLon;
            }
            currentHeight -= deltaHeight;
        }

        indexBuffer.position(0);
        vertexBuffer.position(0);
        colorBuffer.position(0);
    }

    /** Generate a 2D slice.  Differs from 3D in info... */
    public void generate2DGrid(
            VolumeSliceInput g,
            VolumeSlice2DOutput dest,
            FilterList list,
            boolean useFilters) {
        //if (g == null){ return ; }
        if (list != null) {
            list.prepForVolume(this);
        }

        DataValueRecord rec = getNewDataValueRecord();

        // Buffers are reused from our geometry object
        int[] color2DVertices = dest.getColor2dFloatArray(3 * g.rows * g.cols);
        float[] value2DVertices = dest.getValue2dFloatArray(g.rows * g.cols);

        double startHeight = g.topHeight;
        double deltaHeight = (g.topHeight - g.bottomHeight) / (1.0 * g.rows);
        double deltaLat = (g.endLat - g.startLat) / g.cols;
        double deltaLon = (g.endLon - g.startLon) / g.cols;
        ColorMapOutput data = new ColorMapOutput();

        // Shift to 'center' for each square so we get data value at the center of the grid square
        double currentHeight = startHeight - (deltaHeight / 2.0);
        double currentLat = g.startLat + (deltaLat / 2.0);
        double currentLon = g.startLon + (deltaLon / 2.0);

        int cp2d = 0;
        int cpv2d = 0;
        boolean warning = false;
        String message = "";

        for (int row = 0; row < g.rows; row++) {
            currentLat = g.startLat;
            currentLon = g.startLon;
            for (int col = 0; col < g.cols; col++) {
                // Add color for 2D table....
                try {
                    getValueAt(currentLat, currentLon, currentHeight, data, rec, list, useFilters);
                } catch (Exception e) {
                    warning = true;
                    message = e.toString();
                    data.setColor(0, 0, 0, 255);
                    //  data.red = data.green = data.blue = 0;
                    data.filteredValue = DataType.MissingData;
                } finally {
                    color2DVertices[cp2d++] = data.redI();
                    color2DVertices[cp2d++] = data.greenI();
                    color2DVertices[cp2d++] = data.blueI();
                    value2DVertices[cpv2d++] = data.filteredValue;
                }
                currentLat += deltaLat;
                currentLon += deltaLon;
            }
            currentHeight -= deltaHeight;
        }
        if (warning) {
            System.out.println("Exception during 2D VSlice grid generation " + message);
        }
    }
}
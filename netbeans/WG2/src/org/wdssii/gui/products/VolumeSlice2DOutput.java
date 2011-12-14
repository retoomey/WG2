package org.wdssii.gui.products;

/** Output for a volume slice that is a pure 2D.  Designed for rendering 
 * 2 dimensional images such as charts.  This is not the 2D slice in the display..that
 * is actually 3D.
 * @author Robert Toomey
 * 
 */
public class VolumeSlice2DOutput {

    /** Int is wasteful.  Could compact as a single float for RGBA */
    private int[] myColor2DBuffer = null;
    private float[] myValue2DBuffer = null;
    
    /** The number of rows this was made with */
    private int rows = 0;
    
    /** The number of cols this was made with */
    private int cols = 0;
    
    public void setDimensions(int r, int c){
        rows = r;
        cols = c;
    }
    
    public int getRows(){
        return rows;
    }
    
    public int getCols(){
        return cols;
    }
    
    /** An array used for filling in colors */
    public int[] getColor2dFloatArray(int size) {
        if (myColor2DBuffer == null) {
            myColor2DBuffer = new int[size];
        }
        return myColor2DBuffer;
    }

    /** An array used for filling in data values */
    public float[] getValue2dFloatArray(int size) {
        if (myValue2DBuffer == null) {
            myValue2DBuffer = new float[size];
        }
        return myValue2DBuffer;
    }
}
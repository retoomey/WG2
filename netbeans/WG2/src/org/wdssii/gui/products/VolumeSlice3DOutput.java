package org.wdssii.gui.products;

import gov.nasa.worldwind.render.airspaces.Geometry;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;

import com.sun.opengl.util.BufferUtil;

/** Output for a volume slice.  Eventually we'll override for stuff like 3d windfield...
 * @author Robert Toomey
 * 
 */
public class VolumeSlice3DOutput {

    private Geometry fillIndexGeometry;
    private Geometry outlineIndexGeometry;
    private Geometry vertexGeometry;
    private Geometry vsliceColorGeometry;   // color vertices
    private Geometry vsliceFillIndex;       // indices for fill
    private Geometry vsliceVertexGeometry;  // position vertices
    public Geometry.CacheKey cacheKey;
    private boolean haveVSliceData = false;
    
    final int SIZE_OF_COLOR = 3; // 3 floats for a color
    final int SIZE_OF_VERTEX = 3; // xyz 3 floats for a vertex
    /** Storage for color vertices.  Only about 1/4 of these values are filled in, the
     * rest undefined since opengl lets us skip
     */
    private FloatBuffer myColorBuffer = null;  // the opengl native version.
    private FloatBuffer myVertexBuffer = null;
    private IntBuffer myIndexBuffer = null;
    private int myNumberElements = 0;

    public FloatBuffer getNewColorBuffer(int size) {
        if (myColorBuffer == null) {
            myColorBuffer = BufferUtil.newFloatBuffer(size);
        }
        myColorBuffer.position(0);
        return myColorBuffer;
    }

    public FloatBuffer getNewVertexBuffer(int size) {
        if (myVertexBuffer == null) {
            myVertexBuffer = BufferUtil.newFloatBuffer(size);
        }
        myVertexBuffer.position(0);
        return myVertexBuffer;
    }

    public IntBuffer getNewIndexBuffer(int size) {
        if (myIndexBuffer == null) {
            myIndexBuffer = BufferUtil.newIntBuffer(size);
        }
        myIndexBuffer.position(0);
        myNumberElements = size;
        return myIndexBuffer;
    }

    /** Set up a gl vertex pointer with our vertex data */
    public void setupVertexPointer(GL gl) {
        gl.glVertexPointer(SIZE_OF_VERTEX, GL.GL_FLOAT, 0, myVertexBuffer);
    }

    /** Set up a gl color pointer with our color data */
    public void setupColorPointer(GL gl) {
        gl.glColorPointer(SIZE_OF_COLOR, GL.GL_FLOAT, 0, myColorBuffer);
    }

    public void drawElementBuffer(GL gl) {
        gl.glDrawElements(GL.GL_QUADS, myNumberElements, GL.GL_UNSIGNED_INT, myIndexBuffer);
    }

    public VolumeSlice3DOutput() {
        this.fillIndexGeometry = new Geometry();
        this.outlineIndexGeometry = new Geometry();
        this.vertexGeometry = new Geometry();
        this.vsliceColorGeometry = new Geometry();
        this.vsliceVertexGeometry = new Geometry();
        this.vsliceFillIndex = new Geometry();
    }

    public Geometry getFillIndexGeometry() {
        return this.fillIndexGeometry;
    }

    public Geometry getOutlineIndexGeometry() {
        return this.outlineIndexGeometry;
    }

    public Geometry getVertexGeometry() {
        return this.vertexGeometry;
    }

    public Geometry getVSliceVertexGeometry() {
        return this.vsliceVertexGeometry;
    }

    public Geometry getVSliceFillIndexGeometry() {
        return this.vsliceFillIndex;
    }

    public void setHaveVSliceData(boolean flag){
        this.haveVSliceData = flag;
    }
    
    public boolean hasVSliceData(){
        return this.haveVSliceData;
    }
    // @Override
    public long getSizeInBytes() {
        long sizeInBytes = 0L;
        sizeInBytes += (this.fillIndexGeometry != null) ? this.fillIndexGeometry.getSizeInBytes() : 0L;
        sizeInBytes += (this.outlineIndexGeometry != null) ? this.outlineIndexGeometry.getSizeInBytes() : 0L;
        sizeInBytes += (this.vertexGeometry != null) ? this.vertexGeometry.getSizeInBytes() : 0L;
        sizeInBytes += (this.vsliceColorGeometry != null) ? this.vsliceColorGeometry.getSizeInBytes() : 0L;
        sizeInBytes += (this.vsliceVertexGeometry != null) ? this.vsliceVertexGeometry.getSizeInBytes() : 0L;
        sizeInBytes += (this.vsliceFillIndex != null) ? this.vsliceFillIndex.getSizeInBytes() : 0L;

        return sizeInBytes;
    }
}
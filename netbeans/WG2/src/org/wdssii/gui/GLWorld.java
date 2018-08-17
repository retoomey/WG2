package org.wdssii.gui;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.geom.V2;
import org.wdssii.geom.V3;
import org.wdssii.gui.renderers.PickWithOpenGLColor;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

import com.jogamp.common.nio.Buffers;

/**
 * A wrapper for rendering in an openGL Lat/Lon/Height world, passing this allows
 * hiding of what opengl world is being used to render.
 * *
 * @author Robert Toomey
 */
public abstract class GLWorld {
	private static final Logger LOG = LoggerFactory.getLogger(GLWorld.class);
    /**
     * The openGL object, required for rendering in openGL
     */
    public final GL gl;
    
    /** GL2 object */
    public final GL2 gl2;
    /**
     * The viewport width
     */
    public final int width;
    /**
     * The viewport height
     */
    public final int height;
    
    /** Are we in picking mode? Renderers can draw different for pick vs display */
	private boolean myPickingMode;
   
	/** Counter for unique pick values.  We can pick up to 16777216 objects..
	 * hopefully that's enough if coded properly. */
	private int myPickID;
	
	/** Separate byte counter for colors, avoids having to shift/multiply on the fly */
	private byte[] myPick = new byte[4];
	
	/** Every pickable object has a reference object in vector */
	private ArrayList<Object> myPicked = new ArrayList<Object>();
	
    public GLWorld(GL aGL, int w, int h) {
      gl = aGL;
      gl2 = aGL.getGL2();
      width = w;
      height = h;   
    }

    /**
     * Project from 3d to 2D in the current world.  Basic 3D model to the
     * point on the screen in 2d it hits.
     */
    public abstract V2 project(V3 a3D);

    /**
     * Project a lat, lon, height into a 3D model point...
     */
    public abstract V3 projectLLH(float latDegrees, float lonDegrees, float heightMeters);
    
    public abstract V3 projectLLH(double latDegrees, double lonDegrees, double heightMeters);
    
    /** Project a x,y,z 3D model point to lat, lon, height (reverse of projectLLH).
     * x = latitude degrees, y = longitude degrees, z = height meters.
     */
	public abstract V3 projectV3ToLLH(V3 in);
	
	/** Project from a 2D screen point to the earth's surface at a given height elevation */
	public abstract V3 project2DToEarthSurface(double x, double y, double elevation);
	 
    /** Get the elevation in meters at a given latitude, longitude location */
    public abstract float getElevation(float latDegrees, float lonDegrees);
    
     /** Get the elevation in meters at a given latitude, longitude location */
    public abstract double getElevation(double latDegrees, double lonDegrees);
    
    /** Set picking mode flag for renderers.  This resets the unique color counter and
     * picked object vector */
    public void setPickingMode() {
    	myPickingMode = true;
    	myPick[0] = myPick[1] = myPick[2] = myPick[3] = 0;
    	myPickID = 0;
        myPicked.clear();
    }
    public boolean isPickingMode() {
    	return myPickingMode;
    };

    /** Set unique gl color for rendering and return a unique ID key for object */
	public int setUniqueGLColor() {
		myPickID++;
		// separate bytes to count to avoid int to byte conversion math
		myPick[0]++;
		if (myPick[0] == 0) {
			myPick[1]++;
			if (myPick[1] == 0) {
				myPick[2]++;
				if (myPick[2] == 0) {
					myPick[3]++;
					if (myPick[3] == 0) {
						LOG.error("Too many objects for color rendering picking algorithm\n");
					}
				}
			}
		}
		LOG.error("BYTES: "+Byte.toString(myPick[0])+" "+Byte.toString(myPick[1])+" "+Byte.toString(myPick[2])+
				" "+Byte.toString(myPick[3]));	
		gl2.glColor4ub(myPick[0], myPick[1], myPick[2], myPick[3]);
		return myPickID;
	}

	public int doPickRead(GLWorld w, int x, int y) {
		// Then snag the color pixel value...
		//int fullH = (int) (world.height);
		//int yf = fullH - y - 1; // Invert Y for openGL...
		// ByteBuffer data = BufferUtil.newByteBuffer(4);
		ByteBuffer data = Buffers.newDirectByteBuffer(4);

		// gl.glDisable(GL.GL_SCISSOR_TEST); // need to restore state then right?
		gl.glReadPixels(x, y, 1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, data);

		byte d0 = data.get(0);
		byte d1 = data.get(1);
		byte d2 = data.get(2);
		//byte d3 = data.get(3);
		
		LOG.error("READ: "+Byte.toString(d0)+" "+Byte.toString(d1)+" "+Byte.toString(d2)+
				" ");
		System.out.println(String.format("0x%02X", d0));
		System.out.println(String.format("0x%02X", d1));
		System.out.println(String.format("0x%02X", d2));
		//System.out.println(String.format("0x%02X", d3));
		

		//int id = (d3 << 24)+(d2 << 16)+(d1 << 8) + d0;
		int id = (d2 << 16)+(d1 << 8) + d0;

		LOG.error("INT: "+Integer.toString(id));
		return id;  // Note zero means no hit
	}
	
    /** Get the multiplier in the vertical direction */
    public abstract double getVerticalExaggeration();
    
    /** Is this point in the view frustrum? Can be used for software clipping */
    public abstract boolean inView(V3 a3D);
    
    /** Redraw the scene because we moved or changed something.  Handle as needed */
    public abstract void redraw();
}

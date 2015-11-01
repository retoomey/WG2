package org.wdssii.gui;

import javax.media.opengl.GL;
import org.wdssii.geom.V2;
import org.wdssii.geom.V3;

/**
 * A wrapper for rendering in an openGL Lat/Lon/Height world, passing this allows
 * hiding of what opengl world is being used to render.
 * *
 * @author Robert Toomey
 */
public abstract class GLWorld {

    /**
     * The openGL object, required for rendering in openGL
     */
    public final GL gl;
    /**
     * The viewport width
     */
    public final int width;
    /**
     * The viewport height
     */
    public final int height;
   
    public GLWorld(GL aGL, int w, int h) {
      gl = aGL;
      width = w;
      height = h;
    }

    /**
     * Project from 3d to 2D in the current world
     */
    public abstract V2 project(V3 a3D);

    /**
     * Project a lat, lon, height into a 3D model point...
     */
    public abstract V3 projectLLH(float latDegrees, float lonDegrees, float heightMeters);
    
    public abstract V3 projectLLH(double latDegrees, double lonDegrees, double heightMeters);
    
    /** Get the elevation in meters at a given latitude, longitude location */
    public abstract float getElevation(float latDegrees, float lonDegrees);
    
     /** Get the elevation in meters at a given latitude, longitude location */
    public abstract double getElevation(double latDegrees, double lonDegrees);
    
    /** Are we picking?  Think this can go away */
    public abstract boolean isPickingMode();
    
    /** Get the multiplier in the vertical direction */
    public abstract double getVerticalExaggeration();
    
    /** Is this point in the view frustrum? Can be used for software clipping */
    public abstract boolean inView(V3 a3D);
}

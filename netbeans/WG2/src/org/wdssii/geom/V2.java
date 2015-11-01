package org.wdssii.geom;

/**
 *
 * @author Robert Toomey
 */
public class V2 {

    public final float x;
    public final float y;

    public V2(float xi, float yi) {
        x = xi;
        y = yi;
    }

    public V2(double xi, double yi) {
        x = (float) xi;
        y = (float) yi;
    }
    
    public V2 offset(float dx, float dy){
    	return new V2(x+dx, y+dy);
    }
    
    public V2 offset(double dx, double dy){
    	return new V2(x+dx, y+dy);
    }
}

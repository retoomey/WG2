package org.wdssii.geom;

/**
 * LLD that holds information for the polygon hack
 * A special object to hold all the point dragging stuff for polygons/vslice, ec.
 * 
 * @author Robert Toomey
 */
public class LLD_X { //extends LLD {

	private int polygon=1;
	private String note;
	private boolean selected = false;
	public  float x;
	public  float y;

	public LLD_X(LLD_X i){
		//super(i.x, i.y);
		x = i.x;
		y = i.y;
		polygon=i.polygon;
		note=i.note;
		selected = i.selected;
	}

	public LLD_X(float lat, float lon) {
		x = lat;
		y = lon;
		//super(lat, lon);
	}

	public LLD_X(double lat, double lon) {
		//super(lat, lon);
		x = (float)lat;
		y = (float)lon;
	}

	// Because superclass is immutable...
	public LLD_X(float lat, float lon, LLD_X old) {
		//super(lat, lon);
		x = lat;
		y = lon;
		polygon=old.polygon;
		note=old.note;
		selected = old.selected;
	}

	public LLD_X(double lat, double lon, LLD_X old) {
		//super(lat, lon);
		x = (float)lat;
		y = (float)lon;
		polygon=old.polygon;
		note=old.note;
		selected = old.selected;
	}

	public int getPolygon(){
		return polygon;
	}

	public void setPolygon(int p){
		polygon = p;
	}

	public String getNote(){
		return note;
	}

	public void setNote(String s){
		note = s;
	}

	public void setSelected(boolean f){
		selected = f;
	}
	public boolean getSelected(){
		return selected;

	}
	
    public float latDegrees(){
        return x;
    }
    
    public float lonDegrees(){
        return y;
    }
}

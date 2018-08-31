package org.wdssii.geom;

/**
 * A special object to hold all the point dragging stuff for polygons/vslice,
 * etc.
 * 
 * @author Robert Toomey
 */
public class LLD_X extends V2 {

	private int polygon = 1;
	private String note = "";
	private boolean selected = false;

	public LLD_X(LLD_X i) {
		super(i.x, i.y);
		polygon = i.polygon;
		note = i.note;
		selected = i.selected;
	}

	public LLD_X(double lat, double lon) {
		super(lat, lon);
	}

	public LLD_X(double lat, double lon, LLD_X old) {
		super(lat, lon);
		polygon = old.polygon;
		note = old.note;
		selected = old.selected;
	}

	public int getPolygon() {
		return polygon;
	}

	public void setPolygon(int p) {
		polygon = p;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String s) {
		note = s;
	}

	public void setSelected(boolean f) {
		selected = f;
	}

	public boolean getSelected() {
		return selected;

	}
}

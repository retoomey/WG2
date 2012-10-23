package org.wdssii.gui.volumes;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.render.markers.MarkerAttributes;
import javax.media.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicLLHAreaControlPoint implements LLHAreaControlPoint {

	private static Logger log = LoggerFactory.getLogger(BasicLLHAreaControlPoint.class);

	public static class CircleMarker extends BasicMarker {

		public CircleMarker(Position pstn, MarkerAttributes ma) {
			super(pstn, ma);
		}

		public CircleMarker(Position pstn, MarkerAttributes ma, Angle angle) {
			super(pstn, ma, angle);
		}

		public void billboard(DrawContext dc, Vec4 point) {
			// Get 2D on the screen..we're making a fixed shape...
			View v = dc.getView();
			Vec4 p = v.project(point);
			GL gl = dc.getGL();
			//	gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			int z = 10;
			// Standard ortho projection
			int aViewWidth = v.getViewport().width;
			int aViewHeight = v.getViewport().height;

                        gl.glDisable(GL.GL_LIGHTING);
			gl.glDisable(GL.GL_DEPTH_TEST);
			gl.glDisable(GL.GL_TEXTURE_2D); // no textures
			gl.glMatrixMode(GL.GL_PROJECTION);
			gl.glPushMatrix();
			gl.glLoadIdentity();
			gl.glOrtho(0, aViewWidth, 0, aViewHeight, -1, 1);  // TopLeft
			gl.glMatrixMode(GL.GL_MODELVIEW);
			gl.glPushMatrix();
			gl.glLoadIdentity();

			if (dc.isPickingMode()) {
				gl.glBegin(GL.GL_QUADS);
				gl.glVertex2d(p.x - z, p.y - z);
				gl.glVertex2d(p.x + z, p.y - z);
				gl.glVertex2d(p.x + z, p.y + z);
				gl.glVertex2d(p.x - z, p.y + z);
				gl.glEnd();
			} else {
                            
                                gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
				gl.glBegin(GL.GL_QUADS);
				gl.glVertex2d(p.x - z, p.y - z);
				gl.glVertex2d(p.x + z, p.y - z);
				gl.glVertex2d(p.x + z, p.y + z);
				gl.glVertex2d(p.x - z, p.y + z);
				gl.glEnd();
                                z--;
                                gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
				gl.glBegin(GL.GL_QUADS);
				gl.glVertex2d(p.x - z, p.y - z);
				gl.glVertex2d(p.x + z, p.y - z);
				gl.glVertex2d(p.x + z, p.y + z);
				gl.glVertex2d(p.x - z, p.y + z);
				gl.glEnd();
                                z--;
				gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
				gl.glBegin(GL.GL_QUADS);
				gl.glVertex2d(p.x - z, p.y - z);
				gl.glVertex2d(p.x + z, p.y - z);
				gl.glVertex2d(p.x + z, p.y + z);
				gl.glVertex2d(p.x - z, p.y + z);
				gl.glEnd();
                                
				/*gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
				gl.glBegin(GL.GL_LINE_LOOP);
				gl.glVertex2d(p.x - z, p.y - z);
				gl.glVertex2d(p.x + z, p.y - z);
				gl.glVertex2d(p.x + z, p.y + z);
				gl.glVertex2d(p.x - z, p.y + z);
				gl.glEnd();
                                
                                z--;
                                gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
				gl.glBegin(GL.GL_LINE_LOOP);
				gl.glVertex2d(p.x - z, p.y - z);
				gl.glVertex2d(p.x + z, p.y - z);
				gl.glVertex2d(p.x + z, p.y + z);
				gl.glVertex2d(p.x - z, p.y + z);
				gl.glEnd();
                                * */
			}

			gl.glMatrixMode(GL.GL_PROJECTION);
			gl.glPopMatrix();
			gl.glMatrixMode(GL.GL_MODELVIEW);
			gl.glPopMatrix();
		}

		@Override
		public void render(DrawContext dc, Vec4 point, double radius, boolean isRelative) {
		//	this.attributes.getShape(dc).render(dc, this, point, radius, isRelative);
		}

		@Override
		public void render(DrawContext dc, Vec4 point, double radius) {
		//	this.attributes.getShape(dc).render(dc, this, point, radius, false);
			billboard(dc, point);
		}
	}

	public static class NULLMarker extends BasicMarker {

		public NULLMarker(Position pstn, MarkerAttributes ma) {
			super(pstn, ma);
		}

		public NULLMarker(Position pstn, MarkerAttributes ma, Angle angle) {
			super(pstn, ma, angle);
		}

		@Override
		public void render(DrawContext dc, Vec4 point, double radius, boolean isRelative) {
		}

		@Override
		public void render(DrawContext dc, Vec4 point, double radius) {
		}
	}
	/** Default bottom marker used if one has not been set */
	public static Marker sharedBottomMarker;
	/** Default top marker used if one has not been set */
	public static Marker sharedTopMarker;

	@Override
	public Marker getControlPointMarker() {
		Marker m;
		// Bottom control...not sure I need it, we can disable just having our null marker, that was the control point is still
		// there if I want to put it back later...
		if (altitudeIndex == 0) {
			if (sharedBottomMarker == null) {
				MarkerAttributes attributes = new BasicMarkerAttributes(Material.BLUE, BasicMarkerShape.SPHERE, 1.0, 16, 16);
				sharedBottomMarker = new CircleMarker(null, attributes, null);
			}
			m = sharedBottomMarker;
		} else {
			if (sharedBottomMarker == null) {
				MarkerAttributes attributes = new BasicMarkerAttributes(Material.BLUE, BasicMarkerShape.SPHERE, 1.0, 16, 16);
				sharedTopMarker = new NULLMarker(null, attributes, null);
			}
			m = sharedTopMarker;
		}
		return m;
	}

	public static class BasicControlPointKey {

		private int locationIndex;
		private int altitudeIndex;

		public BasicControlPointKey(int locationIndex, int altitudeIndex) {
			this.locationIndex = locationIndex;
			this.altitudeIndex = altitudeIndex;
		}

		public int getLocationIndex() {
			return this.locationIndex;
		}

		public int getAltitudeIndex() {
			return this.altitudeIndex;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || this.getClass() != o.getClass()) {
				return false;
			}

			BasicControlPointKey that = (BasicControlPointKey) o;
			return (this.locationIndex == that.locationIndex) && (this.altitudeIndex == that.altitudeIndex);
		}

		@Override
		public int hashCode() {
			int result = this.locationIndex;
			result = 31 * result + this.altitudeIndex;
			return result;
		}
	}
	private LLHArea airspace;
	private int locationIndex;
	private int altitudeIndex;
	private Vec4 point;

	public BasicLLHAreaControlPoint(LLHArea airspace, int locationIndex, int altitudeIndex,
		Vec4 point) {
		this.airspace = airspace;
		this.locationIndex = locationIndex;
		this.altitudeIndex = altitudeIndex;
		this.point = point;
	}

	public BasicLLHAreaControlPoint(LLHArea airspace, Vec4 point) {
		this(airspace, -1, -1, point);
	}

	@Override
	public LLHArea getAirspace() {
		return this.airspace;
	}

	@Override
	public int getLocationIndex() {
		return this.locationIndex;
	}

	@Override
	public int getAltitudeIndex() {
		return this.altitudeIndex;
	}

	@Override
	public Vec4 getPoint() {
		return this.point;
	}

	@Override
	public Object getKey() {
		return keyFor(this.locationIndex, this.altitudeIndex);
	}

	public static Object keyFor(int locationIndex, int altitudeIndex) {
		return new BasicControlPointKey(locationIndex, altitudeIndex);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || this.getClass() != o.getClass()) {
			return false;
		}

		BasicLLHAreaControlPoint that = (BasicLLHAreaControlPoint) o;

		if (this.airspace != that.airspace) {
			return false;
		}
		if (this.altitudeIndex != that.altitudeIndex) {
			return false;
		}
		if (this.locationIndex != that.locationIndex) {
			return false;
		}
		//noinspection RedundantIfStatement
		if (this.point != null ? !this.point.equals(that.point) : that.point != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = 31 * result + (this.airspace != null ? this.airspace.hashCode() : 0);
		result = 31 * result + this.locationIndex;
		result = 31 * result + this.altitudeIndex;
		result = 31 * result + (this.point != null ? this.point.hashCode() : 0);
		return result;
	}
}

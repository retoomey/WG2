package org.wdssii.gui.volumes;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import java.util.Arrays;
import java.util.List;
import javax.media.opengl.GL;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.FeatureCommand;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.worldwind.WorldwindUtil;

/**
 * A 'Pin' or stick in the display.  An LLHArea with a single location
 * 
 * @author Robert Toomey
 */
public class LLHAreaHeightStick extends LLHArea {

	private String lastDrawnKey = "";

	public LLHAreaHeightStick(LLHAreaFeature f) {
		super(f);
	}

	/** Get a key that represents the GIS location of this stick.
	FIXME move up to LLHArea...*/
	public String getGISKey() {
		String newKey = "";

		// Add location and altitude...
		List<LatLon> locations = getLocationList();
		for (int i = 0; i < locations.size(); i++) {
			LatLon l = locations.get(i);
			newKey = newKey + l.getLatitude() + ":";
			newKey = newKey + l.getLongitude() + ":";
		}
		newKey = newKey + this.lowerAltitude;
		newKey = newKey + this.upperAltitude;
		return newKey;
	}

	@Override
	protected void doRenderGeometry(DrawContext dc, String drawStyle, List<LatLon> locations, List<Boolean> edgeFlags) {
		if (locations.isEmpty()) {
			return;
		}
		double[] altitudes = this.getAltitudes();
		List<LatLon> list = this.getLocations();

		GL gl = dc.getGL();

		if (drawStyle.equals("fill")) {

			Globe globe = dc.getGlobe();
			double vert = dc.getVerticalExaggeration();

			LatLon item = list.get(0);
			Vec4 bot = globe.computePointFromPosition(item.latitude, item.longitude, altitudes[0] * vert);
			Vec4 top = globe.computePointFromPosition(item.latitude, item.longitude, altitudes[1] * vert);
			// Shouldn't this code be in the renderer???
			gl.glPushAttrib(GL.GL_LINE_BIT | GL.GL_LIGHTING_BIT);
			gl.glDisable(GL.GL_LIGHTING);
			gl.glLineWidth(5);
			if (!dc.isPickingMode()) { // Pick mode uses a unique color to pick
				gl.glColor4f(1.0f, 1.0f, 1.0f, .20f);
			}

			gl.glBegin(GL.GL_LINES);
			gl.glVertex3d(bot.x, bot.y, bot.z);
			gl.glVertex3d(top.x, top.y, top.z);
			gl.glEnd();

			gl.glPopAttrib();

			// Check for change in GIS and fire event....probably a better place for this somewhere...
			String key = getGISKey();
			if (key.compareTo(lastDrawnKey) != 0) {
				lastDrawnKey = key;
				// Fire changed event?  Is this enough? 
				CommandManager.getInstance().executeCommand(new FeatureCommand(), true);
			}
		}
	}

	/** The default location for a newly created LLHArea */
	@Override
	protected List<LatLon> getDefaultLocations(WorldWindow wwd) {
		// Taken from worldwind...we'll need to figure out how we want the vslice/isosurface to work...
		Position position = WorldwindUtil.getNewShapePosition(wwd);
		Angle heading = WorldwindUtil.getNewShapeHeading(wwd, true);

		/** Create based on viewport. */
		Globe globe = wwd.getModel().getGlobe();
		Matrix transform = Matrix.IDENTITY;
		transform = transform.multiply(globe.computeModelCoordinateOriginTransform(position));
		transform = transform.multiply(Matrix.fromRotationZ(heading.multiply(-1)));
		double sizeInMeters = DEFAULT_LENGTH_METERS;
		double widthOver2 = sizeInMeters / 2.0;
		double heightOver2 = sizeInMeters / 2.0;

		Vec4[] points = new Vec4[]{
			//new Vec4(-widthOver2, -heightOver2, 0.0).transformBy4(transform), // lower left (as if looking down, to sw)
			new Vec4(0.0, 0.0, 0.0).transformBy4(transform), // lower left (as if looking down, to sw)
		//    new Vec4(widthOver2, -heightOver2, 0.0).transformBy4(transform), // lower right
		//    new Vec4(widthOver2, heightOver2, 0.0).transformBy4(transform), // upper right
		//new Vec4(-widthOver2,  heightOver2, 0.0).transformBy4(transform)  // upper left
		};

		/** Convert from vector model coordinates to LatLon */
		LatLon[] locations = new LatLon[points.length];
		for (int i = 0; i < locations.length; i++) {
			locations[i] = new LatLon(globe.computePositionFromPoint(points[i]));
		}
		return Arrays.asList(locations);
	}
}

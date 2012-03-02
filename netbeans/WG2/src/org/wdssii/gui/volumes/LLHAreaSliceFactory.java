package org.wdssii.gui.volumes;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.examples.util.ShapeUtils;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import org.wdssii.gui.features.Feature.FeatureTableInfo;
import org.wdssii.gui.features.FeatureGUI;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.worldwind.WorldwindUtil;

/** Factory which creates a 'slice'.  Two lat/lon points and a fixed height range between them*/
public class LLHAreaSliceFactory extends LLHAreaFactory {

    /** Counter for default name */
    static int counter = 1;

    @Override
    public String getFactoryNameDisplay() {
        return "Slice";
    }

    @Override
    public boolean create(WorldWindow wwd, LLHAreaFeature f, FeatureTableInfo data) {

        boolean success = true;

        // Create the visible object in world window
        String name = "Slice" + String.valueOf(counter++);
        
        data.visibleName = name;
        data.keyName = name;
        data.visible = true;

        LLHAreaSlice poly = new LLHAreaSlice(f);       
        poly.setAttributes(getDefaultAttributes());
        poly.setValue(AVKey.DISPLAY_NAME, name);
        poly.setAltitudes(0.0, 0.0);
        data.created = poly;
        initializePolygon(wwd, poly, false);

        setName(name);
        return success;
    }
    
    @Override
    public FeatureGUI createGUI(LLHArea a, JComponent parent){
        if (a instanceof LLHAreaSlice){
            LLHAreaSlice slice = (LLHAreaSlice)(a);
            LLHAreaSliceGUI gui = new LLHAreaSliceGUI(slice);          
            return gui;
        }
        return super.createGUI(a, parent);
    }

    /** Initialize a new polygon (VSlice) FIXME: should be factory method */
    protected void initializePolygon(WorldWindow wwd, LLHAreaSlice polygon, boolean fitShapeToViewport) {

        // Taken from worldwind...we'll need to figure out how we want the vslice/isosurface to work...
        Position position = WorldwindUtil.getNewShapePosition(wwd);
        Angle heading = WorldwindUtil.getNewShapeHeading(wwd, true);
        double sizeInMeters = fitShapeToViewport
                ? ShapeUtils.getViewportScaleFactor(wwd) : DEFAULT_SHAPE_SIZE_METERS;
        java.util.List<LatLon> locations = createSliceInViewport(wwd, position, heading, sizeInMeters);

        double maxElevation = DEFAULT_HEIGHT_METERS;
        polygon.setAltitudes(0.0,  maxElevation);
        
        polygon.setLocations(locations);
    }

    public static List<LatLon> createSliceInViewport(WorldWindow wwd, Position position, Angle heading,
            double sizeInMeters) {
        
        /** Create based on viewport. */
        Globe globe = wwd.getModel().getGlobe();
        Matrix transform = Matrix.IDENTITY;
        transform = transform.multiply(globe.computeModelCoordinateOriginTransform(position));
        transform = transform.multiply(Matrix.fromRotationZ(heading.multiply(-1)));
        double widthOver2 = sizeInMeters / 2.0;
        double heightOver2 = sizeInMeters / 2.0;
        
        Vec4[] points = new Vec4[]{
            new Vec4(-widthOver2, -heightOver2, 0.0).transformBy4(transform), // lower left (as if looking down, to sw)
    // new Vec4(widthOver2,  -heightOver2, 0.0).transformBy4(transform), // lower right
            new Vec4(widthOver2, heightOver2, 0.0).transformBy4(transform), // upper right
    // new Vec4(-widthOver2,  heightOver2, 0.0).transformBy4(transform)  // upper left
        };

        /** Convert from vector model coordinates to LatLon */
        LatLon[] locations = new LatLon[points.length];
        for (int i = 0; i < locations.length; i++) {
            locations[i] = new LatLon(globe.computePositionFromPoint(points[i]));
        }

        return Arrays.asList(locations);
    }
}
package org.wdssii.gui.volumes;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.examples.util.ShapeUtils;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import java.util.Arrays;
import java.util.List;
import org.wdssii.gui.features.Feature.FeatureTableInfo;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.worldwind.WorldwindUtil;

/** Factory which creates a box for rendering purposes.  This is used for the isosurface
 * @author Robert Toomey
 *
 */
public class LLHAreaBoxFactory extends LLHAreaFactory {

    /** Counter for default name */
    static int counter = 1;

    @Override
    public String getFactoryNameDisplay() {
        return "Box";
    }

    @Override
    public boolean create(WorldWindow wwd, LLHAreaFeature f, FeatureTableInfo data) {

        boolean success = true;

        // Create the visible object in world window
        String name = getFactoryNameDisplay() + String.valueOf(counter++);
        data.visibleName = name;
        data.keyName = name;
        data.visible = true;

        LLHArea poly = new LLHArea(f);
        poly.setAttributes(getDefaultAttributes());
        poly.setValue(AVKey.DISPLAY_NAME, name);
        poly.setAltitudes(0.0,  LLHArea.DEFAULT_HEIGHT_METERS);
        poly.setLocations(poly.getDefaultLocations(wwd));
        data.created = poly;

        setName(name);
        return success;
    }
}
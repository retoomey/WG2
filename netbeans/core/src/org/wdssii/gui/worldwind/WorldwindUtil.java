package org.wdssii.gui.worldwind;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.orbit.OrbitView;

/**
 *
 * @author Robert Toomey
 */
public class WorldwindUtil {

    public static Position getNewShapePosition(WorldWindow wwd) {

        Position p = new Position(Angle.fromDegreesLatitude(35.2225d), // Norman
                Angle.fromDegreesLongitude(-97.4391667d), 10.0d * 1000.0d);
        return p;

        /*  Terrain is null here, why?
        SceneController scene =  wwd.getSceneController();
        SectorGeometryList goop = scene.getTerrain();
        
        
        Line ray = new Line(wwd.getView().getEyePoint(), wwd.getView().getForwardVector());
        SceneController fred = wwd.getSceneController();
        SectorGeometryList terrain = fred.getTerrain();
        
        Intersection[] intersection = terrain.intersect(ray);
        
        if (intersection != null && intersection.length != 0)
        {
        return wwd.getModel().getGlobe().computePositionFromPoint(intersection[0].getIntersectionPoint());
        }
        else if (wwd.getView() instanceof OrbitView)
        {
        return ((OrbitView) wwd.getView()).getCenterPosition();
        }
        
        return Position.ZERO;
         */
    }

    public static Angle getNewShapeHeading(WorldWindow wwd, boolean matchViewHeading) {
        if (matchViewHeading) {
            if (wwd.getView() instanceof OrbitView) {
                return ((OrbitView) wwd.getView()).getHeading();
            }
        }

        return Angle.ZERO;
    }
}

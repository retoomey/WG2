package org.wdssii.gui.volumes;

import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.markers.Marker;

public interface LLHAreaControlPoint {

    LLHArea getAirspace();

    int getLocationIndex();

    int getAltitudeIndex();

    Vec4 getPoint();

    Object getKey();

    /** Let each point have a marker (possibly shared) */
    Marker getControlPointMarker();
    
}
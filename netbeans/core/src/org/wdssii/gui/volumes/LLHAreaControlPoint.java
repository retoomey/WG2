package org.wdssii.gui.volumes;

import gov.nasa.worldwind.geom.Vec4;

public interface LLHAreaControlPoint {

    LLHArea getAirspace();

    int getLocationIndex();

    int getAltitudeIndex();

    Vec4 getPoint();

    Object getKey();
}
package org.wdssii.gui.commands;

import gov.nasa.worldwind.geom.LatLon;
import java.util.ArrayList;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.volumes.LLHAreaSet;

/**
 *
 * @author Robert Toomey
 */
public class PointAddCommand extends FeatureChangeCommand {

    public LLHAreaSet myPointSet;
    public FeatureMemento myNewMemento;
    
    public PointAddCommand(LLHAreaSet area, LatLon newLocation, int index){
        super();
        
        // Create a list copy without the point
        FeatureMemento m = area.getMemento(); // vs getNewMemento as in gui control...hummm
        @SuppressWarnings("unchecked")
        ArrayList<LatLon> list = ((ArrayList<LatLon>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
        if (list != null) {
             FeatureMemento fm = (FeatureMemento) (m); // Check it
             list.add(index, newLocation);
             myNewMemento = fm; 
             myPointSet = area;
        }
    }
    
    @Override
    public boolean execute() {
        if (myPointSet != null){
            set(myPointSet.getFeature().getKey(), myNewMemento);
            return super.execute();
        }
        return false;
    }
}

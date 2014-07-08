package org.wdssii.gui.commands;

import java.util.ArrayList;
import org.wdssii.geom.LLD;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.volumes.LLHAreaSet;

/**
 *
 * @author Robert Toomey
 */
public class PointAddCommand extends FeatureChangeCommand {

    public LLHAreaSet myPointSet;
    public FeatureMemento myNewMemento;
    
    public PointAddCommand(LLHAreaSet area, LLD newLocation, int index){
        super();
        
        // Create a list copy without the point
        FeatureMemento m = area.getMemento(); // vs getNewMemento as in gui control...hummm
        @SuppressWarnings("unchecked")
        ArrayList<LLD> list = ((ArrayList<LLD>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
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

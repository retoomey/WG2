package org.wdssii.gui.commands;

import java.util.ArrayList;
import org.wdssii.geom.LLD;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.volumes.LLHAreaSet;

/**
 * Delete point number index (0 based), or all if index = -1;
 *
 * @author Robert Toomey
 */
public class PointRemoveCommand extends FeatureChangeCommand {

    public LLHAreaSet myPointSet;
    public FeatureMemento myNewMemento;

    public PointRemoveCommand(LLHAreaSet area, int index) {
        super();

        // Create a list copy without the point
        FeatureMemento m = area.getMemento(); // vs getNewMemento as in gui control...hummm
        @SuppressWarnings("unchecked")
        ArrayList<LLD> list = ((ArrayList<LLD>) m.getPropertyValue(LLHAreaSet.LLHAreaSetMemento.POINTS));
        if (list != null) {
            FeatureMemento fm = (FeatureMemento) (m); // Check it
            if (index == -1) {
                m.setProperty(LLHAreaSet.LLHAreaSetMemento.POINTS, new ArrayList<LLD>());
            } else {
                list.remove(index);
            }
            myNewMemento = fm;
            myPointSet = area;
        }
    }

    @Override
    public boolean execute() {
        if (myPointSet != null) {
            set(myPointSet.getFeature().getKey(), myNewMemento);
            return super.execute();
        }
        return false;
    }
}

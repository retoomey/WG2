package org.wdssii.gui.commands;

import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureList.FeatureFilter;

/**
 * FeatureDeleteCommand deletes one of the objects in a FeatureList
 *
 * @author Robert Toomey
 */
public class FeatureDeleteCommand extends FeatureCommand {

    public static class FeatureDeleteAllCommand extends FeatureDeleteCommand {

        @Override
        public boolean execute() {
            return (deleteAllFeatures());
        }
    }
    private String myFeatureKey = null;

    public FeatureDeleteCommand() {
    }

    public FeatureDeleteCommand(String key) {
        myFeatureKey = key;
    }

    public static class DeleteAllFilter implements FeatureFilter {

        @Override
        public boolean matches(Feature f) {
           return (f.getDeletable());
        }
    }

    /**
     * Delete all features from display
     */
    protected boolean deleteAllFeatures() {
        FeatureList.getFeatureList().removeFeatures(new DeleteAllFilter());
        return true;
    }

    @Override
    public boolean execute() {
        FeatureList.getFeatureList().removeFeature(myFeatureKey);
        FeatureList.getFeatureList().updateOnMinTime();
        return true;
    }
}

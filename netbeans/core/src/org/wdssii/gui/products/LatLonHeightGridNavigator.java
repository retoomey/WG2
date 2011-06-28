package org.wdssii.gui.products;

import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.LatLonHeightGrid;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.CommandManager.NavigationMessage;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMoveNextTime;
import org.wdssii.gui.commands.ProductMoveCommand.ProductMovePreviousTime;
import org.wdssii.gui.commands.WdssiiCommand;

/**
 *  Handles navigating within the elevations of a LatLonHeightGrid.
 * These are different because there's no record changing involved unless
 * the time changes.
 * @author Robert Toomey
 */
public class LatLonHeightGridNavigator extends ProductNavigator {

    /** The current elevation subindex we are at.  Note that every
    ProductHandler will have a unique navigator, even if you have two
    that are looking at the same product*/
    protected int myCurrentH = 0;

    public static String format(float elev, int i, int total){
        float kms = elev/1000.0f;
        return String.format("%5.1f KM", kms);
        
    }
    
    @Override
    public WdssiiCommand getNextSubtypeCommand() {
        return new LatProductMoveUp();
    }

    @Override
    public WdssiiCommand getPreviousSubtypeCommand() {
        return new LatProductMoveDown();
    }

    // Experimental
    public class LatProductMoveUp extends ProductMoveNextTime {

        @Override
        public boolean execute() {
            // Going up keeps time the same...unless we're at the 'top',
            // then we roll forward to next product...
            Product p = getOurProduct();
            if (p != null) {
                DataType dt = p.getRawDataType();
                if (dt != null) {
                    if (dt instanceof LatLonHeightGrid) {
                        LatLonHeightGrid g = (LatLonHeightGrid) (dt);
                        int total = g.getNumHeights();
                        myCurrentH++;
                        if (myCurrentH >= total) {
                            myCurrentH = 0;
                            // flip to next product time
                            CommandManager.getInstance().getProductOrderedSet().navigate(
                                    NavigationMessage.NextTime);
                        }
                    }
                }
            }
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            ProductButtonStatus status = new ProductButtonStatus();
            Product p = getOurProduct();
            if (p != null) {
                DataType dt = p.getRawDataType();
                if (dt != null) {
                    if (dt instanceof LatLonHeightGrid) {
                        LatLonHeightGrid g = (LatLonHeightGrid) (dt);
                        int total = g.getNumHeights();
                        int next = myCurrentH + 1;
                        // This is in the next time...
                        if ((next >= total) || (total < 1)) {
                            return super.getButtonStatus();
                        } else {
                            if (next < total) {
                                float stuff = g.getHeight(next);
                                status.setButtonText(format(stuff, next, total));
                            }
                        }
                    }
                }
            }
            return status;
        }
    }

    public class LatProductMoveDown extends ProductMovePreviousTime {

        @Override
        public boolean execute() {
            // Going up keeps time the same...unless we're at the 'top',
            // then we roll forward to next product...
            Product p = getOurProduct();
            if (p != null) {
                DataType dt = p.getRawDataType();
                if (dt != null) {
                    if (dt instanceof LatLonHeightGrid) {
                        LatLonHeightGrid g = (LatLonHeightGrid) (dt);
                        int total = g.getNumHeights();
                        myCurrentH--;
                        if (myCurrentH < 0) {
                            myCurrentH = total - 1;
                            // flip to next product time
                            CommandManager.getInstance().getProductOrderedSet().navigate(
                                    NavigationMessage.PreviousTime);
                        }
                    }
                }
            }
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            ProductButtonStatus status = new ProductButtonStatus();
            Product p = getOurProduct();
            if (p != null) {
                DataType dt = p.getRawDataType();
                if (dt != null) {
                    if (dt instanceof LatLonHeightGrid) {
                        LatLonHeightGrid g = (LatLonHeightGrid) (dt);
                        int total = g.getNumHeights();
                        int prev = myCurrentH - 1;
                        // This is in the previous time...
                        if ((prev < 0) || (total < 1)) {
                            return super.getButtonStatus();
                        } else {
                            if (prev < total) {
                                float stuff = g.getHeight(prev);
                                status.setButtonText(format(stuff, prev, total));
                            }
                        }
                    }
                }
            }
            return status;
        }
    }
}

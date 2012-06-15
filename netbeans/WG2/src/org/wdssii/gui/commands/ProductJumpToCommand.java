package org.wdssii.gui.commands;

import org.wdssii.geom.Location;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductButtonStatus;
import org.wdssii.gui.views.WorldWindView;

/** Command called by the 'jump' button in the navigator 
 * 
 * @author Robert Toomey
 *
 */
public class ProductJumpToCommand extends ProductCommand {

    @Override
    public boolean execute() {
        Product p = getOurProduct();
        if (p != null) {
            Location loc = p.getBaseLocation();
            if (myTargetListener != null) {
                if (myTargetListener instanceof WorldWindView) {
                    WorldWindView e = (WorldWindView) (myTargetListener);
                    e.gotoLocation(loc);
                }
            }
        }
        return false;
    }

    @Override
    public ProductButtonStatus getButtonStatus() {

        Product p = getOurProduct();
        String label, tip, icon;
        label = "None";
        tip = "";
        icon = "";
        boolean enabled = true;

        ProductButtonStatus status = new ProductButtonStatus();
        Location base = null;
        if (p != null) {
            base = p.getBaseLocation();
        }
        if (base == null) {
            label = "???";
            tip = "Unknown geographic center of this data.";
            enabled = false;
        } else {
            label = "Jump";
            tip = "Jump to location of data at " + p.getBaseLocation();
            enabled = true;
        }
        status.setButtonText(label);
        status.setToolTip(tip);
        status.setIconString(ProductMoveCommand.BLANK_FILL_ICON);
        status.setEnabled(enabled);

        return status;
    }
}

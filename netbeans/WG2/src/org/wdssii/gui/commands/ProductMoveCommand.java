package org.wdssii.gui.commands;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.Product.Navigation;
import org.wdssii.core.ProductButtonStatus;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.index.IndexRecord;

/** A product change caused by a navigation move 
 * We stick the basic moves inside this class for now (they are static can be pulled out)
 * The command also does the basic button formatting for products (The stuff displayed
 * in the button affects where the button moves)
 * 
 * @author Robert Toomey
 *
 */
public class ProductMoveCommand extends ProductCommand {

    // Default button formatter
    private static final SimpleDateFormat buttonFormat = new SimpleDateFormat(
            "HH:mm:ss");
    // Tool tip formatter. FIXME: from product?
    private static final SimpleDateFormat tipFormat = new SimpleDateFormat(
            "---HH:mm:ss");
    // Default icons for navigation directions, these are passed to the
    // IconFactory
    public static final String RIGHT_ARROW_ICON = "RightArrowIcon";
    public static final String RIGHT_ARROW_END_ICON = "RightArrowEndIcon";
    public static final String DOWN_ARROW_ICON = "DownArrowIcon";
    public static final String UP_ARROW_ICON = "UpArrowIcon";
    public static final String LEFT_ARROW_ICON = "LeftArrowIcon";
    public static final String PREV_NOT_VIRTUAL_ICON = "LeftArrowEndIcon";
    public static final String BLANK_FILL_ICON = "PadIcon";

    @Override
    public boolean execute() {
        return false;
    }

    public static class ProductMovePreviousLowestSubType extends ProductMoveCommand {

        // FIXME:  Humm...notice two completely different paths for
        // getting the record to navigate to.  Needs a redesign I think
        // The navigate() code is not the same at the getProductVolume
        @Override
        public boolean execute() {

            // Navigation to the top product
            ProductManager.getInstance().navigate(
                    Navigation.PreviousLowestSubType);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {

            Product p = getOurProduct();
            ProductVolume v = p.getProductVolume(false);
            if (v != null){
            return (v.getCurrentBaseStatus());
            }else{
                return null;
            }
        }
    }

    public static class ProductMoveLatestBase extends ProductMoveCommand {

        @Override
        public boolean execute() {
            ProductManager.getInstance().navigate(
                    Navigation.LatestBase);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();
            ProductVolume v = p.getProductVolume(true);
             if (v != null){
            return v.getLatestBaseStatus();
             }else{
                 return null;
             }
        }
    }

    public static class ProductMoveLatestUp extends ProductMoveCommand {

        @Override
        public boolean execute() {
            ProductManager.getInstance().navigate(
                    Navigation.LatestUp);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();
            ProductVolume v = p.getProductVolume(true);
            if (v != null){
                 return (v.getLatestUpStatus());
            }else{
                return null;
            }
        }
    }

    public static class ProductMoveLatestDown extends ProductMoveCommand {

        @Override
        public boolean execute() {
            ProductManager.getInstance().navigate(
                    Navigation.LatestDown);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();
            ProductVolume v = p.getProductVolume(true);
            if (v != null){
            return (v.getLatestDownStatus());
            }else{
                return null;
            }
        }
    }

    public static class ProductMoveNextSubType extends ProductMoveCommand {

        @Override
        public boolean execute() {
            ProductManager.getInstance().navigate(
                    Navigation.NextSubType);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();
            ProductVolume v = p.getProductVolume(false);
            if (v!= null){
                return v.getCurrentUpStatus();
            }else{
                return null;               
            }
        }
    }

    public static class ProductMovePreviousSubType extends ProductMoveCommand {

        @Override
        public boolean execute() {
            ProductManager.getInstance().navigate(
                    Navigation.PreviousSubType);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();
            ProductVolume v = p.getProductVolume(false);
            if (v!= null){
                return v.getCurrentDownStatus();
            }else{
                return null;
            }
        }
    }

    public static class ProductMovePreviousTime extends ProductMoveCommand {

        @Override
        public boolean execute() {
            ProductManager.getInstance().navigate(
                    Navigation.PreviousTime);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();

            String label, tip, icon;
            label = "None";
            tip = "";
            icon = BLANK_FILL_ICON;
            boolean enabled = true;

            boolean useColor = false;
            int red = 0, green = 0, blue = 0;
            ProductButtonStatus status = new ProductButtonStatus();

            if (p != null) {

                IndexRecord newRecord = p.peekRecord(Navigation.PreviousTime);
                if (newRecord != null) {
                    Date aDate = newRecord.getTime();
                    label = buttonFormat.format(aDate);
                    tip = tipFormat.format(aDate) + " "
                            + newRecord.getSubType() + " "
                            + newRecord.getTimeStamp();
                } else {
                    enabled = false;
                }
                icon = LEFT_ARROW_ICON;
            }

            // Humm this forces all buttons to be the same
            if (!enabled) {
                useColor = true;
                enabled = false;
                red = green = blue = 200; /* Gainsboro grey */
            }
            status.setButtonText(label);
            status.setToolTip(tip);
            status.setIconString(icon);
            status.setEnabled(enabled);
            if (useColor) {
                status.setColor(red, green, blue);
            }

            return status;

        }
    }

    public static class ProductMoveSyncCurrent extends ProductMoveCommand {

        @Override
        public boolean execute() {
            ProductManager.getInstance().navigate(
                    Navigation.SyncCurrent);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();

            String label, tip, icon;
            label = "None";
            tip = "";
            icon = BLANK_FILL_ICON;
            boolean enabled = true;

            boolean useColor = false;
            int red = 0, green = 0, blue = 0;
            ProductButtonStatus status = new ProductButtonStatus();

            if (p != null) {

                Date aDate = p.getTime();
                label = buttonFormat.format(aDate); // FIXME: exception
                // recovery?
                tip = tipFormat.format(aDate) + " " + p.getSubType()
                        + " " + p.getTimeStamp();

                Date simTime = ProductManager.getInstance().getSimulationTime();
                if (simTime.compareTo(aDate) == 0) {
                    label = "Synced";
                    useColor = true;
                    enabled = false;
                    red = green = blue = 200; /* Gainsboro grey */
                }

            }

            // Humm this forces all buttons to be the same
            if (!enabled) {
                useColor = true;
                enabled = false;
                red = green = blue = 200; /* Gainsboro grey */
            }
            status.setButtonText(label);
            status.setToolTip(tip);
            status.setIconString(icon);
            status.setEnabled(enabled);
            if (useColor) {
                status.setColor(red, green, blue);
            }
            return status;
        }
    }

    public static class ProductMoveNextTime extends ProductMoveCommand {

        @Override
        public boolean execute() {
            ProductManager.getInstance().navigate(
                    Navigation.NextTime);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();

            String label, tip, icon;
            label = "None";
            tip = "";
            icon = "";
            boolean enabled = true;

            boolean useColor = false;
            int red = 0, green = 0, blue = 0;
            ProductButtonStatus status = new ProductButtonStatus();

            if (p != null) {

                IndexRecord newRecord = p.peekRecord(Navigation.NextTime);
                icon = RIGHT_ARROW_ICON;
                enabled = false;

                if (newRecord != null) {
                    Date nextDate = newRecord.getTime();
                    Date currentDate = p.getTime();
                    if (currentDate.compareTo(nextDate) < 0) {
                        enabled = true;
                        label = buttonFormat.format(nextDate);
                        tip = tipFormat.format(nextDate) + " "
                                + newRecord.getSubType() + " "
                                + newRecord.getTimeStamp();
                    }
                }
            }

            // Humm this forces all buttons to be the same
            if (!enabled) {
                useColor = true;
                enabled = false;
                red = green = blue = 200; /* Gainsboro grey */
            }
            status.setButtonText(label);
            status.setToolTip(tip);
            status.setIconString(icon);
            status.setEnabled(enabled);
            if (useColor) {
                status.setColor(red, green, blue);
            }

            return status;
        }
    }

    public static class ProductMoveLatestTime extends ProductMoveCommand {

        @Override
        public boolean execute() {
            ProductManager.getInstance().navigate(
                    Navigation.LatestTime);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();

            String label, tip, icon;
            label = "None";
            tip = "";
            icon = "";
            boolean enabled = true;

            boolean useColor = false;
            int red = 0, green = 0, blue = 0;
            ProductButtonStatus status = new ProductButtonStatus();

            if (p != null) {

                IndexRecord newRecord = p.peekRecord(Navigation.LatestTime);
                icon = RIGHT_ARROW_END_ICON;
                enabled = false;

                if (newRecord != null) {
                    Date nextDate = newRecord.getTime();
                    Date currentDate = p.getTime();
                    if (currentDate.compareTo(nextDate) < 0) {
                        enabled = true;
                        label = buttonFormat.format(nextDate);
                        tip = tipFormat.format(nextDate) + " "
                                + newRecord.getSubType() + " "
                                + newRecord.getTimeStamp();
                    }
                }
            }

            // Humm this forces all buttons to be the same
            if (!enabled) {
                useColor = true;
                enabled = false;
                red = green = blue = 200; /* Gainsboro grey */
            }
            status.setButtonText(label);
            status.setToolTip(tip);
            status.setIconString(icon);
            status.setEnabled(enabled);
            if (useColor) {
                status.setColor(red, green, blue);
            }
            status.setCommand(new ProductMoveLatestTime());

            return status;
        }
    }
}

package org.wdssii.gui.commands;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.wdssii.gui.CommandManager;
import org.wdssii.gui.CommandManager.NavigationMessage;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductButtonStatus;
import org.wdssii.index.IndexRecord;
import org.wdssii.index.VolumeRecord;
import org.wdssii.index.IndexSubType.SubtypeType;

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

    @Override
    public boolean execute() {
        return false;
    }

    /* If myProduct is NULL, then grab the current top product in the display for
     * movement, otherwise, use the product we were set to.
     * FIXME: we're only getting top right now */
    /*public Product getOurProduct(){
    Product p = null;
    
    // Snag top product in the display
    ProductHandlerList pos = CommandManager.getInstance().getProductOrderedSet();
    if (pos != null){
    ProductHandler tph = pos.getTopProductHandler();
    if (tph != null){
    p = tph.getProduct();
    }
    }
    return p;
    }*/
    public static class ProductMovePreviousLowestSubType extends ProductMoveCommand {

        @Override
        public boolean execute() {

            // Navigation to the top product
            CommandManager.getInstance().getProductOrderedSet().navigate(
                    NavigationMessage.PreviousLowestSubType);
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

            if (p == null) {
                label = new String("None");
                tip = new String("");
            } else {
                VolumeRecord volume = p.getVolumeRecord();
                IndexRecord newRecord = volume.getBaseRecord();

                if (newRecord != null) {
                    Date aDate = newRecord.getTime();
                    String subtype = newRecord.getSubType();
                    label = String.format("%s", subtype); // will be more
                    // advanced
                    tip = tipFormat.format(aDate) + " "
                            + newRecord.getSubType() + " "
                            + newRecord.getTimeStamp();

                    Date current = p.getTime(); // FIXME: sim time
                    if (current.compareTo(aDate) == 0) {
                        enabled = false;
                        label = "Synced";
                        tip = "You're at this record";
                    } else {
                        /*
                         * if (volume.baseIsInLatestVolume()){ red = 144; green
                         * = 238; blue = 144; icon = "rightEndIcon"; useColor =
                         * true; }else{ red = 255; green = 192; blue = 203; icon
                         * = "prevNotVirtualIcon"; useColor = true; }
                         */
                    }

                } else {
                    enabled = false;
                    label = "fail";// FIXME:
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
            status.setValidRecord(p != null);
            status.setIconString(icon);
            status.setEnabled(enabled);
            if (useColor) {
                status.setColor(red, green, blue);
            }

            return status;
        }
    }

    public static class ProductMoveLatestBase extends ProductMoveCommand {

        @Override
        public boolean execute() {
            CommandManager.getInstance().getProductOrderedSet().navigate(
                    NavigationMessage.LatestBase);
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

            if (p == null) {
                label = new String("None");
                tip = new String("");
            } else {

                // IndexRecord newRecord =
                // peekRecord(NavigationMessage.LatestBase);
                VolumeRecord volume = p.getVirtualVolumeRecord();
                if (volume != null) {
                    IndexRecord newRecord = volume.getBaseRecord();

                    if (newRecord != null) {
                        Date aDate = newRecord.getTime();
                        String subtype = newRecord.getSubType();
                        label = String.format("%s", subtype); // will be more
                        // advanced
                        tip = tipFormat.format(aDate) + " "
                                + newRecord.getSubType() + " "
                                + newRecord.getTimeStamp();

                        // Date current = myRecord.getTime(); // FIXME: sim time
                        Date current = CommandManager.getInstance().getProductOrderedSet().getSimulationTime();
                        if (current.compareTo(aDate) == 0) {
                            enabled = false;
                            label = "Synced";
                            tip = "You're at this record";
                        } else {

                            if (volume.baseIsInLatestVolume()) {
                                red = 144;
                                green = 238;
                                blue = 144; /* light green */
                                icon = "rightEndIcon";
                                useColor = true;
                            } else {
                                red = 255;
                                green = 192;
                                blue = 203; /* pink */
                                icon = "prevNotVirtualIcon";
                                useColor = true;
                            }
                        }

                    } else {
                        enabled = false;
                        label = "fail";// FIXME:
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
            status.setValidRecord(p != null);
            status.setIconString(icon);
            status.setEnabled(enabled);
            if (useColor) {
                status.setColor(red, green, blue);
            }
            return status;
        }
    }

    public static class ProductMoveLatestUp extends ProductMoveCommand {

        @Override
        public boolean execute() {
            CommandManager.getInstance().getProductOrderedSet().navigate(
                    NavigationMessage.LatestUp);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();

            ProductButtonStatus status = new ProductButtonStatus();

            IndexRecord newRecord = null;
            status.setButtonText("Test");
            SubtypeType theType = p.getSubtypeType();

            switch (theType) { // FIXME: Could create subclass
                case ELEVATION: {
                    VolumeRecord volume = p.getVirtualVolumeRecord();
                    if (volume != null) {
                        newRecord = volume.peekUp();
                        if (volume.upIsInLatestVolume()) {
                            status.setIconString("rightEndIcon");
                            status.setColor(144, 238, 144);
                        } else {
                            status.setIconString("prevNotVirtualIcon");
                            status.setColor(255, 192, 203);
                        }
                    } else {
                        status.setButtonText("NVol");
                    }
                }
                break;
                case MODE_SELECTION: {
                    status.setButtonText("Mode");
                    status.setEnabled(false);
                }
                break;
            }

            if (newRecord != null) {
                status.setIndexRecord(newRecord);
                Date aDate = newRecord.getTime();
                String subtype = newRecord.getSubType();
                String label = String.format("%s", subtype);
                String tip = tipFormat.format(aDate) + " "
                        + newRecord.getSubType() + " "
                        + newRecord.getTimeStamp();
                status.setButtonText(label);
                status.setToolTip(tip);
            } else {
                status.setColor(200, 200, 200); // disabled grey
            }
            return status;

        }
    }

    public static class ProductMoveLatestDown extends ProductMoveCommand {

        @Override
        public boolean execute() {
            CommandManager.getInstance().getProductOrderedSet().navigate(
                    NavigationMessage.LatestDown);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();
            ProductButtonStatus status = new ProductButtonStatus();
            IndexRecord newRecord = null;
            status.setButtonText("Test");
            SubtypeType theType = p.getSubtypeType();
            switch (theType) { // FIXME: Could create subclass
                case ELEVATION: {
                    VolumeRecord volume = p.getVirtualVolumeRecord();
                    if (volume != null) {
                        newRecord = volume.peekDown();
                        if (volume.upIsInLatestVolume()) {
                            status.setIconString("rightEndIcon");
                            status.setColor(144, 238, 144);
                        } else {
                            status.setIconString("prevNotVirtualIcon");
                            status.setColor(255, 192, 203);
                        }
                    } else {
                        status.setButtonText("NVol");
                    }
                }
                break;
                case MODE_SELECTION: {
                    status.setButtonText("Mode");
                }
                break;
            }

            if (newRecord != null) {
                status.setIndexRecord(newRecord);
                Date aDate = newRecord.getTime();
                String subtype = newRecord.getSubType();
                String label = String.format("%s", subtype);
                String tip = tipFormat.format(aDate) + " "
                        + newRecord.getSubType() + " "
                        + newRecord.getTimeStamp();
                status.setButtonText(label);
                status.setToolTip(tip);
            } else {
                status.setColor(200, 200, 200); // disabled grey
            }
            return status;
        }
    }

    public static class ProductMoveNextSubType extends ProductMoveCommand {

        @Override
        public boolean execute() {
            CommandManager.getInstance().getProductOrderedSet().navigate(
                    NavigationMessage.NextSubType);
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

            if (p == null) {
                label = new String("None");
                tip = new String("");
            } else {

                // FIXME: shouldn't be using peekRecord....crap.
                IndexRecord newRecord = p.peekRecord(NavigationMessage.NextSubType);
                if (newRecord != null) {
                    Date aDate = newRecord.getTime();
                    String subtype = newRecord.getSubType();
                    label = String.format("%s", subtype); // will be more
                    // advanced
                    tip = tipFormat.format(aDate) + " "
                            + newRecord.getSubType() + " "
                            + newRecord.getTimeStamp();
                }
                icon = "upIcon";

            }

            // Humm this forces all buttons to be the same
            if (!enabled) {
                useColor = true;
                enabled = false;
                red = green = blue = 200; /* Gainsboro grey */
            }
            status.setButtonText(label);
            status.setToolTip(tip);
            status.setValidRecord(p != null);
            status.setIconString(icon);
            status.setEnabled(enabled);
            if (useColor) {
                status.setColor(red, green, blue);
            }
            return status;
        }
    }

    public static class ProductMovePreviousSubType extends ProductMoveCommand {

        @Override
        public boolean execute() {
            CommandManager.getInstance().getProductOrderedSet().navigate(
                    NavigationMessage.PreviousSubType);
            return true;
        }

        @Override
        public ProductButtonStatus getButtonStatus() {
            Product p = getOurProduct();

            // Return status for the standard nav grid, a 4x4 array of buttons
            // all products have for navigation purposes.
            // FIXME: Kind of a mess.

            String label, tip, icon;
            label = "None";
            tip = "";
            icon = "";
            boolean enabled = true;

            boolean useColor = false;
            int red = 0, green = 0, blue = 0;
            ProductButtonStatus status = new ProductButtonStatus();

            if (p == null) {
                label = new String("None");
                tip = new String("");
            } else {

                IndexRecord newRecord = p.peekRecord(NavigationMessage.PreviousSubType);
                if (newRecord != null) {
                    Date aDate = newRecord.getTime();
                    String subtype = newRecord.getSubType();
                    label = String.format("%s", subtype); // will be more
                    // advanced
                    tip = tipFormat.format(aDate) + " "
                            + newRecord.getSubType() + " "
                            + newRecord.getTimeStamp();
                }
                icon = "downIcon";
            }

            // Humm this forces all buttons to be the same
            if (!enabled) {
                useColor = true;
                enabled = false;
                red = green = blue = 200; /* Gainsboro grey */
            }
            status.setButtonText(label);
            status.setToolTip(tip);
            status.setValidRecord(p != null);
            status.setIconString(icon);
            status.setEnabled(enabled);
            if (useColor) {
                status.setColor(red, green, blue);
            }

            return status;

        }
    }

    public static class ProductMovePreviousTime extends ProductMoveCommand {

        @Override
        public boolean execute() {
            CommandManager.getInstance().getProductOrderedSet().navigate(
                    NavigationMessage.PreviousTime);
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

            if (p == null) {
                label = new String("None");
                tip = new String("");
            } else {

                IndexRecord newRecord = p.peekRecord(NavigationMessage.PreviousTime);
                if (newRecord != null) {
                    Date aDate = newRecord.getTime();
                    label = buttonFormat.format(aDate);
                    tip = tipFormat.format(aDate) + " "
                            + newRecord.getSubType() + " "
                            + newRecord.getTimeStamp();
                } else {
                    enabled = false;
                }
                icon = "leftIcon";
            }

            // Humm this forces all buttons to be the same
            if (!enabled) {
                useColor = true;
                enabled = false;
                red = green = blue = 200; /* Gainsboro grey */
            }
            status.setButtonText(label);
            status.setToolTip(tip);
            status.setValidRecord(p != null);
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
            CommandManager.getInstance().getProductOrderedSet().navigate(
                    NavigationMessage.SyncCurrent);
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

            if (p == null) {
                label = new String("None");
                tip = new String("");
            } else {

                Date aDate = p.getTime();
                label = buttonFormat.format(aDate); // FIXME: exception
                // recovery?
                tip = tipFormat.format(aDate) + " " + p.getSubType()
                        + " " + p.getTimeStamp();

                Date simTime = CommandManager.getInstance().getProductOrderedSet().getSimulationTime();
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
            status.setValidRecord(p != null);
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
            CommandManager.getInstance().getProductOrderedSet().navigate(
                    NavigationMessage.NextTime);
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

            if (p == null) {
                label = new String("None");
                tip = new String("");
            } else {

                IndexRecord newRecord = p.peekRecord(NavigationMessage.NextTime);
                icon = "rightIcon";
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
            status.setValidRecord(p != null);
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
            CommandManager.getInstance().getProductOrderedSet().navigate(
                    NavigationMessage.LatestTime);
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

            if (p == null) {
                label = new String("None");
                tip = new String("");
            } else {

                IndexRecord newRecord = p.peekRecord(NavigationMessage.LatestTime);
                icon = "rightEndIcon";
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
            status.setValidRecord(p != null);
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

package org.wdssii.gui.products.volumes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.commands.ProductMoveCommand;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.Product.Navigation;
import org.wdssii.core.ProductButtonStatus;
import org.wdssii.gui.sources.IndexSource;
import org.wdssii.gui.sources.Source;
import org.wdssii.gui.sources.SourceList;
import org.wdssii.index.HistoricalIndex;
import org.wdssii.index.IndexRecord;
import org.wdssii.index.VolumeRecord;

/**
 * A Product volume that uses individual records for 'up' and 'down'. RadialSets
 * are an example of this type of volume LatLonGrids are an example of this type
 * of volume
 *
 * @author Robert Toomey
 */
public class IndexRecordVolume extends ProductVolume {
    // Default button formatter

    private static final SimpleDateFormat buttonFormat = new SimpleDateFormat(
            "HH:mm:ss");
    // Tool tip formatter. FIXME: from product?
    private static final SimpleDateFormat tipFormat = new SimpleDateFormat(
            "---HH:mm:ss");
    // The comparator that tells record ordering in a volume
    // For now, static final (shared for all products )since we'll probably never need to change the volume subtype order,
    // but ya never know.
    // FIXME: maybe push into DataType class??
    protected static final Comparator<IndexRecord> myRecordComparer = VolumeRecord.getDefaultComparator();

    @Override
    public void initVirtual(Product init, boolean virtual) {
        super.initVirtual(init, virtual);
    }

    public static HistoricalIndex getIndexByName(String name) {
        Source s = SourceList.theSources.getSource(name);
        HistoricalIndex anIndex = null;
        if (s instanceof IndexSource) {
            anIndex = ((IndexSource) s).getIndex();
        }
        return anIndex;
    }

    /**
     * Get the virtual VolumeRecord for this volume
     *
     * @param indexKey	SourceManager index key
     * @param reference	IndexRecord to use as reference to find the volume
     * @return	the virtual (latest records per subtype) VolumeRecord
     */
    public static VolumeRecord getVirtualVolumeRecord(String indexKey,
            IndexRecord reference) {
        HistoricalIndex index = getIndexByName(indexKey);
        VolumeRecord newOne = VolumeRecord.getVirtualVolumeRecord(index, reference, myRecordComparer);
        if (newOne != null) {
            for (IndexRecord r : newOne) { // We require the source name
                r.setSourceName(indexKey);
            }
        }
        return newOne;
    }

    /**
     * Get the VolumeRecord for this volume
     *
     * @param indexKey	SourceManager index key
     * @param reference	IndexRecord to use as reference to find the volume
     * @return	the VolumeRecord
     */
    public static VolumeRecord getVolumeRecord(String indexKey, IndexRecord reference) {
        HistoricalIndex index = getIndexByName(indexKey);
        VolumeRecord newOne = VolumeRecord.getVolumeRecord(index, reference, myRecordComparer);
        for (IndexRecord r : newOne) { // We require the source name
            r.setSourceName(indexKey);
        }
        return newOne;
    }

    /**
     * Load every product in our virtual volume... FIXME: move into
     * ProductVolume helper class
     */
    public ArrayList<Product> loadVolumeProducts(String indexKey, IndexRecord sourceRecord, boolean virtual) {
        ArrayList<Product> list = new ArrayList<Product>();
        VolumeRecord volume = null;
        if (virtual) {
            volume = getVirtualVolumeRecord(indexKey, sourceRecord);
        } else {
            volume = getVolumeRecord(indexKey, sourceRecord);
        }

        if (volume != null) {
            Iterator<IndexRecord> iter = volume.iterator();
            while (iter.hasNext()) {
                // Create a cache key for product....
                IndexRecord record = iter.next();
                String productCacheKey = Product.createCacheKey(indexKey, record);
                Product p = ProductManager.CreateProduct(productCacheKey, indexKey, record);
                list.add(p);
            }
        }

        return list;
    }

    // -------------------------------------------------------------------------------
    // ProductStatus information, for buttons, menus, etc...
    @Override
    public ProductButtonStatus getLatestUpStatus() {
        ProductButtonStatus status = new ProductButtonStatus();
        IndexRecord newRecord = null;
        status.setButtonText("?");
        if (myRootProduct != null) {

            VolumeRecord volume = getVirtualVolumeRecord(myRootProduct.getIndexKey(), myRootProduct.getRecord());
            if (volume != null) {
                newRecord = volume.peekUp();
                if (volume.upIsInLatestVolume()) {
                    status.setIconString(ProductMoveCommand.RIGHT_ARROW_END_ICON);
                    status.setColor(144, 238, 144);
                } else {
                    status.setIconString(ProductMoveCommand.PREV_NOT_VIRTUAL_ICON);
                    status.setColor(255, 192, 203);
                }
            } else {
                status.setButtonText("NVol");
            }

        }

        if (newRecord != null) {
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

    @Override
    public ProductButtonStatus getLatestDownStatus() {
        ProductButtonStatus status = new ProductButtonStatus();
        IndexRecord newRecord = null;
        status.setButtonText("?");
        if (myRootProduct != null) {

            VolumeRecord volume = getVirtualVolumeRecord(myRootProduct.getIndexKey(), myRootProduct.getRecord());
            if (volume != null) {
                newRecord = volume.peekDown();
                if (volume.upIsInLatestVolume()) {
                    status.setIconString(ProductMoveCommand.RIGHT_ARROW_END_ICON);
                    status.setColor(144, 238, 144);
                } else {
                    status.setIconString(ProductMoveCommand.PREV_NOT_VIRTUAL_ICON);
                    status.setColor(255, 192, 203);
                }
            } else {
                status.setButtonText("NVol");
            }

        }

        if (newRecord != null) {
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

    @Override
    public ProductButtonStatus getLatestBaseStatus() {
        String label, tip, icon;
        label = "None";
        tip = "";
        icon = ProductMoveCommand.BLANK_FILL_ICON;
        boolean enabled = true;

        boolean useColor = false;
        int red = 0, green = 0, blue = 0;
        ProductButtonStatus status = new ProductButtonStatus();

        if (myRootProduct != null) {

            // IndexRecord newRecord =
            // peekRecord(NavigationMessage.LatestBase);
            VolumeRecord volume = getVirtualVolumeRecord(myRootProduct.getIndexKey(), myRootProduct.getRecord());
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
                    Date current = ProductManager.getInstance().getSimulationTime();
                    if (current.compareTo(aDate) == 0) {
                        enabled = false;
                        label = "Synced";
                        tip = "You're at this record";
                    } else {

                        if (volume.baseIsInLatestVolume()) {
                            red = 144;
                            green = 238;
                            blue = 144; /*
                             * light green
                             */
                            icon = ProductMoveCommand.RIGHT_ARROW_END_ICON;

                            useColor = true;
                        } else {
                            red = 255;
                            green = 192;
                            blue = 203; /*
                             * pink
                             */
                            icon = ProductMoveCommand.PREV_NOT_VIRTUAL_ICON;
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
            red = green = blue = 200; /*
             * Gainsboro grey
             */
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

    /**
     * What's funny is that the standard up doesn't require any volume data..
     *
     * @return
     */
    @Override
    public ProductButtonStatus getCurrentUpStatus() {

        String label, tip, icon;
        label = "None";
        tip = "";
        icon = ProductMoveCommand.BLANK_FILL_ICON;
        boolean enabled = true;
        boolean useColor = false;
        int red = 0, green = 0, blue = 0;
        ProductButtonStatus status = new ProductButtonStatus();

        if (myRootProduct != null) {

            // FIXME: shouldn't be using peekRecord....crap.
            IndexRecord newRecord = myRootProduct.peekRecord(Navigation.NextSubType);
            if (newRecord != null) {
                Date aDate = newRecord.getTime();
                String subtype = newRecord.getSubType();
                label = String.format("%s", subtype); // will be more
                // advanced
                tip = tipFormat.format(aDate) + " "
                        + newRecord.getSubType() + " "
                        + newRecord.getTimeStamp();
            }
            icon = ProductMoveCommand.UP_ARROW_ICON;

        }

        // Humm this forces all buttons to be the same
        if (!enabled) {
            useColor = true;
            enabled = false;
            red = green = blue = 200; /*
             * Gainsboro grey
             */
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

    @Override
    public ProductButtonStatus getCurrentDownStatus() {

        // Return status for the standard nav grid, a 4x4 array of buttons
        // all products have for navigation purposes.
        // FIXME: Kind of a mess.

        String label, tip, icon;
        label = "None";
        tip = "";
        icon = ProductMoveCommand.BLANK_FILL_ICON;
        boolean enabled = true;

        boolean useColor = false;
        int red = 0, green = 0, blue = 0;
        ProductButtonStatus status = new ProductButtonStatus();

        if (myRootProduct != null) {

            IndexRecord newRecord = myRootProduct.peekRecord(Navigation.PreviousSubType);
            if (newRecord != null) {
                Date aDate = newRecord.getTime();
                String subtype = newRecord.getSubType();
                label = String.format("%s", subtype); // will be more
                // advanced
                tip = tipFormat.format(aDate) + " "
                        + newRecord.getSubType() + " "
                        + newRecord.getTimeStamp();
            }
            icon = ProductMoveCommand.DOWN_ARROW_ICON;
        }

        // Humm this forces all buttons to be the same
        if (!enabled) {
            useColor = true;
            enabled = false;
            red = green = blue = 200; /*
             * Gainsboro grey
             */
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

    @Override
    public ProductButtonStatus getCurrentBaseStatus() {

        String label, tip, icon;
        label = "None";
        tip = "";
        icon = ProductMoveCommand.BLANK_FILL_ICON;
        boolean enabled = true;

        boolean useColor = false;
        int red = 0, green = 0, blue = 0;
        ProductButtonStatus status = new ProductButtonStatus();

        if (myRootProduct != null) {
            VolumeRecord volume = getVolumeRecord(myRootProduct.getIndexKey(), myRootProduct.getRecord());
            IndexRecord newRecord = volume.getBaseRecord();

            if (newRecord != null) {
                Date aDate = newRecord.getTime();
                String subtype = newRecord.getSubType();
                label = String.format("%s", subtype); // will be more
                // advanced
                tip = tipFormat.format(aDate) + " "
                        + newRecord.getSubType() + " "
                        + newRecord.getTimeStamp();

                Date current = myRootProduct.getTime(); // FIXME: sim time
                if (current.compareTo(aDate) == 0) {
                    enabled = false;
                    label = "Synced";
                    tip = "You're at this record";
                } else {
                    /*
                     * if (volume.baseIsInLatestVolume()){
                     * red = 144; green = 238; blue = 144;
                     * icon = RIGHT_END_ICON; useColor =
                     * true; }else{ red = 255; green = 192;
                     * blue = 203; icon =
                     * PREV_NOT_VIRTUAL_ICON; useColor =
                     * true; }
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
            red = green = blue = 200; /*
             * Gainsboro grey
             */
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

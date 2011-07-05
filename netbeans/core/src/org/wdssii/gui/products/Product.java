package org.wdssii.gui.products;

import gov.nasa.worldwind.render.DrawContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;

import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.SourceManager;
import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMetric;
import org.wdssii.gui.CommandManager.NavigationMessage;
import org.wdssii.core.LRUCache.LRUCacheItem;
import org.wdssii.gui.products.navigators.ProductNavigator;
import org.wdssii.gui.products.volumes.IndexRecordVolume;
import org.wdssii.gui.products.volumes.ProductVolume;

import org.wdssii.index.IndexRecord;
import org.wdssii.index.IndexSubType;
import org.wdssii.index.VolumeRecord;
import org.wdssii.index.HistoricalIndex.Direction;
import org.wdssii.index.IndexSubType.SubtypeType;

/** Product is a holder for everything that can possibly be done for a particular
 * type of data in the display.
 * It is a wrapper to a DataType.  It allows a few things:
 * 1.  Lazy loading/creation of DataType in background thread.
 * 2.  Helper objects for this DataType, created by DataType name:
 * 		Standard Helper objects include:
 * 		a.  "Renderer" ProductRenderer(s) -- Draw in the 3D world window
 *      b.  "Volume" (virtual and regular) ProductVolume -- The volume collection of the product (cached/shared with other products)
 *      c.  "2DTable"  Product2DTable -- Draws 2D grid of data (if possible) in the DataTable view
 *      d.  "Navigator" -- Handles navigation for product
 *      
 * 3. Products are shared objects.  So things like visibility should not be here.  Instead
 * they are in the ProductHandler class (which handles a particular display/etc of a Product)
 * 4. So much is involved in the concept of a 'Product', that the work of a product is 
 * broken down into helper classes.
 * 
 * FIXME: Should have 'helper' ability as its own class or interface probably...
 * 
 * Product should hide the Index and IndexRecord (core code) from the display.
 *
 * @author Robert Toomey
 **/
public class Product implements LRUCacheItem {

    private static Log log = LogFactory.getLog(Product.class);
    // FIXME: Currently products will have only one possible product handler,
    // when we go to multiple product handler lists design will have to change
    // to allow for example more than one renderer per product
    protected ProductHandler myProductHandler = null;
    // Helper class paths.  These are created by name from the DataType, thus
    // a RadialSetNavigator will be created (if there) for a RadialSet, etc.
    private final String RENDERER_CLASSPATH = "org.wdssii.gui.products";
    private final String TABLE_CLASSPATH = "org.wdssii.gui.products";
    private final String VOLUME_CLASSPATH = "org.wdssii.gui.products.volumes";
    private final String NAVIGATOR_CLASSPATH = "org.wdssii.gui.products.navigators";

    public enum ProductTimeWindowInfo {

        IN_WINDOW,
        TOO_NEW,
        TOO_OLD,
        BAD_PRODUCT
    };
    // Raw DataType, if loaded. DataType is loaded in background thread
    final protected Object myRawDataSync = new Object();
    protected DataType myRawDataType = null;
    protected String myDataType; // FIXME: Store this or just get from record?
    protected boolean myDirtyRenderer = true;
    protected String myCacheKey;
    protected SubtypeType mySubtypeType;
    protected DataRequest myDataRequest = null;
    /** Units of the actual data, if known  */
    protected String myDataUnits = "Data";
    // FIXME: need to sync access to this probably...
    protected TreeMap<String, Object> myHelperObjects = new TreeMap<String, Object>();
    // We're going to hide the record details
    protected IndexRecord myRecord; // /< The index record we hold
    private String myIndexKey; 		// /< The key used to query the source manager

    public void superInit(DataRequest dr, String anIndex, IndexRecord init) {
        myDataRequest = dr;
        init(anIndex, init, null);
    }

    /** Update product if DataType is loaded */
    public boolean updateDataTypeIfLoaded() {
        boolean ready = false;
        if ((myDataRequest != null) && (myDataRequest.isReady())) {
            ready = true;
            synchronized (myRawDataSync) {
                myRawDataType = myDataRequest.getDataType();
                if (myRawDataType != null) {
                    myDataUnits = myRawDataType.getUnit();
                } else {
                    myDataUnits = "?";
                }
            }
        }
        return ready;
    }

    /**
     * 
     * @return if this product fills the physical space around radar. Thus, any
     *         other product that alsos fills the space cannot be shown at same
     *         time (two radial sets, for example)
     */
    public boolean fillsSpaceAroundRadar() {

        // FIXME: no overlap for now...will need a visual helper object...
        return true;
    }

    // FIXME: some sort of visual helper object or put into renderer?
    public boolean spaceAvailable(ProductHandlerList list) {
        // Look through the list of products for other radial sets
        // Iterate through the drawing product list...
        // if find a visible radial set AFTER us in list..we can't draw
        // or it will conflict...(Remember last in list draws on top)
        Iterator<ProductHandler> iter = list.getDrawIterator();
        boolean foundUs = false;
        boolean spaceIsAllOurs = true;
        String mySource = getIndexKey();
        // String outString = "";
        while (iter.hasNext()) {
            ProductHandler h = iter.next();
            Product p = h.getProduct();
            if (p != null) {
                if (p == this) {
                    // Now if we find a radial set further on that's
                    // not hidden, it will be drawing, so we don't.
                    foundUs = true;
                } else {
                    if (mySource == null) {
                        System.out.println("***** my source is NULL?????");
                    }
                    if (foundUs
                            && // We're in the list (so this draws after us)
                            // FIXME: or do we want the ACTUAL would be showing function instead?
                            // For instance if h is out of time window, do we want to draw?
                            (h.getIsVisible()) && // This product is not hidden
                            // by user
                            (p.fillsSpaceAroundRadar()) && // More general than
                            // class check
                            (p.getIndexKey().compareTo(mySource) == 0)) // Source
                    // name
                    // same
                    {
                        spaceIsAllOurs = false; // We can't draw :(
                        // outString = p.getProductInfoString();
                    }
                }
            }
        }
        // if (!spaceIsAllOurs){
        // System.out.println("Can't draw "+this.getProductInfoString() +
        // "(space conflict) with "+outString);
        // }
        return spaceIsAllOurs;
    }

    public void setCacheKey(String cacheKey) {
        myCacheKey = cacheKey;
    }

    @Override
    public String getCacheKey() {
        return myCacheKey;
    }

    /** Return the list of records for looping when this product is selected.
     * Typically this is the latest(n) time records at our subtype.  Count is the 
     * number of records wanted
     * 
     *  FIXME: should become a helper class...
     */
    public static class ProductLoopRecord {

        /** The list of records for looping on this product.  Note product might eventually not use this */
        public ArrayList<IndexRecord> list = new ArrayList<IndexRecord>();
        /** The current record we are at, can be null */
        public IndexRecord atRecord;
        /** The current index into list that is us, if any...otherwise -1 */
        public int atRecordReference = -1;

        /** Get the number of records in us */
        public int size() {
            return list.size();
        }
    }

    /** Get the records for looping upon for this product.  We use the IndexRecord here,
     * but eventually we could remove this dependency if needed.
     * @param count number of records backwards from latest
     * @return a ProductLoopRecord containing looping information
     */
    public ProductLoopRecord getLoopRecords(int count) {
        ProductLoopRecord lr = new ProductLoopRecord();

        if (myRecord != null) {
            lr.atRecord = myRecord;
            // FIXME: this isn't very efficient...make a routine for this
            if (count > 0) {
                IndexRecord latestTime = SourceManager.getRecord(myIndexKey, myRecord,
                        Direction.LatestTime);
                lr.list.add(latestTime);

                // FIXME: latestTime comes back null sometimes..what to do then?

                // Compare records based on fields, not object.
                if (latestTime.getTime().equals(myRecord.getTime())) {
                    lr.atRecordReference = 0;
                }
                IndexRecord last = latestTime;
                for (int i = 1; i < count; i++) {
                    if (last != null) {
                        IndexRecord rec = SourceManager.getRecord(myIndexKey, last,
                                Direction.PreviousTime);
                        if (rec != null) {
                            lr.list.add(rec);
                            // Compare records based on fields, not object.
                            if (rec.getTime().equals(myRecord.getTime())) {
                                lr.atRecordReference = i;
                            }
                        }
                        last = rec;
                    }
                }
            }
        }
        return lr;
    }

    /** Every Product has a single record for it.  */
    public IndexRecord getRecord() {
        return myRecord;
    }

    public String getDataType() {
        if (myRecord != null) {
            return myRecord.getDataType();
        }
        return "DATATYPE"; // What to do here?
    }

    public String getSubType() {
        if (myRecord != null) {
            return myRecord.getSubType();
        }
        return "SUBTYPE"; // What to do here?
    }

    public String getTimeStamp() {
        if (myRecord != null) {
            return myRecord.getTimeStamp();
        }
        return "TIMESTAMP"; // What to do here?
    }

    /** Return the 'base' location, such as radar center, data center, etc...
     * This is used by the jump ability in the navigator.
     * @return Center location of radar, for example
     */
    final public Location getBaseLocation() {
        if (updateDataTypeIfLoaded()) {
            synchronized (myRawDataSync) {
                if (myRawDataType != null) {
                    return myRawDataType.getJumpToLocation();
                }
            }
        }
        return null;
    }

    private Object getHelperClass(String name) {
        return myHelperObjects.get(name);
    }

    private void setHelperClass(String name, Object helper) {
        myHelperObjects.put(name, helper);
    }

// Stock helper objects -------------------------------------------------------
    // Return the thing that draws this product
    public ProductRenderer getRenderer() {
        ProductRenderer pr = (ProductRenderer) getHelperObject("Renderer", false, RENDERER_CLASSPATH, "");
        return pr;
    }

    // Return the thing that draws this product
    public ProductVolume getProductVolume(boolean virtual) {
        // We create one Volume for virtual, one for regular. We need unique ones
        // since multiple threads hit this.  Example: Chart draws a virtual VSlice in one thread,
        // while worldwind window draws regular volume 3D Slice in it.
        ProductVolume vp = (ProductVolume) getHelperObject("Volume", false, VOLUME_CLASSPATH, virtual ? "Virtual" : "");
        if (vp != null) {
            vp.initVirtual(this, virtual);
        }
        return vp;
    }

    /** Convenience method to get the 2D Table for this Product, if any */
    public Product2DTable get2DTable() {
        Product2DTable t = (Product2DTable) getHelperObject("2DTable", false, TABLE_CLASSPATH, "");
        if (t == null) {  // Do a default...for the moment at least....
            t = new Product2DTable();
            setHelperClass("2DTable:", t);
        }
        return t;
    }

    /** Create a navigator for this product.  Every ProductHandler will have
    a unique ProductNavigator.  Imagine two windows..each would have 
    a different ProductHandler with the SAME Product, different actual
    navigated location...
     * 
     * This is called by ProductHandler to get the Navigator for this product.
     * Note it will return NULL until the product DataType is loaded.
     */
    public ProductNavigator createNavigator() {
        ProductNavigator t = (ProductNavigator) getHelperObject("Navigator", true, NAVIGATOR_CLASSPATH, "");
        return t;
    }

    // ----------------------------------------------------------------------
    /** Return the data value for this product for a location.  Subclasses should override */
    public double getValueAtLocation(Location loc, boolean useHeight) {
        return DataType.MissingData;
    }

    /** Get the current units for product.  This may change as product is loaded into memory,
     * typically it starts as Dimensionless
     * @return
     */
    public String getCurrentUnits() {
        return myDataUnits;
    }

    // This tells me if the getTime below is a real time or not...
    public boolean isValidProduct() {
        return (myRecord != null);
    }

    public Date getTime() {
        if (myRecord != null) {
            return myRecord.getTime();
        }
        return new Date();  /// FIXME: this will screw up time window stuff
    }

    public String getCacheFilePath() {
        if (myRecord != null) {
            // FIXME: maybe a builder/buffer here to save some memory
            return myRecord.getSourceName() + "/" + myRecord.getSubType() + "/"
                    + myRecord.getTimeStamp();
        }
        return "ERROR"; // What to do here? Random directory name?
    }

    // Convert from a data value to a string.  Subclasses can add their own static methods
    // to change the default
    public static String valueToString(float value) {
        String text = null;
        if (DataType.isRealDataValue(value)) {
            if (value < .05) {
                text = String.format("%5.5f", value);
            } else {
                text = String.format("%5.2f", value);
            }

        } else {
            text = ".";
        }
        return text;
    }

    // Return the current color map for this product (cache within color map
    // manager)
    // Note the colormap can CHANGE. Objects should keep a copy for a single
    // pass
    public ColorMap getColorMap() {
        ColorMap aColorMap;
        ProductManager cman = ProductManager.getInstance();

        /** Force generation test */
        boolean forceGenerate = false;

        if (forceGenerate) {  // For testing.
            if (cman.hasColorMap(myDataType)) {
                aColorMap = cman.getColorMap(myDataType);
            } else {
                aColorMap = generateColorMap(myDataUnits);
                if (aColorMap != null) {
                    cman.storeNewColorMap(myDataType, aColorMap);
                }
            }

        } else {
            aColorMap = cman.getColorMap(myDataType);
            if (aColorMap == null) {
                aColorMap = generateColorMap(myDataUnits);
                if (aColorMap != null) {
                    cman.storeNewColorMap(myDataType, aColorMap);
                }
            }
        }
        return aColorMap;
    }

    /** Used by classes instead of color map */
    public FilterList getFilterList() {
        return myProductHandler.getFList();
    }

    /** Generate a color map based on data values.  This is currently used when
     * colormap is missing.  Subclasses can override to make a map better based
     * on class (RadialSet, etc.)
     */
    public ColorMap generateColorMap(String units) {

        ColorMap aColorMap = null;
        if (updateDataTypeIfLoaded()) {
            aColorMap = new ColorMap();
            if (myRawDataType != null) {
                DataTypeMetric m = myRawDataType.getDataTypeMetric();
                float minValue = m.getMinValue();
                float maxValue = m.getMaxValue();
                aColorMap.initFromLinear(30, minValue, maxValue, units, ProductTextFormatter.DEFAULT_FORMATTER);
            } else {
                aColorMap.initFromLinear(30, -1000, 1000, "?", ProductTextFormatter.DEFAULT_FORMATTER);
            }
        }
        return aColorMap;
    }

    /** Get a helper object from cache */
    protected Object getHelperObject(String classSuffix, boolean useBaseClass, String root, String extrainfo) {
        Object helper = null;

        // Is it cached?
        helper = getHelperClass(classSuffix + ":" + extrainfo);
        if (helper == null) {

            // If we are loaded as of NOW....
            if (updateDataTypeIfLoaded()) {
                if (myDataRequest != null) {
                    DataType dt = myDataRequest.getDataType();
                    // Try to load by NAME.  This fails only if class doesn't
                    // actually exist...
                    if (dt != null) {
                        String dataName = dt.getClass().getSimpleName();
                        helper = createClassFromDataType(dataName, root, classSuffix);

                        // If "RadialSetVolume" missing, create "ProductVolume" (example)
                        if ((helper == null) && (useBaseClass == true)) {
                            helper = createClassFromDataType("Product", root, classSuffix);
                        }
                    }else{
                        log.error("Datatype doesn't exist..should be here");
                    }
                }
            }

            // Store object in cache
            if (helper != null) {
                setHelperClass(classSuffix + ":" + extrainfo, helper);
            }
        }
        return helper;
    }

    /** Create a helper object class from a valid DataType */
    protected Object createClassFromDataType(String dataName, String rootpath, String suffix) {

        Object newClass = null;

        String createIt = rootpath + "." + dataName + suffix;
        log.info("Looking for class " + createIt);

        Class<?> c = null;

        //boolean foundByName = false;
        try {
            c = Class.forName(createIt);
            newClass = c.newInstance();
        } catch (Exception e) {
            log.warn("DataType " + dataName + " doesn't have a " + suffix + " it seems");
        }
        log.info("Generated " + suffix + " for datatype " + dataName);

        return newClass;
    }

    public ProductHandler getNewProductHandler(ProductHandlerList aList) {
        return new ProductHandler(aList, this); // Default is the 'base' handler that
        // just hides/shows
    }

    // FIXME: readout object, not a string
    public String getReadout(double latDegree, double lonDegree, double height) {
        return "?";
    }

    // called with null
    public void init(String anIndex, IndexRecord init, DataType incoming) {
        synchronized (myRawDataSync) {
            myRawDataType = incoming;
        }
        myRecord = init;
        if (myRecord != null) {
            myDataType = myRecord.getDataType();
            mySubtypeType = IndexSubType.getSubtypeType(myRecord.getSubType());
        } else {
            myDataType = "UNKNOWN";
            mySubtypeType = IndexSubType.SubtypeType.ELEVATION;
        }
        myIndexKey = anIndex;
        if (incoming != null) {
            // FIXME: Strange the unit has done away from the netcdf..
            String unit = (String) incoming.getAttribute("Unit");
            if (unit != null) {
                myDataUnits = unit;
            }
        }
    }

    /** Get DataType if it is available */
    public DataType getRawDataType() {
        synchronized (myRawDataSync) {
            return myRawDataType;
        }
    }

    public SubtypeType getSubtypeType() {
        return mySubtypeType;
    }

    /** Get the index key this product uses.  This is not the gui visible name */
    public String getIndexKey() {
        return myIndexKey;
    }

    /**
     * Get the info string. This is displayed by the navigation view when this product
     * is selected.
     * 
     * @return string
     */
    public String getProductInfoString() {
        String shortName = SourceManager.getInstance().getNiceShortName(getIndexKey());
        return (String.format("%s %s %s %s", shortName, getDataType(), getSubType(), getTimeStamp()));
    }

    /**
     * Get the display string. This is displayed by the product selection list
     * @return string
     */
    public String getIndexDatatypeString() {
        String shortName = SourceManager.getInstance().getNiceShortName(getIndexKey());
        return (String.format("%s %s", shortName, getDataType()));
    }

    public boolean timeStampLess(IndexRecord a, IndexRecord b) {
        boolean less = false;
        if (a != null && (b != null)) {
            if (a.getTime().before(b.getTime())) {
                less = true;
            }
        }
        return less;
    }

    /** Sort the given product list into volume order.  For RadialSets, this would be the
     * elevation order.
     * 
     * Note: This function assumes every product in the list is the same datatype
     * This should always be the case, mixing different products in a volume currently
     * would be strange.
     * 
     * FIXME: move into ProductVolume helper class
     * @param products
     */
    public void sortVolumeProducts(ArrayList<Product> products) {
        Collections.sort(products, new Comparator<Product>() {

            @Override
            public int compare(Product o1, Product o2) {
                double u1 = o1.sortInVolume();
                double u2 = o2.sortInVolume();
                if (u1 < u2) {
                    return -1;
                }
                if (u1 > u2) {
                    return 1;
                }
                return 0;
            }
        });
    }

    /** Return a double used to sort a volume of this DataType.  For example,
     * for RadialSets this would be the elevation value.
     * @return value in volume
     */
    public double sortInVolume() {
        if (updateDataTypeIfLoaded()) {
            return myRawDataType.sortInVolume();
        }
        return 0.0;
    }

    private IndexRecord getLatestUp() {
        IndexRecord newRecord = null;

        switch (mySubtypeType) { // FIXME: Could create subclass
            case ELEVATION: {
                VolumeRecord volume = IndexRecordVolume.getVirtualVolumeRecord(myIndexKey,
                        myRecord);
                if (volume != null) {
                    newRecord = volume.peekUp();
                }
            }
            break;
            case MODE_SELECTION: {
            }
            break;
        }

        return newRecord;
    }

    private IndexRecord getLatestDown() {
        IndexRecord newRecord = null;

        switch (mySubtypeType) { // FIXME: Could create subclass
            case ELEVATION: {
                VolumeRecord volume = IndexRecordVolume.getVirtualVolumeRecord(myIndexKey,
                        myRecord);
                if (volume != null) {
                    newRecord = volume.peekDown();
                }
            }
            break;
            case MODE_SELECTION: {
            }
            break;
        }

        return newRecord;
    }

    /** Create the default cache key.  The record selector uses this to get the default
     * key for a regular product.  The default product internal creation uses this key
     * as well.  Regular products at least have one index and record per product
     * 
     * @return the cache key for a product
     */
    public static String createCacheKey(String indexKey, IndexRecord aRecord) {
        if (aRecord != null) {
            return String.format("%s:%s:%s:%s", aRecord.getTimeStamp(), indexKey, aRecord.getDataType(), aRecord.getSubType());
        }
        return null;
    }

    /** Get the product in the given navigation direction, if any.
    FIXME: this routine only works for index-record products that change
    subtype on each IndexRecord...
     */
    public Product getProduct(NavigationMessage nav) {
        Product navigateTo = null;
        IndexRecord newRecord = null; // do it by a record
        newRecord = peekRecord(nav);
        if (newRecord != null) {

            // Create a cache key for product....
            String productCacheKey = Product.createCacheKey(myIndexKey, newRecord);
            navigateTo = ProductManager.CreateProduct(productCacheKey, myIndexKey, newRecord);
        }
        return navigateTo;
    }

    /**
     * Peek in a navigation direction and get the record for it. Note since a
     * product could exist without an Index or IndexRecord this may not be valid
     * for all types of products. 
     * @param nav
     *            Navigation direction
     * @return IndexRecord of direction, if ANY
     */
    public IndexRecord peekRecord(NavigationMessage nav) {
        IndexRecord newRecord = null;

        switch (nav) {

            // Standard movement by index record
            case PreviousTime:
                newRecord = SourceManager.getRecord(myIndexKey, myRecord,
                        Direction.PreviousTime);
                break;
            case NextTime:
                newRecord = SourceManager.getRecord(myIndexKey, myRecord,
                        Direction.NextTime);
                break;
            case PreviousSubType:
                newRecord = SourceManager.getRecord(myIndexKey, myRecord,
                        Direction.PreviousSubType);
                break;
            case NextSubType:
                newRecord = SourceManager.getRecord(myIndexKey, myRecord,
                        Direction.NextSubType);
                break;
            case PreviousLowestSubType:
                newRecord = SourceManager.getPreviousLatestRecord(myIndexKey,
                        myRecord);
                break;

            // Union of latest time and latest virtual volume
            case LatestTime:
                newRecord = SourceManager.getRecord(myIndexKey, myRecord,
                        Direction.LatestTime);
                break;

            // The virtual volume products (or latest mode settings for non-elevation products)
            case LatestUp:
                newRecord = getLatestUp();
                break;
            case LatestDown:
                newRecord = getLatestDown();
                break;
            case LatestBase: {
                VolumeRecord volume = IndexRecordVolume.getVirtualVolumeRecord(myIndexKey, myRecord);
                newRecord = volume.getBaseRecord();
            }
            break;

            case SyncCurrent:
                newRecord = myRecord; // It's the record we're at right?
                break;
            default:
                System.out.println("Navigation I don't understand");
                break;
        }

        return newRecord;
    }

    public String getProductType() {
        String name = "loading";
        if (updateDataTypeIfLoaded()) {
            if (myRawDataType == null) {
                name = "failed";
            } else {
                Class<?> c = myRawDataType.getClass();
                name = c.getSimpleName();
            }
        }
        return (name);
    }

    /** @return true if product is in valid time window */
    public ProductTimeWindowInfo isInTimeWindow(Date aSimulationTime) {
        ProductTimeWindowInfo info;
        if (myRecord != null) {
            Date ourDate = myRecord.getTime();
            Date simulationTime = aSimulationTime;
            long seconds = (simulationTime.getTime() - ourDate.getTime()) / 1000;
            if (seconds < 0) {
                info = ProductTimeWindowInfo.TOO_NEW;
            } else if (seconds > 1 * 60) {
                info = ProductTimeWindowInfo.TOO_OLD;
            } else {
                info = ProductTimeWindowInfo.IN_WINDOW;
            }
        } else {
            info = ProductTimeWindowInfo.BAD_PRODUCT;
        }
        return info;
    }

    /** Draw our product in the 3d world 
     * @param dc  worldwind drawing context
     */
    public void draw(DrawContext dc) {

        // wouldDraw has already been called by here
        getRenderer();
        ProductRenderer pr = getRenderer();
        if (pr != null) {
            if (myDirtyRenderer) {

                // FIXME: Design problem.  Renderer needs the FilterList from handler,
                // but eventually we'll have multiple handlers..one might have a low pass filter
                // and one might not...but we only have a SINGLE renderer kept right now...
                pr.initToProduct(dc, this);
                myDirtyRenderer = false;
            }
            pr.draw(dc);
        }
    }

    /*
     * Get our product that sync in time/subtype to the given product.  We can
     * return null if we don't have a match.
     */
    public Product getSyncToProduct(Product someOtherProduct) {
        // Default is to find the latest time <= the time of the product to sync
        // to. Subclasses should override to check subtype, for instance
        Date d = someOtherProduct.getTime();

        // First try to get the latest record that matches the subtype of the other product,
        // for example...subtype of other is "0.50" we want to match to it
        IndexRecord r = SourceManager.getInstance().getRecordLatestUpToDate(
                myIndexKey, myRecord.getDataType(),
                someOtherProduct.getSubType(), d);

        if (r == null) {
            // If the subtype is not found, then try to just match time, but keep OUR subtype
            r = SourceManager.getInstance().getRecordLatestUpToDate(
                    myIndexKey, myRecord.getDataType(),
                    myRecord.getSubType(), d);
        }

        String productCacheKey = Product.createCacheKey(myIndexKey, r);
        Product ourProduct = ProductManager.CreateProduct(productCacheKey, myIndexKey, r);
        return ourProduct;
    }

    public ProductReadout getProductReadout(Point p, DrawContext dc) {

        // Readout comes from the Renderer....because we use our color trick to get it....
        // Readout has to render in order to get the value.
        ProductRenderer r = getRenderer();
        if (r != null) {
            return r.getProductReadout(p, dc);
        }
        return new ProductReadout();
    }
}

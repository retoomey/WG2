package org.wdssii.gui.features;

import gov.nasa.worldwind.render.DrawContext;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.Product.ProductTimeWindowAge;
import org.wdssii.gui.products.Product.ProductTimeWindowInfo;
import org.wdssii.gui.products.Product2DTable;
import org.wdssii.gui.products.navigators.ProductNavigator;
import org.wdssii.gui.sources.Source;
import org.wdssii.gui.sources.SourceList;

/**
 * ProductFeature handles a collection of the same product type (i.e. KTLX
 * Reflectivity). It can be duplicated by the user (say in order to have
 * original KTLX Velocity and another with SRM activated, for toggling.
 * Basically, each LINE of the product list is a different product handler...
 *
 * @author Robert Toomey
 *
 */
public class ProductFeature extends Feature {

    private static Logger log = LoggerFactory.getLogger(ProductFeature.class);
    /**
     * The current product we are using
     */
    protected Product myProduct;
    /**
     * The navigator for our product type
     */
    protected ProductNavigator myNavigator = null;
    /**
     * The 2DTable for our product type
     */
    protected Product2DTable my2DTable = null;
    /**
     * The filter list for this handler.
     */
    protected FilterList myFList = new FilterList();
    /**
     * Current text time stamp from product, if any
     */
    protected String myCurrentTimeStamp;
    /**
     * Current date format time stamp from product, if any
     */
    protected Date myCurrentTime;
    /**
     * Current subtype from product, if any
     */
    protected String myCurrentSubType;
    public static final String ProductGroup = "PRODUCTS";

    public ProductFeature(FeatureList f, Product p) {
        super(f, ProductGroup);
        setProduct(p);
    }

    /**
     * Set the product object that this feature is currently using. This would
     * be something like KTLX Reflectivity 0.50 time
     *
     * @param p theProduct
     */
    public final void setProduct(Product p) {

        // On 'first' setting of product to non-null, use the 
        // factory of the product to create the filters....
        //FIXME: actually, for now, we're just gonna create our first
        // silly filter based on radial sets...
        ///if (myProduct == null){
        //	// Add proof of concept filters:  FIXME: need generic interface
        //	myFilterList.add(new StormRMFilter());
        //	myFilterList.add(new LowCutFilter());
        //}

        // ProductHandler should be unique for each product type
        if (myProduct == null) {
            myFList.createFiltersForProduct(p);
        }
        myProduct = p;
        myFList.setColorMap(p.getColorMap());

        setKey(getKeyForProduct(p));
        setName(getIndexDatatypeString());

        // Should we hold these or pull from product directly?
        myCurrentTime = myProduct.getTime();
        myCurrentTimeStamp = myProduct.getTimeStamp();
        myCurrentSubType = myProduct.getSubType();
        setMessage("None");
    }

    public FilterList getFList() {
        if (myProduct != null) {
            myFList.setColorMap(myProduct.getColorMap());
        }
        return myFList;
    }

    public Product getProduct() {
        return myProduct;
    }

    /* else {
           
        }
     * Time we are AT. a 'Product' will always have the same time..the time it's
     * atgetVisible();
     */
    public String getTimeStamp() {
        return myCurrentTimeStamp;
    }

    /*
     * Time we are AT. a 'Product' will always have the same time..the time it's
     * at
     */
    public Date getTime() {
        return myCurrentTime;
    }

    /**
     * Subtype we are AT. a 'Product' will have a subtype setting.
     */
    public String getSubType() {
        return myCurrentSubType;
    }

    public String getProductType() {
        return (myProduct.getProductType());
    }

    /**
     *
     * @param p a product
     * @return the key we would make for given product
     */
    protected String getKeyForProduct(Product p) {
        return (String.format("%s %s", p.getIndexKey(), p.getDataType()));
    }

    /**
     * Get the display string. This is displayed by the product selection list
     *
     * @return string
     */
    protected String getIndexDatatypeString() {
        if (myProduct != null) {
            Source s = SourceList.theSources.getSource(myProduct.getIndexKey());
            if (s != null) {
                String d = myProduct.getDataType();
                String v = s.getVisibleName();
                return (String.format("%s %s", v, d));
            }
        }
        return "?";
    }

    /**
     * ProductHandlers are kept in sets. Return true if we can handle the given
     * product, otherwise a new handler will be created
     *
     * @param p
     * @return can we handle this product
     */
    public boolean canHandleThisProduct(Product p) {
        String myKey = getKey();
        boolean canHandle = myKey.equalsIgnoreCase(getKeyForProduct(p));
        return canHandle;
    }

    /**
     * Get the name shown in product selection gui list. This is typically of
     * the form "SourceName Product" such as "KTLX Reflectivity"
     */
    public String getListName() {
        return getName();  // Just use the name
    }

    /**
     * Product features have to check time as well and the visible state of
     * other product features...
     *
     * @return
     */
    @Override
    public boolean wouldRender() {

        // Handle only mode.
        // If we are not top handler, and top handler is in only mode,
        // we don't draw.
        boolean draw = false;
        boolean onlyModeHide = false;
        boolean topIsInOnlyMode = false;

        Feature f = list().getSelected(ProductGroup);
        if (f != null) {
            topIsInOnlyMode = f.getOnlyMode();
        }
        if ((this != f) && (topIsInOnlyMode)) {
            onlyModeHide = true;
        }
        ProductTimeWindowInfo info = myProduct.isInTimeWindow(list().getSimulationTime());

        if (getVisible() && !onlyModeHide) {
            if ((info.myState == ProductTimeWindowAge.IN_WINDOW) && myProduct.spaceAvailable(list())) {
                draw = true;
            }
        }
        return draw;
    }

    @Override
    public void render(DrawContext dc) {
        if (wouldRender()) {
            // This is bad actually..we should pass it down the tree in
            // case of multiple objects sharing product....
            myProduct.myCurrentFList = this.getFList();  // bleh
            myProduct.draw(dc);
        }
    }

    /**
     * Set the simulation time to ours. Usually done when we are selected, this
     * doesn't actually change any other products simulation time.
     */
    public void setToOurSimulationTime() {
        list().setSimulationTime(getTime());
        list().setSimulationTimeStamp(getTimeStamp());
    }

    /**
     * Synchronize the times of ALL product features to the selected product.
     *
     * Basically if top is at time T, all others will switch to products of time
     * <= T. Note if this is done for just selection than it can cause 'past
     * walking' where toggling between two products makes it go further and
     * further back in time.
     *
     */
    public void syncToTopProductFeature() {

        // Get selected ProductFeature, if any
        ProductFeature top = (ProductFeature) list().getSelected(ProductFeature.ProductGroup);

        // Notify any ProductFeature that's not selected to follow the selected one
        List<ProductFeature> pf = list().getFeatureGroup(ProductFeature.class);
        Iterator<ProductFeature> iter = pf.iterator();
        while (iter.hasNext()) {
            ProductFeature f = iter.next();
            if (f != top) {
                f.syncToProductFeature(top);
            }

        }

        // Now update the sim time messages of all...
        iter = pf.iterator();
        while (iter.hasNext()) {
            ProductFeature f = iter.next();
            f.updatedSimTime();
        }
    }

    /**
     * Called to sync this ProductFeature to given ProductFeature. This is so
     * other handlers can time, subtype, etc.. synchronize to the product
     * displayed by this handler. We should make our product time <= the current
     * one
     *
     *
     *
     *
     *

     *
     * @param current product to match
     */
    public void syncToProductFeature(ProductFeature current) {
        Product syncedProduct = myProduct.getSyncToProduct(current.getProduct());
        if (syncedProduct != null) {
            setProduct(syncedProduct);
        }
    }

    /**
     * Called by our product handler list whenever the sim time changes
     *
     */
    public void updatedSimTime() {

        // Update the 'message' which is based on the simulation time window
        setMessage("None");
        if (myProduct != null) {
            String d = myProduct.getTimeStamp();
            // Only the product type knows the time window for product type
            ProductTimeWindowInfo info = myProduct.isInTimeWindow(list().getSimulationTime());
            if (info != null) {
                setMessage(info.myAgeString + ":" + d);
            } else {
                setMessage("??");
            }
        }
    }

    /**
     * ProductFeature was chosen manually, by navigation button or record
     * picker, etc. Navigate time to us.
     */
    public void timeNavigateTo() {

        // Set entire FilterList simulation time to us
        setToOurSimulationTime();

        // Sync all other products to our time
        syncToTopProductFeature();
    }

    public ProductNavigator getNavigator() {

        // All Products we handle will be of the same type, thus
        // we only create the navigator once.
        if (myNavigator == null) {
            myNavigator = myProduct.createNavigator();
        }
        return myNavigator;
    }

    public Product2DTable get2DTable() {

        // All Products we handle will be of the same type, thus
        // we only create the table once.
        if (my2DTable == null) {
            my2DTable = myProduct.create2DTable();
        }
        return my2DTable;
    }
}

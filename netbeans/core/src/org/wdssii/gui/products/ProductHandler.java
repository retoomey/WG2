package org.wdssii.gui.products;

import java.util.ArrayList;
import java.util.Date;

import org.wdssii.gui.products.Product.ProductTimeWindowInfo;
import org.wdssii.gui.products.filters.DataFilter;
import gov.nasa.worldwind.render.DrawContext;
import org.wdssii.gui.products.navigators.ProductNavigator;

/**
 * The class that handles a collection of the same product type (i.e. KTLX
 * Reflectivity).  It can be duplicated by the user (say in order to have
 * original KTLX Velocity and another with SRM activated, for toggling.
 * Basically, each LINE of the product list is a different product handler...
 * 
 * There could be multiple 'product' classes existing to render different tilts
 * for example..but a global 'hide/show' flag is at this layer.
 * 
 * @author Robert Toomey
 * 
 */
public class ProductHandler { // Interface?

    /** The current product we are using */
    protected Product myProduct;
    
    /** The navigator for our product type */
    protected ProductNavigator myNavigator = null;
    
    /** The filter list for this handler.
     * Humm..we'll need to 'spawn' a new ProductHandler with different filters right? 
     */
    //protected ArrayList<DataFilter> myFilterList = new ArrayList<DataFilter>();
    protected FilterList myFList = new FilterList();
    protected String myKey;
    protected String myDisplayName;
    protected String myCurrentTimeStamp;
    protected String myCurrentSubType;
    protected Date myCurrentTime;
    protected boolean myIsVisible = true;
    protected boolean myOnlyMode = false;
    private String myMessage = "None";
    protected ProductHandlerList myHandlerList = null;

    public ProductHandler(ProductHandlerList aList, Product p) {
        myHandlerList = aList;  // All handlers are in a list.
        setProduct(p);
    }

    public void setProduct(Product p) {

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
        p.myProductHandler = this;

        myKey = getKeyForProduct(p);
        myDisplayName = p.getIndexDatatypeString();

        // Should we hold these or pull from product directly?
        myCurrentTime = myProduct.getTime();
        myCurrentTimeStamp = myProduct.getTimeStamp();
        myCurrentSubType = myProduct.getSubType();

        myMessage = "None";

    }

    @Deprecated
    public ArrayList<DataFilter> getFilterList() {
        return myFList.getFilterList();
    }

    public FilterList getFList() {
        if (myProduct != null){
             myFList.setColorMap(myProduct.getColorMap());
        }
        return myFList;
    }

    public Product getProduct() {
        return myProduct;
    }

    /**
     * The key that identifies what this handler handles. This is used to make
     * sure there is only one handler for a type. For example, KTLX Reflectivity
     * gets a handler and KTLX Velocity another.
     * 
     * 
     * @return
     */
    public String getKey() {
        return myKey;
    }

    /** Called with name from source manager delete event to see if this matches */
    // FIXME: keys will become different from shown source name
    public boolean matchesForDelete(String toDelete) {
        return (toDelete.compareTo(myProduct.getIndexKey()) == 0);
    }

    public String getTimeStamp() {
        // Time we are AT. a 'Product' will always have the same time..the time
        // it's at.
        return myCurrentTimeStamp;
    }

    public String getSubType() {
        return myCurrentSubType;
    }

    public Date getTime() {
        // Time we are AT. a 'Product' will always have the same time..the time
        // it's at.
        return myCurrentTime;
    }

    public String getProductType() {
        return (myProduct.getProductType());
    }

    /**
     * 
     * @param p
     *            a product
     * @return the key we would make for given product
     */
    protected String getKeyForProduct(Product p) {
        return (String.format("%s %s", p.getIndexKey(), p.getDataType()));
    }

    /**
     * ProductHandlers are kept in sets. Return true if we can handle the given
     * product, otherwise a new handler will be created
     * 
     * @param p
     * @return can we handle this product
     */
    public boolean canHandleThisProduct(Product p) {
        boolean canHandle = myKey.equalsIgnoreCase(getKeyForProduct(p));
        return canHandle;
    }

    /** Get the name shown in product selection gui list.  This is typically index information */
    public String getListName() {

        return myDisplayName;
    }

    public void setIsVisible(boolean flag) {
        myIsVisible = flag;
    }

    public boolean getIsVisible() {
        // return (myProduct.getIsVisible());
        return myIsVisible;
    }

    public boolean wouldDraw(ProductHandlerList handlerList) {
        // Product doesn't know about visible or only flags, since
        // those are manual settings by user.  Think of it this way,
        // the product can be in multiple windows..some with visible on
        // some with visible off.  Thus visible is in the handler, not the
        // product.

        // Handle only mode.
        // If we are not top handler, and top handler is in only mode,
        // we don't draw.
        boolean draw = false;
        boolean onlyModeHide = false;
        boolean topIsInOnlyMode = false;
        ProductHandler top = handlerList.getTopProductHandler();
        if (top != null) {
            topIsInOnlyMode = top.getOnlyMode();
        }
        if ((this != top) && (topIsInOnlyMode)) {
            onlyModeHide = true;
        }
        ProductTimeWindowInfo info = myProduct.isInTimeWindow(handlerList.getSimulationTime());

        //draw = (
        //	getIsVisible() &&  								// Is the physical visible flag checked on?
        //	!onlyModeHide && 								// The top product isn't in only mode
        //	(info == ProductTimeWindowInfo.IN_WINDOW) && 	// Product is in visible time window
        //	(myProduct.spaceAvailable(handlerList))			// No physical conflict with another showing product
        //);


        if (getIsVisible() && !onlyModeHide) {
            //return(myProduct.wouldDraw(handlerList));

            if ((info == ProductTimeWindowInfo.IN_WINDOW) && myProduct.spaceAvailable(handlerList)) {
                draw = true;
            }
        }
        //System.out.println("WOULD DRAW IS "+draw);
        return draw;
    }

    public void draw(DrawContext dc) { // , ProductHandlerList handlerList) {
        // System.out.println("Handler called draw on "+getKeyForProduct(myProduct));
        myProduct.myProductHandler = this;
        myProduct.draw(dc); // , handlerList, this);
    }

    /**
     * Called to sync this handler to given handler. This is so other handlers
     * can time, subtype, etc.. synchronize to the product displayed by this
     * handler.
     * 
     * @param current
     */
    public void syncToHandler(ProductHandler current) {
        Product syncedProduct = myProduct.getSyncToProduct(current.getProduct());
        if (syncedProduct != null) {
            setProduct(syncedProduct);
        }
    }

    public boolean getOnlyMode() {
        return myOnlyMode;
    }

    public void setOnlyMode(boolean flag) {
        myOnlyMode = flag;
    }

    public String getMessage() {
        return myMessage;
    }

    /** Called by our product handler list whenever the sim time changes
     * 
     */
    public void updatedSimTime() {

        // Update the 'message' which is based on the simulation time window
        myMessage = "None";
        if (myProduct != null) {
            // Only the product type knows the time window for product type
            ProductTimeWindowInfo info = myProduct.isInTimeWindow(myHandlerList.getSimulationTime());

            switch (info) {
                case IN_WINDOW:
                    myMessage = "In window";
                    break;
                case BAD_PRODUCT:
                    myMessage = "Invalid";
                    break;
                case TOO_NEW:
                    myMessage = "In future";
                    break;
                case TOO_OLD:
                    myMessage = "> 1 min old";
                    break; // FIXME: based on product info
                default:
                    myMessage = "??";
                    break;
            }
        }
    }
    /*
    public boolean isInTimeWindow(ProductHandlerList aList)
    {
    boolean inWindow = false;
    if (myRecord != null){
    Date ourDate = myRecord.getTime();
    Date simulationTime = aList.getSimulationTime();
    long seconds = (simulationTime.getTime() - ourDate.getTime())/1000;
    
    // Message has to be higher up, depends upon 
    myMessage = "Testing";
    if (seconds < 0){
    myMessage = "In future";
    }else if (seconds > 1*60){
    myMessage = "> 1 min old";
    }else{
    myMessage = "In window";
    inWindow = true;
    }
    }
    return inWindow;
    }
     */

    public ProductNavigator getNavigator() {
        
        // All Products we handle will be of the same type, thus
        // we only create the navigator once.
        if (myNavigator == null){
            myNavigator = myProduct.createNavigator();
        }
        return myNavigator;
    }
}

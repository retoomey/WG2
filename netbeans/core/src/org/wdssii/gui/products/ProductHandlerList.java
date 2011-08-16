package org.wdssii.gui.products;

import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.render.DrawContext;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.SourceManager;
import org.wdssii.gui.CommandManager.NavigationAction;
import org.wdssii.gui.CommandManager.NavigationMessage;
import org.wdssii.gui.views.EarthBallView;
import org.wdssii.index.HistoricalIndex;
import org.wdssii.index.IndexRecord;

/**
 * ProductHandlerList contains a set of ProductHandlers Each productHandler is
 * typically navigated to a particular product. The list has a simulation time,
 * or time the display would be at if it is using this list. Basically, this
 * contains everything required for what you'd think of as a collection of
 * products in a window.
 * 
 * FIXME: definitely need to make this thing 100% thread safe
 * 
 * @author Robert Toomey
 * 
 *         Design: load product(p) wm.getProductHandlerList().loadProduct(p) -->
 *         wm.notify(loadedProduct)
 * 
 *         delete selected wm.getProductHandlerList().deleteSelected()
 *         -->wm.notify(deleteSelected)
 * 
 *         Nav wm.getProductHandlerList().navigationMessage();
 */
public class ProductHandlerList {

    @SuppressWarnings("unused")
    private static Log log = LogFactory.getLog(ProductHandlerList.class);
    /** Key used to request the current top product */
    public static final String TOP_PRODUCT = "TOP";
    // The list of handlers in order of sorted key
    protected ArrayList<ProductHandler> myProductHandlers = new ArrayList<ProductHandler>();
    // The list of handlers in order of drawing (selection moves to bottom)
    protected ArrayList<ProductHandler> myDrawHandlers = new ArrayList<ProductHandler>();
    // The last of the draw handlers, or the selected product
    protected ProductHandler myTopProductHandler;
    // We keep the 'stamp' which is hidden/made in the wdssii core
    protected String mySimulationTimeStamp;
    // We keep the date from the product
    protected Date mySimulationTime;
    
    /** The internal keyname, unique for this */
    private String myKeyName;
    
    /** The current visible name of this in the GUI */
    private String myVisibleName;
    
    public ProductHandlerList(String key, String visible){
        myKeyName = key;
        myVisibleName = visible;
    }
    
    public String getKey(){
        return myKeyName;
    }
    
    public String getVisibleName(){
        return myVisibleName;
    }
    
    public void setVisibleName(String visible){
        myVisibleName = visible;
    }
    
    // User direct navigation routines --------------------------------------
    /**
     * Called by the ProductLoadCommand from the record picker
     * 
     * @param aProduct
     */
    public void recordPickerSelectedProduct(String indexName, String datatype, String subtype, Date time) {
        // ----------------------------------------------------------------
        // Try to create default product from selections.
        SourceManager manager = SourceManager.getInstance();
        HistoricalIndex anIndex = SourceManager.getIndexByName(indexName);

        if (anIndex == null) {
            System.out.println("ProductHandlerList: Index null, cannot create new product");
            return;
        }

        IndexRecord aRecord = anIndex.getRecord(datatype, subtype, time);
        if (aRecord == null){
            System.out.println("ProductHandlerList: Record is null, cannot create new product");
            return;
        }
        aRecord.setSourceName(manager.getIndexName(anIndex));
        String productCacheKey = Product.createCacheKey(indexName, aRecord);
        Product aProduct = ProductManager.CreateProduct(productCacheKey, indexName, aRecord);

        // ----------------------------------------------------------------
        // Load product into our list, creating a handler for it
        ProductHandler theHandler = loadProduct(aProduct);

        if (theHandler != null) {
            // ------------------------------------------------------------
            // When a product is picked in record picker, move the simulation
            // time to the selected product time, sync all other products
            // to this product's time.
            mySimulationTimeStamp = theHandler.getTimeStamp();
            mySimulationTime = theHandler.getTime();

            // Select this product that was chosen
            selectProductHandler(theHandler);

            // Synchronize all others time to <= us
            syncToTopProductHandler();
            simTimeChangeNotify();
        } else {
            // This "shouldn't" happen. Lol
        }
    }

    /*
     * This is called when one of the buttons is actively clicked to navigate
     */
    public void navigationAction(NavigationAction nav) {
        Product p = null;
        if (myTopProductHandler != null) {
            p = myTopProductHandler.getProduct();
        }

        if (p == null) {
            System.out.println("ProductHandlerList: Can't move from a null record, no reference");
        } else {
            Product aProduct = p.getProduct(nav.message());

            loadProduct(aProduct);

            // ------------------------------------------------------------
            // When a product is picked by navigation, move the simulation
            // time to the selected product time, sync all other products
            // to this product's time.
            mySimulationTimeStamp = myTopProductHandler.getTimeStamp();
            mySimulationTime = myTopProductHandler.getTime();
            // Synchronize all others time to <= us
            syncToTopProductHandler();
            simTimeChangeNotify();

            //CommandManager.getInstance().productChangeNotify();	
            // ------------------------------------------------------------
        }
    }

    public void navigate(NavigationMessage message) {
        Product p = null;
        if (myTopProductHandler != null) {
            p = myTopProductHandler.getProduct();
        }

        if (p == null) {
            System.out.println("ProductHandlerList: Can't move from a null record, no reference");
        } else {
            Product aProduct = p.getProduct(message);
            loadProduct(aProduct);

            // ------------------------------------------------------------
            // When a product is picked by navigation, move the simulation
            // time to the selected product time, sync all other products
            // to this product's time.
            mySimulationTimeStamp = myTopProductHandler.getTimeStamp();
            mySimulationTime = myTopProductHandler.getTime();
            // Synchronize all others time to <= us
            syncToTopProductHandler();
            simTimeChangeNotify();

            //CommandManager.getInstance().productChangeNotify();	
            // ------------------------------------------------------------
        }
    }

    private void simTimeChangeNotify() {
        for (ProductHandler current : myProductHandlers) {
            current.updatedSimTime();
        }
    }

    /** The code that syncs time/subtype/etc. of all the other handlers to the selected one
     * 
     */
    private void syncToTopProductHandler() {
        // The first 'sync' of product attempt. Send a message to each
        // product
        for (ProductHandler current : myProductHandlers) {
            if (current != myTopProductHandler) {
                current.syncToHandler(myTopProductHandler);
            }
        }
        // GUI update done higher up
        //CommandManager.getInstance().productChangeNotify();	
    }

    /**
     * We check each product handler we currently have to see if any of them can
     * handle this product. Otherwise, we ask the product to create a new
     * instance of the handler for it. This gets added to our set and popped to
     * top This is a generic load without gui updates.
     * 
     * @param aProduct
     */
    public ProductHandler loadProduct(Product aProduct) {
        if (aProduct == null) {
            return null;
        }

        boolean found = false;
        Iterator<ProductHandler> iter = myProductHandlers.iterator();
        ProductHandler theHandler = null;

        while (iter.hasNext()) {
            ProductHandler current = iter.next();
            if (current.canHandleThisProduct(aProduct)) {
                // System.out.println("Found handler already...needs work");
                theHandler = current;
                found = true;
                break;
            }
        }
        if (!found) {
            ProductHandler newOne = aProduct.getNewProductHandler(this);
            myProductHandlers.add(newOne);
            myDrawHandlers.add(newOne);
            theHandler = newOne;
            Collections.sort(myProductHandlers,
                    new Comparator<ProductHandler>() {

                        @Override
                        public int compare(ProductHandler o1, ProductHandler o2) {
                            return o1.getKey().compareTo(o2.getKey());
                        }
                    });

            System.out.println("Created NEW handler for product, size now is "
                    + myProductHandlers.size());
        }

        // Move handler to this product
        theHandler.setProduct(aProduct);
        return theHandler;
    }

    public Date getSimulationTime() {
        return mySimulationTime;
    }

    public String getSimulationTimeStamp() {
        return mySimulationTimeStamp;
    }

    public boolean isProductSelected() {
        return (myTopProductHandler != null);
    }

    /** Return the 'top' or selected product handler.  Usually you can use the
     * getProductHandler passing in ProductHandlerList.TOP_PRODUCT
     */
    public ProductHandler getTopProductHandler() {
        return myTopProductHandler;
    }

    /** Return handler number in draw order.  */
    public ProductHandler getProductHandlerNumber(int i) {
        ProductHandler h = null;
        try {
            h = myProductHandlers.get(myDrawHandlers.size() - i - 1);
        } catch (IndexOutOfBoundsException e) {
            // Just let it be null
        }
        return h;
    }

    public ProductHandler getProductHandler(String handlerKey) {
        // Find handler with given key
        ProductHandler h = null;
        for (ProductHandler p : myProductHandlers) {
            if (p.getKey().compareTo(handlerKey) == 0) {
                h = p;
                break;
            }
        }
        if (h == null) {
            if (TOP_PRODUCT.compareTo(handlerKey) == 0) {
                h = myTopProductHandler;
            }
        }
        return h;
    }

    /** Called from a ProductSelectCommand */
    public void selectProductHandler(String handlerKey) {
        selectProductHandler(getProductHandler(handlerKey));
    }

    /*
     * // @deprecated public void selectProductHandler(int index) { // FIXME:
     * check index range // Product handlers not draw (selecting based on listed
     * index, usually alphabetical)
     * selectProductHandler(myProductHandlers.get(index)); }
     */
    public void selectProductHandler(ProductHandler theSelection) {
        // FIXME: Bring this selection to the bottom of the draw handlers.
        if (myTopProductHandler != theSelection) {
            myTopProductHandler = theSelection;
            // System.out.println("SELECTED: "+myTopProductHandler.getKey());

            // Put this selected handler at the BOTTOM of the draw list...so it
            // draws last
            // and on top of everything else
            myDrawHandlers.remove(myTopProductHandler);
            myDrawHandlers.add(myTopProductHandler); // Now at end of list (last
            // drawn)
        }
        // Don't fire a select command Every method calling this will update GUI at end.
    }

    /** Toggle the visibility of a handler, usually in response to the user clicking the visible
     * checkbox in the GUI */
    public void setVisibleHandler(String handlerKey, boolean flag) {
        setVisibleHandler(getProductHandler(handlerKey), flag);
    }

    /*
     * public void setVisibleHandler(int index, boolean flag) { // FIXME: check
     * index range setVisibleHandler(myProductHandlers.get(index), flag); }
     */
    public void setVisibleHandler(ProductHandler theSelection, boolean flag) {
        // FIXME: Bring this selection to the bottom of the draw handlers.
        // System.out.println("SET TO: "+flag+" "+theSelection.getKey());
        CommandManager.getInstance().setProductHandlerVisible(theSelection,
                flag);
    }

    // Humm iterate through each product, calling it's renderer? Is this
    // the place for this stuff?
    public void drawProducts() {
    }

    public Iterator<ProductHandler> getIterator() {
        return myProductHandlers.iterator();
    }

    public ListIterator<ProductHandler> getDrawIterator() {
        return myDrawHandlers.listIterator();
    }

    public String getReadout(PositionEvent event) {
        String readout = "None";
        if (myTopProductHandler != null) {
            Product current = myTopProductHandler.getProduct();
            if (current != null) {
                //Position p = event.getPosition();
                Point point = event.getScreenPoint();
                readout = String.format("(%d, %d)", point.x, point.y);
                EarthBallView earth = CommandManager.getInstance().getEarthBall();
                if (earth != null) {
                    earth.getColor(point.x, point.y);
                }
                //readout = current.getReadout(p.getLatitude().getDegrees(),
                //		p.getLongitude().getDegrees(), 
                //		p.getHeight().get);
            }
        }
        return readout;
    }

    public ColorMap getCurrentColorMap() {
        for (ProductHandler current : myDrawHandlers) {
            if (current.wouldDraw(this)) {
                // Just the first color map for now at least
                return (current.getProduct().getColorMap());
            }
        }
        return null;
    }

    // Render this set of product handlers.
    public void draw(DrawContext dc) {
        //log.info("Product handler list draw");
        for (ProductHandler current : myDrawHandlers) {
            if (current.wouldDraw(this)) {
                current.draw(dc);
            }
        }
    }

    public void setOnlyMode(String handlerKey, boolean flag) {
        setOnlyMode(getProductHandler(handlerKey), flag);
    }

    public void setOnlyMode(ProductHandler h, boolean flag) {
        h.setOnlyMode(flag);
        //CommandManager.getInstance().productChangeNotify();
    }

    // Called from ProductDeleteCommand
    public ProductHandler deleteSelectedProduct() {
        // The selected product should be the top handler...
        if (myTopProductHandler != null) {
            myDrawHandlers.remove(myTopProductHandler);
            myProductHandlers.remove(myTopProductHandler);
            if (myDrawHandlers.size() != 0) {
                // Make the new top the last drawn (the top);
                myTopProductHandler = myDrawHandlers.get(myDrawHandlers.size() - 1);
            } else {
                myTopProductHandler = null; // FIXME: is this ok? 'should' be
            }
        }
        selectProductHandler(myTopProductHandler);
        return myTopProductHandler;
    }

    // Called from ProductDeleteCommand (with a source filter parameter)
    public void deleteProductsMatchingSource(String toDelete) {
        System.out.println("Delete product matching source " + toDelete);

        ArrayList<ProductHandler> deleteList = new ArrayList<ProductHandler>();
        for (ProductHandler current : myProductHandlers) {
            if (current.matchesForDelete(toDelete)) {
                deleteList.add(current);
            }
        }
        for (ProductHandler current : deleteList) {
            myDrawHandlers.remove(current);
            myProductHandlers.remove(current);

        }

        // duplicate code with delete selected
        if (myDrawHandlers.size() != 0) {
            // Make the new top the last drawn (the top);
            myTopProductHandler = myDrawHandlers.get(myDrawHandlers.size() - 1);
        } else {
            myTopProductHandler = null; // FIXME: is this ok? 'should' be
        }

        selectProductHandler(myTopProductHandler);
        //	CommandManager.getInstance().productChangeNotify();

    }
    //@Override
    //public Iterator<ProductHandler> iterator() {
    //	return myProductHandlers.iterator();
    //}
}

package org.wdssii.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wdssii.core.ConfigurationException;
import org.wdssii.core.W2Config;
import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.builders.BuilderFactory;
import org.wdssii.gui.PreferencesManager.PrefConstants;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductHandler;
import org.wdssii.gui.products.ProductHandlerList;
import org.wdssii.gui.products.ProductVolume;
import org.wdssii.index.IndexRecord;

/**
 * --Maintains a set of color maps by product name (color map cache FIXME: Move to generic cache)
 * --Maintains the product 'info', colors, etc. Singleton
 * --Maintains the product factory creating the default product for an IndexRecord
 * --Maintains the product LRU cache (FIXME: Move to generic cache)
 * --Maintains the product chart factory list
 * 
 * @author Robert Toomey
 * 
 */
public class ProductManager implements Singleton {

    private static Log log = LogFactory.getLog(ProductManager.class);
    public static final String DEFAULTS = "defaults";
    private static ProductManager instance = null;
    final public static int MIN_CACHE_SIZE = 50;
    final public static int MAX_CACHE_SIZE = 500;
    private int myProductCacheSize = MIN_CACHE_SIZE;  // The cache size (number of products we hold)
    // FIXME: Color map might be part of product info object?
    TreeMap<String, ColorMap> myColorMaps = new TreeMap<String, ColorMap>();
    TreeMap<String, ProductDataInfo> myProductInfo = new TreeMap<String, ProductDataInfo>();
    ProductDataInfo myDefaults = new ProductDataInfo();
    // Cache stuff
    TreeMap<String, Product> myProductCache = new TreeMap<String, Product>();
    ArrayList<Product> myLRUStack = new ArrayList<Product>();  //0, 1, 2, ... LRU product (at end of list)

    // Product charts	
    /** Set the size of the product cache */
    public void setCacheSize(int size) {
        if ((size >= MIN_CACHE_SIZE) && (size <= MAX_CACHE_SIZE)) {
            myProductCacheSize = size;
            PreferencesManager p = PreferencesManager.getInstance();
            p.setValue(PrefConstants.PREF_cacheSize, myProductCacheSize);
            trimCache(myProductCacheSize);
            CommandManager.getInstance().cacheManagerNotify(); // added product it changed
        }
    }

    /** Get the size of the product cache */
    public int getCacheSize() {
        return myProductCacheSize;
    }

    /** Trim cache down to the MIN_CACHE_SIZE */
    private void trimCache(int toSize) {

        // Don't trim less than zero
        if (toSize < 0) {
            toSize = 0;
        }
        try {
            while (true) {
                // Drop oldest from stack until we've got space...
                if (myLRUStack.size() > toSize) {
                    Product oldestProduct = myLRUStack.get(0); // Oldest
                    myLRUStack.remove(0);
                    myProductCache.remove(oldestProduct.getCacheKey());
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception purging cache element " + e.toString());
        }
    }

    /** Trim all products from cache matching a given index key */
    public int trimCacheMatchingIndexKey(String indexKey) {
        int removed = 0;
        try {
            ArrayList<Product> toDelete = new ArrayList<Product>();
            for (Product p : myLRUStack) {
                if (p.getIndexKey().compareTo(indexKey) == 0) {
                    toDelete.add(p);
                    myProductCache.remove(p.getCacheKey());
                    removed++;
                }
            }
            myLRUStack.removeAll(toDelete);
        } catch (Exception e) {
            System.out.println("Exception purging cache for index " + e.toString());
        }
        return removed;
    }

    /**
     * Database node which holds information for a data type by name
     * 
     * @author Robert Toomey FIXME: separate class file? It's static-inner so
     *         this only affects namespace, however this class is meaningless
     *         without the manager.
     */
    public static class ProductDataInfo {

        private String myName = DEFAULTS;
        private Color myListColor = null; // Null means system default theme
        // color (not set)
        private boolean myVisibleInList = true;
        private String myListName = DEFAULTS;
        private ColorMap myColorMap = null;

        public void copyFrom(ProductDataInfo another) {
            myName = another.myName;
            myListColor = another.myListColor;
            myVisibleInList = another.myVisibleInList;
            myListName = another.myListName;
        }

        public boolean isDefault() {
            return myName.equals(DEFAULTS);
        }

        public String getName() {
            return myName;
        }

        public void setName(String name) {
            myName = name;
        }

        public Color getListColor() {
            return myListColor;
        }

        public void setColor(Color c) {
            myListColor = c;
        }

        public boolean getVisibleInList() {
            return myVisibleInList;
        }

        public void setVisibleInList(boolean flag) {
            myVisibleInList = flag;
        }

        public String getListName() {
            return myListName;
        }

        public void setListName(String name) {
            myListName = name;
        }

        // FIXME: this will be the real color map
        public ColorMap getColorMap() {
            return myColorMap;
        }

        public void setColorMap(ColorMap theColorMap) {
            myColorMap = theColorMap;
        }
    }

    /**
     * SAME FUNCTION AS ColorMap (xml util class) FIXME parse the text of a
     * color number field into an integer value
     * 
     * @param textOfNumber
     *            the raw text of number, such as 0xFF or 45
     */
    protected static int parseColorValue(String textOfNumber) {
        int value = 255;
        try {
            if (textOfNumber.toLowerCase().startsWith("0x")) {
                textOfNumber = textOfNumber.substring(2);
                value = Integer.parseInt(textOfNumber, 16);
            } else {
                value = Integer.parseInt(textOfNumber);
            }
        } catch (NumberFormatException e) {
            // Recover...we'll just return 1.0;
        }
        return value;
    }

    /**
     * Parse a 'data' line in the xml. If ProductDataInfo is null, fill in our
     * default settings, otherwise fill the ProductDataInfo
     * 
     * @param fillMe
     * @param aProductXML
     */
    protected void parseDataXML(ProductDataInfo ref, Element aDataXML,
            String productName) {
        // NodeList nodes = aDataXML.getElementsByTagName("data");
        // Factory based on 'name'
        // data name = "listColor" a Color
        // data name = "listVisible" a boolean
        // data name = "abbreviation" a String
        String dataName = aDataXML.getAttribute("name");

        ref.setName(productName);
        // System.out.println("PARSE DATAXML NAME IS "+dataName);
        // <data name="listColor"
        if (dataName.equalsIgnoreCase("listColor")) {
            Color color = myDefaults.getListColor(); // default value;
            try {
                Integer r = parseColorValue(aDataXML.getAttribute("r"));
                Integer g = parseColorValue(aDataXML.getAttribute("g"));
                Integer b = parseColorValue(aDataXML.getAttribute("b"));
                color = new Color(r, g, b);
            } catch (Exception e) {
                // log.warn("Missing or incorrect colors in product info");
            }
            ref.myListColor = color;

            // <data name = "listVisible"
        } else if (dataName.equalsIgnoreCase("listVisible")) {
            // System.out.println("Got list visible flag");
            boolean flag = myDefaults.getVisibleInList(); // default value
            String flagText = aDataXML.getAttribute("flag");
            if ((flagText != null)) {
                flag = flagText.equalsIgnoreCase("1");
            }
            // System.out.println("Set "+ref.toString() + " to "+flag);
            ref.setVisibleInList(flag);

            // <data name = "listText"
        } else if (dataName.equalsIgnoreCase("listText")) {
            // System.out.println("FOUND LISTTEXT FOR "+productName);
            String s = aDataXML.getAttribute("value");
            if (s != null) {
                ref.setListName(s);
            } else {
                ref.setListName(productName);
            }
            // System.out.println("S is "+s);
        } else {
        }
    }

    /**
     * Private to prevent creation except by getInstance method
     */
    private ProductManager() {
        // Exists only to defeat instantiation.
        // Do nothing here! Do it in singletonManagerCallback, this way
        // the singleton initialization order is controlled.
    }

    /**
     * Called by singleton manager after all singletons are created, this
     * controls the initialization order of all the singletons.
     */
    @Override
    public void singletonManagerCallback() {
        // load database information needed by application
        loadProductInformation();

        // Load cache size from preferences
        PreferencesManager p = PreferencesManager.getInstance();
        myProductCacheSize = p.getInteger(PrefConstants.PREF_cacheSize);
    }

    /**
     * @return the singleton for the manager
     */
    public static ProductManager getInstance() {
        if (instance == null) {
            instance = new ProductManager();
            SingletonManager.registerSingleton(instance);
        }
        return instance;
    }

    public boolean hasColorMap(String name) {
        ColorMap aColorMap = myColorMaps.get(name);
        return (aColorMap != null);
    }

    /**
     * @param name
     *            the name of the color map to return such as 'Reflectivity'
     * @return the color map
     */
    public ColorMap getColorMap(String name) {
        // Not sure we should allow others to 'get' a color map.
        ColorMap aColorMap = myColorMaps.get(name);
        if (aColorMap == null) {
            aColorMap = loadColorMap(name);
        }
        if (aColorMap == null) {
            // Ok, create a fake color map here...
        }
        return aColorMap;
    }

    /**
     * @param name
     *            the name of the color map to return such as 'Reflectivity'
     * @return a new color map
     */
    private ColorMap loadColorMap(String name) {
        // System.out.println("Attempt to load colormap '"+name+"'");
        ColorMap aColorMap = null;
        // String s = System.getProperty("user.home");
        // System.out.println("user home is " +s);
        Element colormapXML = null;

        // Try twice, the file.xml (preferred), and without .xml
        try {
            colormapXML = W2Config.getFileElement("colormaps/" + name + ".xml");
        } catch (ConfigurationException c) {
            try {
                colormapXML = W2Config.getFileElement("colormaps/" + name);
            } catch (ConfigurationException e) {
                System.out.println("Unable to load colormap for " + name
                        + ", will generate based on values");
            }
        }

        // Store color map into cache
        if (colormapXML != null) {
            // System.out.println("Colormap:" + name);
            aColorMap = new ColorMap();
            aColorMap.initFromXML(colormapXML);
            storeNewColorMap(name, aColorMap);
        } else {
            // FIXME: Some sort of constructed colop map from the data values?
        }
        return aColorMap;
    }

    /** Store a new colormap, with option to force store it over an old one.
     * @param colorMapName
     * @param map
     * @param force
     * @return true on success
     */
    public boolean storeNewColorMap(String colorMapName, ColorMap map, boolean force) {
        boolean success = false;
        if (force || !myColorMaps.containsKey(colorMapName)) {
            myColorMaps.put(colorMapName, map);
            success = true;
        }
        return success;
    }

    public boolean storeNewColorMap(String colorMapName, ColorMap map) {
        return (storeNewColorMap(colorMapName, map, false));
    }

    public ProductDataInfo getProductDataInfo(String name) {
        ProductDataInfo d = myProductInfo.get(name);
        if (d == null) {
            // If we don't have one, create one with defaults
            d = myDefaults;
            d = new ProductDataInfo();
            d.copyFrom(myDefaults);
            d.setName(name);
            d.setListName(name);
            // FIXME: Could store this object to avoid recreation.
            // Will only work if defaults never change on the fly
        }
        return d;
    }

    /**
     * load the information about products. Current this includes the
     * prodTextColors.xml file, which gives us the color, name, and abbreviation
     * name of products.
     */
    private void loadProductInformation() {
        // XML FORMAT:
        // <products>
        // <!-- Default values for all products (value used if missing)
        // <data name="field" ... value attributes depending on field>
        // <data name="field" ...
        //
        // <product name = "AzShear">
        // <data name="field" ... value attributes depending on field>
        // </product>
        //
        // </products>
        Element productInfoXML = null;
        try {
            productInfoXML = W2Config.getFileElement("misc/productinfo.xml");
            //	System.out
            //			.println(">>>>>>>>>>>>LOADED THE PRODUCT INFO DATABASE>>>>YAY!!!");
        } catch (ConfigurationException c) {
            //	System.out
            //			.println("Couldn't log product information, using defaults.");
        }

        if (productInfoXML != null) {

            // Go through all the tags directly under <products>
            // non-recursively. Any
            // 'data' tags at top level here are defaults for all products
            NodeList defaults = productInfoXML.getChildNodes();
            // System.out.println("There are "+defaults.getLength()+
            // " child nodes for first");
            for (int j = 0; j < defaults.getLength(); j++) {
                Node some = defaults.item(j);
                String nodeName = some.getNodeName();

                // If we find a 'data' node
                if (nodeName == "data") {
                    if (some instanceof Element) {
                        Element aDataXML = (Element) (some);
                        parseDataXML(myDefaults, aDataXML, DEFAULTS); // fill
                        // defaults
                        // from
                        // data
                        // lines
                    }
                } else {
                    // ignore all other tags
                }
            }

            // For each 'product' tag, create a new ProductDataInfo and fill it
            // with 'data' fields
            NodeList products = productInfoXML.getElementsByTagName("product");
            for (int i = 0; i < products.getLength(); i++) {

                // <product subtag
                Element aProductXML = (Element) products.item(i);

                // <product name ="something"
                String aName = aProductXML.getAttribute("name");
                if (aName != null) {
                    ProductDataInfo product = new ProductDataInfo();
                    product.copyFrom(myDefaults);
                    product.setListName(aName);
                    product.setName(aName);
                    // Now just get the data items inside the product tag, these
                    // will override any defaults
                    NodeList dataItems = aProductXML.getElementsByTagName("data");
                    for (int j = 0; j < dataItems.getLength(); j++) {
                        Element aDataXML = (Element) dataItems.item(j);
                        parseDataXML(product, aDataXML, aName);
                    }
                    // Now store this product data info under the product name
                    myProductInfo.put(aName, product);
                }
            }
        }
    }

    // External create product method (convenience routine)
    public static Product CreateProduct(
            String productCacheKey,
            String anIndex, IndexRecord init) {
        return (ProductManager.getInstance().getProduct(productCacheKey, anIndex, init));
    }

    public static ArrayList<Product> getArrayList() {
        return ProductManager.getInstance().myLRUStack;
    }

    private Product getProduct(String productCacheKey, String anIndex, IndexRecord init) {

        Product theProduct = null;
        if (productCacheKey != null) {

            theProduct = myProductCache.get(productCacheKey);

            // Product not in cache, create it and add it to cache
            if (theProduct == null) {
                //System.out.println("Product not in cache: "+productCacheKey);

                theProduct = ProductManager.getInstance().makeProduct(anIndex, init);
                if (theProduct != null) {
                    theProduct.setCacheKey(productCacheKey);

                    // First, check cache size and trim to maxsize -1 before adding new product
                    // Problem with this is if cache size can change on the fly we need to trim
                    // to the new lower size actually.  This only works with new cache == old
                    trimCache(myProductCacheSize - 1);

                    myProductCache.put(productCacheKey, theProduct);
                    myLRUStack.add(theProduct);
                    CommandManager.getInstance().cacheManagerNotify(); // added product it changed
                } else {
                    log.error("Wasn't able to create the product for data.  Nothing will show");
                }

                // Product already found in cache.  Raise it in the LRU to top
            } else {
                //System.out.println("Product is IN cache: "+productCacheKey);
                // Move item to top of LRU cache...
                myLRUStack.remove(theProduct);  // Move product from inside stack to 'top'
                myLRUStack.add(theProduct);
                theProduct.updateDataTypeIfLoaded();
                CommandManager.getInstance().cacheManagerNotify(); // Raised product it changed
            }

        }
        return (theProduct);
    }

    // Internal create product method
    private Product makeProduct(String anIndex, IndexRecord init) {
        Product p = null;
        //DataType dt = null;
        DataRequest dr = null;

        if (init == null) {
            return null;
        }
        p = new Product();  // Ok now we try creating a  generic product for all data types so they can lazy load
        try {
            dr = BuilderFactory.createDataRequest(init);
            p.superInit(dr, anIndex, init);
        } catch (Exception e) {
            log.error("Exception loading data..." + e.toString());
        }
        /*
        if (init == null){ return null; }
        String type = init.getDataType();
        try {
        
        // This spawns a worker thread to do the initial loading attempt....
        //dt = BuilderFactory.createDataType(init);
        dr = BuilderFactory.createDataRequest(init);
        //boolean check = dr.isReady();
        //while(!check){
        //	log.info("Data type not ready yet...");
        //	Thread.sleep(5000);
        //	check = dr.isReady(); // Force recheck (avoiding using violatile isReady)
        //}
        dt = dr.getDataType();
        
        String dataTypeName = dt.getClass().getSimpleName(); // Such as
        // 'RadialSet'
        // from
        // org.wdssii.core.RadialSet
        // FIXME: Can we use class name of base class to avoid hardcoding
        // string?
        // Product.class.getName() --> remove .Product add
        // dateTypeName+"Product". Less breakable
        String createIt = "org.wdssii.gui.products." + dataTypeName
        + "Product";
        log
        .info("Datatype/classname is " + type + "/" + dataTypeName
        + "/");
        log.info("Looking for class " + createIt);
        
        Class<?> c = null;
        
        //boolean foundByName = false;
        try {
        c = Class.forName(createIt);
        // p = (Product)((Object) c);
        p = (Product) c.newInstance();
        //	foundByName = true;
        } catch (Exception e) {
        log.warn("No special class for datatype, using default: '"
        + createIt + "' " + e.toString());
        p = new Product();
        }
        
        //if (foundByName) {
        p.init(anIndex, init, dt);
        log.info("Generated product object is "+p);
        return p;
        //}
        } catch (Exception e) { // Catching blindly considered bad practice.
        // Would rather catch and have display keep
        // functioning
        log.warn("Couldn't load product for some reason " + e.toString()
        + ", creating null product");
        
        }
        p = new Product();
        p.init(anIndex, init, dt);
         */
        return p;
    }

    // Called when a product is clicked in the record picker
    // Notice this is using a single record for a product
	/*public static boolean RecordPickerSelection(String indexName, 
    String datatype, String subtype, Date time) {
    System.out.println("*****************************RECORD PICKED");	
    // Just pass it on to the current product handler list
    ProductHandlerList h = CommandManager.getInstance()
    .getProductOrderedSet();
    h.recordPickerSelectedProduct(indexName, datatype, subtype, time);
    
    return true; // FIXME:
    }
     */
    public void clearProductCache() {
        myProductCache.clear();
        myLRUStack.clear();
        CommandManager.getInstance().cacheManagerNotify(); // added product it changed
    }

    /** Get the current volume of the top selected product in the product selector
     */
    public static ProductVolume getCurrentVolumeProduct(boolean virtual) {
        ProductHandlerList phl = CommandManager.getInstance().getProductOrderedSet();
        ProductVolume volume = null;
        if (phl != null) {
            ProductHandler tph = phl.getTopProductHandler();
            if (tph != null) {
                Product product = tph.getProduct();
                if (product != null) {
                    volume = product.getProductVolume(virtual);
                }
            }
        }
        // Use a 'fake' volume is null...
        if (volume == null) {
            volume = new ProductVolume(); // steal the root class for now.
        }
        return volume;
    }

    /** Get the current volume of the top selected product in the product selector
     */
    public static ProductVolume getCurrentVolumeProduct(String key, boolean virtual) {
        ProductHandlerList phl = CommandManager.getInstance().getProductOrderedSet();
        ProductVolume volume = null;
        if (phl != null) {
            ProductHandler tph = phl.getProductHandler(key);
            if (tph != null) {
                Product product = tph.getProduct();
                if (product != null) {
                    volume = product.getProductVolume(virtual);
                }
            }
        }
        // Use a 'fake' volume is null...
        if (volume == null) {
            volume = new ProductVolume(); // steal the root class for now.
        }
        return volume;
    }

    /** Get the current volume of the top selected product in the product selector
     * this is for testing only, might go away....
     */
    public static ProductVolume getCurrentVolumeProduct(int i, boolean virtual) {
        ProductHandlerList phl = CommandManager.getInstance().getProductOrderedSet();
        ProductVolume volume = null;
        if (phl != null) {
            ProductHandler tph = phl.getProductHandlerNumber(i);
            if (tph != null) {
                Product product = tph.getProduct();
                if (product != null) {
                    volume = product.getProductVolume(virtual);
                }
            }
        }
        // Use a 'fake' volume is null...
        if (volume == null) {
            volume = new ProductVolume(); // steal the root class for now.
        }
        return volume;
    }

    /** Get the latest N records */
    public Product getCurrentTopProduct() {

        ProductHandlerList p = CommandManager.getInstance().getProductOrderedSet();
        ProductHandler ph = p.getTopProductHandler();
        Product prod = null;
        if (ph != null) {
            prod = ph.getProduct();
        }
        return prod;
    }
}
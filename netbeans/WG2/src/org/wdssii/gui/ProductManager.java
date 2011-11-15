package org.wdssii.gui;

import gov.nasa.worldwind.event.PositionEvent;
import java.awt.Color;
import java.net.URL;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wdssii.core.ConfigurationException;
import org.wdssii.core.LRUCache;
import org.wdssii.core.W2Config;
import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.builders.BuilderFactory;
import org.wdssii.core.LRUCache.LRUTrimComparator;
import org.wdssii.gui.CommandManager.NavigationAction;
import org.wdssii.gui.PreferencesManager.PrefConstants;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductHandler;
import org.wdssii.gui.products.ProductHandlerList;
import org.wdssii.gui.products.ProductTextFormatter;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.index.IndexRecord;
import org.wdssii.xml.Tag_color;
import org.wdssii.xml.Tag_colorBin;
import org.wdssii.xml.Tag_colorDatabase;
import org.wdssii.xml.Tag_colorDef;
import org.wdssii.xml.Tag_colorMap;
import org.wdssii.xml.iconSetConfig.Tag_iconSetConfig;

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
    private Tag_colorDatabase myColorDefs = new Tag_colorDatabase();
    /** A static database of information about products */
    TreeMap<String, ProductDataInfo> myProductInfo = new TreeMap<String, ProductDataInfo>();
    ProductDataInfo myDefaults = new ProductDataInfo();
    /** The cache for Product objects */
    LRUCache<Product> myProductCache = new LRUCache<Product>();
    /** A map of names to ProductHandlerLists */
    private ArrayList<ProductHandlerList> myProductGroups = new ArrayList<ProductHandlerList>();
    /** Current selected group */
    ProductHandlerList myProductOrderedSet;

    {
        myProductGroups.add(new ProductHandlerList("data1", "Data1"));
        myProductGroups.add(new ProductHandlerList("data2", "Data2"));
        selectKey("data1");
        myProductCache.setMinCacheSize(MIN_CACHE_SIZE);
        myProductCache.setMaxCacheSize(MAX_CACHE_SIZE);
    }

    public void selectKey(String key) {
        ProductHandlerList found = getGroupForKey(key);
        myProductOrderedSet = found;
    }

    private ProductHandlerList getGroupForKey(String key) {
        ProductHandlerList found = null;
        for (ProductHandlerList l : myProductGroups) {
            if (l.getKey().equals(key)) {
                found = l;
                break;
            }
        }
        return found;
    }

    // called by ColorKeyLayer to get the current color map...
    public ColorMap getCurrentColorMap() {
        return (myProductOrderedSet.getCurrentColorMap());
    }

    /** Get the current selected product handler list */
    public ProductHandlerList getProductOrderedSet() {
        return myProductOrderedSet;
    }

    /** Used by group view to show the stuff */
    public ArrayList<ProductHandlerList> getGroupList() {
        return myProductGroups;
    }

    public FilterList getFilterList(String product) {
        FilterList aList = null;
        if (myProductOrderedSet != null) {
            ProductHandler tph = myProductOrderedSet.getProductHandler(product);
            if (tph != null) {
                aList = tph.getFList();
            }
        }
        return aList;
    }

    // Called to get the top product in the display
    public Product getTopProduct() {
        Product aProduct = null;
        ProductHandlerList list = getProductOrderedSet();
        if (list != null) {
            ProductHandler h = list.getTopProductHandler();
            if (h != null) {
                aProduct = h.getProduct();
            }
        }
        return aProduct;
    }

    /** Currently called by ReadoutStatusBar to get the text for readout */
    public String getReadout(PositionEvent event) {
        return (myProductOrderedSet.getReadout(event));
    }

    public void navigationAction(NavigationAction nav) {
        myProductOrderedSet.navigationAction(nav);
    }

    /** Compare a product to a given indexKey, if it matchs, remove from cache */
    private static class IndexKeyComparator<T> implements LRUTrimComparator<T> {

        String indexKey;

        public IndexKeyComparator(String key) {
            indexKey = key;
        }

        @Override
        public boolean shouldDelete(T test) {
            boolean delete = false;
            if (test instanceof Product) {
                Product p = (Product) (test);
                if (p.getIndexKey().compareTo(indexKey) == 0) {
                    delete = true;
                }
            }
            return true;
        }
    }
    // Product charts	

    /** Set the size of the product cache */
    public void setCacheSize(int size) {
        myProductCache.setCacheSize(size);
        int aSize = myProductCache.getCacheSize();
        PreferencesManager p = PreferencesManager.getInstance();
        p.setValue(PrefConstants.PREF_cacheSize, aSize);
        CommandManager.getInstance().cacheManagerNotify(); // added product it changed
    }

    /** Get the size of the product cache */
    public int getCacheSize() {
        return myProductCache.getCacheSize();
    }
    
    /** Get the number of items in the cache */
    public int getCacheFilledSize() {
        return myProductCache.getCacheFilledSize();
    }
    
    public ArrayList<Product> getCurrentCacheList(){
         return myProductCache.getStackCopy();
    }

    /** Trim all products from cache matching a given index key */
    public int trimCacheMatchingIndexKey(String indexKey) {

        IndexKeyComparator<Product> c = new IndexKeyComparator<Product>(indexKey);
        return (myProductCache.trimCacheMatching(c));
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
        
        /** The current color map for this product */
        private ColorMap myColorMap = null;
        
        /** Set to true if we have tried to load the xml files for this type,
         * such as the colormap.xml or iconconfig.xml
         */
        private boolean myLoadedXML = false;
        
        /** Time window for this product.  If outside time of window minus
         * this date value, product is considered too old to display
         */
        private long myTimeWindowSeconds = 350;  // 5 min default
        
        // We have two main xml formats for Products.  One is the 
        // <colormap> for float based data to color lookup.  The other
        // is the icon configuration file <iconSetConfig>.  For the moment
        // not going to bother with separate classes for this.
        private Tag_colorMap myColorMapTag = null;
        private Tag_iconSetConfig myIconSetConfig = null;

        private URL colorMapURL;
        private URL iconSetConfigURL;
        
        /** Debug flag for forcing generated maps always */
        private static boolean forceGenerated = false;
        
        public URL getCurrentColorMapURL(){
            return colorMapURL;
        }
        
        public URL getCurrentIconSetURL(){
            return iconSetConfigURL;
        }
        
        /** Load any xml files that pertain to this particular product */
        public void loadProductXMLFiles(boolean force) {

            if (force){ myLoadedXML = false; }
            if (myLoadedXML == false){
                loadIconSetConfigFromXML();
                if (!forceGenerated){
                    loadColorMapFromXML();
                }
                myLoadedXML = true;
            }
        }

        /**
         * Force load a color map from xml and make a new color map from it
         */
        private void loadColorMapFromXML() {
            URL u = W2Config.getURL("colormaps/" + myName);
            colorMapURL = u;
            Tag_colorMap tag = new Tag_colorMap();
            if (tag.processAsRoot(u)) {
                myColorMapTag = tag;
                ColorMap aColorMap = new ColorMap();
                aColorMap.initFromTag(tag, ProductTextFormatter.DEFAULT_FORMATTER);
                myColorMap = aColorMap;       
            }
        }
        
       /**
         * Force load an icon configuration file
         */
        private void loadIconSetConfigFromXML() {
            URL u = W2Config.getURL("icons/" + myName);
            iconSetConfigURL = u;
            Tag_iconSetConfig tag = new Tag_iconSetConfig();
            if (tag.processAsRoot(u)) {
                myIconSetConfig = tag;
                
                // We are going to use the color map of the polygon for 
                // the moment.  The color of the polygon fill is the key.
                try{  // since any subtag might be null.  We have no map then
                    Tag_colorMap c = tag.polygonTextConfig.polygonConfig.colorMap;
                    ColorMap aColorMap = new ColorMap();
                    aColorMap.initFromTag(c, ProductTextFormatter.DEFAULT_FORMATTER);
                    myColorMap = aColorMap;      
                    myColorMapTag = c;
                }catch(Exception e){
                    // Any of it null, etc..ignore it...
                }finally{
                   // null/missing tag
                }
            }
        }

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

        /** Return if loaded.  Some stuff like the color key layer will
         * check this so that it doesn't cause a thrash load of EVERY colormap
         * by calling getColorMap
         * @return 
         */
        public boolean isLoaded(){
            return myLoadedXML;
        }
        
        /** Get color map.  Will attempt to load the color map if not
         * already loaded
         * @return the ColorMap, or null
         */
        public ColorMap getColorMap() {
            loadProductXMLFiles(false);
            return myColorMap;
        }

        public void setColorMap(ColorMap theColorMap) {
            myColorMap = theColorMap;
        }
        
        public Tag_iconSetConfig getIconSetConfig(){
            loadProductXMLFiles(false);
            return myIconSetConfig;
        }
        
        public long getTimeWindowSeconds(){
            return myTimeWindowSeconds;
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
     * FIXME: redo this for stax, clean up xml format as well...
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
        loadColorDatabase();

        // Load cache size from preferences
        PreferencesManager p = PreferencesManager.getInstance();
        int aSize = p.getInteger(PrefConstants.PREF_cacheSize);
        myProductCache.setCacheSize(aSize);
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
        // ColorMap aColorMap = myColorMaps.get(name);
        //return (aColorMap != null);
        ProductDataInfo d = getProductDataInfo(name);
        return (d.getColorMap() != null);
    }

    /**
     * @param name
     *            the name of the color map to return such as 'Reflectivity'
     * @return the color map
     */
    public ColorMap getColorMap(String name) {
        ProductDataInfo d = getProductDataInfo(name);
        return d.getColorMap();
    }

    /** Store a new colormap, with option to force store it over an old one.
     * @param colorMapName
     * @param map
     * @param force
     * @return true on success
     */
    public boolean storeNewColorMap(String colorMapName, ColorMap map, boolean force) {
        boolean success = false;
        //  if (force || !myColorMaps.containsKey(colorMapName)) {
        //      myColorMaps.put(colorMapName, map);
        //      success = true;
        //   }
        ProductDataInfo d = getProductDataInfo(colorMapName);
        if (force || (d.getColorMap() == null)) {
            d.setColorMap(map);
            success = true;
        }
        return success;
    }

    public boolean storeNewColorMap(String colorMapName, ColorMap map) {
        return (storeNewColorMap(colorMapName, map, false));
    }

    // FIXME: what about any sync issues with access?
    public TreeMap<String, ProductDataInfo> getProductDataInfoSet() {
        return myProductInfo;
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
            myProductInfo.put(name, d);
        }
        // Can't do this here or it will load every file
        // (the product browser uses info as well )
        // d.loadProductXMLFiles(false);
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
            productInfoXML = W2Config.getElement("misc/productinfo");
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

    public void loadColorDatabase() {
        URL aURL = null;
        try {
            aURL = W2Config.getURL("colorDatabase.xml");
            myColorDefs.processAsRoot(aURL);
        } catch (Exception c) {
        }
    }

    /** Modify a Tag_colorMap, enhancing color names with actually color
     * information from the color database.
     */
    public void updateNamesToColors(Tag_colorMap map) {
        if (myColorDefs.colorDefs != null) {
            ArrayList<Tag_colorBin> bins = map.colorBins;
            if (bins != null) {
                for (Tag_colorBin b : bins) {
                    ArrayList<Tag_color> colors = b.colors;
                    if (colors != null) {
                        for (Tag_color c : colors) {
                            if (c.name != null) { // If missing, leave color alone
                                Tag_colorDef t = myColorDefs.colorDefs.get(c.name);
                                if (t != null) {
                                    c.r = t.r;
                                    c.g = t.g;
                                    c.b = t.b;
                                    c.a = t.a;
                                }
                            }
                        }

                    }

                }
            }
        }
    }

    /** Return named color from database, fallback to pure white */
    public Color getNamedColor(String name) {
        Color c = null;
        boolean success = false;
        if (name != null) {
            Tag_colorDef t = myColorDefs.colorDefs.get(name);
            if (t != null) {
                c = new Color(t.r, t.g, t.b, t.a);
                success = true;
            }
        }
        if (success == false) {
            c = new Color(255, 0, 0, 255);
        }
        return c;
    }

    public boolean hasNamedColor(String name) {
        return false;
    }

    // External create product method (convenience routine)
    public static Product CreateProduct(
            String productCacheKey,
            String anIndex, IndexRecord init) {
        return (ProductManager.getInstance().getProduct(productCacheKey, anIndex, init));
    }

    //public static ArrayList<Product> getArrayList() {
    //   return ProductManager.getInstance().myLRUStack;
    //}
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
                    myProductCache.put(productCacheKey, theProduct);
                    CommandManager.getInstance().cacheManagerNotify(); // Raised product it changed
                } else {
                    log.error("Wasn't able to create the product for data.  Nothing will show");
                }

                // Product already found in cache.  Raise it in the LRU to top
            } else {
                theProduct.updateDataTypeIfLoaded();
                CommandManager.getInstance().cacheManagerNotify(); // Raised product it changed
            }
        }
        return (theProduct);
    }
    
    /** GUI uses to pull current stack item.  We'll probably need to synchronize later. */
    public Product getProductAt(int i){
        return myProductCache.get(i);
    }

    // Internal create product method
    private Product makeProduct(String anIndex, IndexRecord init) {
        Product p = null;
        DataRequest dr = null;
        if (init != null) {
            try {
                dr = BuilderFactory.createDataRequest(init);
                p = new Product(dr, anIndex, init);
            } catch (Exception e) {
                log.error("Exception loading data..." + e.toString());
            }
        }
        return p;
    }

    public void clearProductCache() {
        myProductCache.clear();
        //  myLRUStack.clear();
        CommandManager.getInstance().cacheManagerNotify(); // added product it changed
    }

    /** Get the current volume of the top selected product in the product selector
     */
    public static ProductVolume getCurrentVolumeProduct(boolean virtual) {
        ProductHandlerList phl = ProductManager.getInstance().getProductOrderedSet();
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
        ProductHandlerList phl = ProductManager.getInstance().getProductOrderedSet();
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
        ProductHandlerList phl = ProductManager.getInstance().getProductOrderedSet();
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

        ProductHandlerList p = getProductOrderedSet();
        ProductHandler ph = p.getTopProductHandler();
        Product prod = null;
        if (ph != null) {
            prod = ph.getProduct();
        }
        return prod;
    }
}
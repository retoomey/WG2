package org.wdssii.gui;

import org.wdssii.core.SingletonManager;
import org.wdssii.core.Singleton;
import gov.nasa.worldwind.event.PositionEvent;
import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.net.URL;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wdssii.core.ConfigurationException;
import org.wdssii.core.LRUCache;
import org.wdssii.core.LRUCache.LRUTrimComparator;
import org.wdssii.core.W2Config;
import org.wdssii.gui.PreferencesManager.PrefConstants;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.FeatureList.FeatureFilter;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.Product.Navigation;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.products.ProductTextFormatter;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.sources.IndexSource;
import org.wdssii.gui.sources.Source;
import org.wdssii.gui.sources.SourceList;
import org.wdssii.gui.views.WorldWindView;
import org.wdssii.index.HistoricalIndex;
import org.wdssii.index.IndexRecord;
import org.wdssii.xml.*;
import org.wdssii.xml.ColorDatabase.ColorDef;
import org.wdssii.xml.W2ColorMap.W2ColorBin;
import org.wdssii.xml.iconSetConfig.Categories;
import org.wdssii.xml.iconSetConfig.Category;
import org.wdssii.xml.iconSetConfig.IconSetConfig;
import org.wdssii.xml.iconSetConfig.ImageSymbol;
import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.StarSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * --Maintains a set of color maps by product name (color map cache FIXME: Move
 * to generic cache) --Maintains the product 'info', colors, etc. Singleton
 * --Maintains the product factory creating the default product for an
 * IndexRecord --Maintains the product LRU cache (FIXME: Move to generic cache)
 * --Maintains the product chart factory list
 *
 * @author Robert Toomey
 *
 */
public class ProductManager implements Singleton {

    private static Logger log = LoggerFactory.getLogger(ProductManager.class);
    public static final String DEFAULTS = "defaults";
    private static ProductManager instance = null;
    final public static int MIN_CACHE_SIZE = 50;
    final public static int MAX_CACHE_SIZE = 500;
    private ColorDatabase myColorDefs = new ColorDatabase();
    public static final String TOP_PRODUCT = "TOP_PRODUCT_____";
    /**
     * A static database of information about products
     */
    TreeMap<String, ProductDataInfo> myProductInfo = new TreeMap<String, ProductDataInfo>();
    ProductDataInfo myDefaults = new ProductDataInfo();
    /**
     * The cache for Product objects
     */
    LRUCache<String, Product> myProductCache = new LRUCache<String, Product>(MIN_CACHE_SIZE, 200, MAX_CACHE_SIZE);

    // Feature convenience?
    /**
     * Wrapper to get list of ProductFeatures. Currently only one . This should
     * take a key eventually
     */
    public List<ProductFeature> getProductFeatures() {
        FeatureList flist = FeatureList.theFeatures;
        List<ProductFeature> forg = flist.getFeatureGroup(ProductFeature.class);
        return forg;
    }

    public FeatureList getFeatureList() {
        return FeatureList.theFeatures;
    }

    public Feature getNamedFeature(String key) {
        Feature f = FeatureList.theFeatures.getFeature(key);
        return f;
    }

    public ProductFeature getProductFeature(String key) {

        ProductFeature pf = null;
        Feature f = FeatureList.theFeatures.getFeature(key);
        if (f instanceof ProductFeature) {
            pf = (ProductFeature) (f);
        }

        // We can use the key 'TOP_PRODUCT' to always get the top product
        if (pf == null) {
            if (TOP_PRODUCT.compareTo(key) == 0) {
                pf = getTopProductFeature();
            }
        }

        return pf;
    }

    public ProductFeature getTopProductFeature() {

        ProductFeature pf = null;
        Feature f = FeatureList.theFeatures.getSelected(ProductFeature.ProductGroup);
        if (f instanceof ProductFeature) {
            pf = (ProductFeature) (f);
        }
        return pf;
    }

    public Date getSimulationTime() {
        return FeatureList.theFeatures.getSimulationTime();
    }

    public String getSimulationTimeStamp() {
        return FeatureList.theFeatures.getSimulationTimeStamp();
    }

    public void deleteSelectedProduct() {
        log.error("Need to implement deleteSelectedProduct");
    }

    /**
     * Simple Product delete filter based on index key
     */
    private static class ProductDeleteFilter implements FeatureFilter {

        private String theKey;

        public ProductDeleteFilter(String k) {
            theKey = k;
        }

        @Override
        public boolean matches(Feature f) {
            boolean remove = false;
            if (f instanceof ProductFeature) {
                ProductFeature pf = (ProductFeature) (f);
                Product p = pf.getProduct();
                if (p != null) {
                    String key = p.getIndexKey();
                    remove = (theKey.compareTo(key) == 0);
                }
            }
            return remove;
        }
    }

    // Called from ProductDeleteCommand (with a source filter parameter)
    public void deleteProductsMatchingSource(String toDelete) {
        log.info("Delete product matching source " + toDelete);
        log.error("Need to implement delete products matching source");
        ProductDeleteFilter filter = new ProductDeleteFilter(toDelete);
        FeatureList.theFeatures.removeFeatures(filter);
    }

    public void deleteProduct(String key) {
        FeatureList.theFeatures.removeFeature(key);
    }

    /**
     * Navigate usually called by the navigation buttons. A button has been
     * clicked, load next product, sync, etc..
     */
    public void navigate(Navigation message) {

        Product p = getTopProduct();
        if (p == null) {
            log.warn("Can't move from a null record, no reference");
        } else {
            Product aProduct = p.getProduct(message);
            ProductFeature pf = loadProduct(aProduct);

            if (pf != null) {
                selectProductFeature(pf);
                pf.timeNavigateTo();
            }
        }
    }

    /**
     * Called by the ProductLoadCommand from the index record picker
     *
     * @param aProduct
     */
    public void recordPickerSelectedProduct(String indexName, String datatype, String subtype, Date time) {

        FeatureList toAdd = FeatureList.theFeatures;
        // ----------------------------------------------------------------
        // Try to create default product from selections.
        //SourceManager manager = SourceManager.getInstance();
        Source s = SourceList.theSources.getSource(indexName);
        IndexSource is = null;
        if (s instanceof IndexSource) {  // Hack for moment, this code should move
            is = (IndexSource) (s);
        } else {
            return;
        }
        HistoricalIndex anIndex = is.getIndex();
        //  HistoricalIndex anIndex = SourceManager.getIndexByName(indexName);

        if (anIndex == null) {
            log.error("Index null, cannot create new product");
            return;
        }

        IndexRecord aRecord = anIndex.getRecord(datatype, subtype, time);
        if (aRecord == null) {
            log.error("Record is null, cannot create new product");
            return;
        }
        String indexKey = is.getKey();

        aRecord.setSourceName(indexKey);
        String productCacheKey = Product.createCacheKey(indexName, aRecord);
        Product aProduct = ProductManager.CreateProduct(productCacheKey, indexName, aRecord);

        // ----------------------------------------------------------------
        // Load product into our list, creating a handler for it
        ProductFeature pf = loadProduct(aProduct);
        if (pf != null) {
            selectProductFeature(pf);
            pf.timeNavigateTo();
        }
    }

    // called by ColorKeyLayer to get the current color map...
    public ColorMap getCurrentColorMap() {
        List<ProductFeature> l = getProductFeatures();
        for (ProductFeature current : l) {
            if (current.wouldRender()) {
                // Just the first color map for now at least
                return (current.getProduct().getColorMap());
            }
        }
        return null;
    }

    /**
     * Get the current selected product handler list
     */
    ////public ProductHandlerList getProductOrderedSet() {
    //    return myProductOrderedSet;
    //}
    public FilterList getFilterList(String product) {
        FilterList aList = null;
        Product aProduct = null;
        ProductFeature pf = this.getProductFeature(product);
        if (pf != null) {
            aList = pf.getFList();
        }
        return aList;
    }

    // Called to get the top product in the display
    public Product getTopProduct() {


        Product aProduct = null;
        ProductFeature pf = getTopProductFeature();
        if (pf != null) {
            aProduct = pf.getProduct();
        }
        return aProduct;
    }

    /**
     * Currently called by ReadoutStatusBar to get the text for readout
     */
    public String getReadout(PositionEvent event) {

        String readout = "None";
        Product current = getTopProduct();
        if (current != null) {
            //Position p = event.getPosition();
            Point point = event.getScreenPoint();
            readout = String.format("(%d, %d)", point.x, point.y);
            WorldWindView earth = FeatureList.theFeatures.getWWView();
            if (earth != null) {
                earth.getColor(point.x, point.y);
            }
            //readout = current.getReadout(p.getLatitude().getDegrees(),
            //		p.getLongitude().getDegrees(), 
            //		p.getHeight().get);
        }

        return readout;
    }

    /**
     * Compare a product to a given indexKey, if it matches, remove from cache
     */
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

    /**
     * Set the size of the product cache
     */
    public void setCacheSize(int size) {
        myProductCache.setCacheSize(size);
        int aSize = myProductCache.getCacheSize();
        PreferencesManager p = PreferencesManager.getInstance();
        p.setValue(PrefConstants.PREF_cacheSize, aSize);
    }

    /**
     * Get the size of the product cache
     */
    public int getCacheSize() {
        return myProductCache.getCacheSize();
    }

    /**
     * Get the number of items in the cache
     */
    public int getCacheFilledSize() {
        return myProductCache.getCacheFilledSize();
    }

    public ArrayList<Product> getCurrentCacheList() {
        return myProductCache.getStackCopy();
    }

    /**
     * Trim all products from cache matching a given index key
     */
    public int trimCacheMatchingIndexKey(String indexKey) {

        IndexKeyComparator<Product> c = new IndexKeyComparator<Product>(indexKey);
        return (myProductCache.trimCacheMatching(c));
    }

    /**
     * Database node which holds information for a data type by name
     *
     * @author Robert Toomey FIXME: separate class file? It's static-inner so
     * this only affects namespace, however this class is meaningless without
     * the manager.
     */
    public static class ProductDataInfo {

        private String myName = DEFAULTS;
        private Color myListColor = null; // Null means system default theme
        // color (not set)
        private boolean myVisibleInList = true;
        private String myListName = DEFAULTS;
        // When null, color map key is name of product, otherwise overridden
        private String myColorMapKey = null;
        // Min value for our algoritm maps
        private float myMin = -100;
        // Max value for out algoritm maps
        private float myMax = 100;
        /**
         * The current color map for this product
         */
        private ColorMap myColorMap = null;
        /**
         * The symbology for this product
         */
        private Symbology mySymbology = null;
        /**
         * Set to true if we have tried to load the xml files for this type,
         * such as the colormap.xml or iconconfig.xml
         */
        private boolean myLoadedXML = false;
        /**
         * Time window for this product. If outside time of window minus this
         * date value, product is considered too old to display
         */
        private long myTimeWindowSeconds = 350;  // 5 min default
        // We have two main xml formats for Products.  One is the 
        // <colormap> for float based data to color lookup.  The other
        // is the icon configuration file <iconSetConfig>.  For the moment
        // not going to bother with separate classes for this.
        private W2ColorMap myColorMapTag = null;
        private IconSetConfig myIconSetConfig = null;
        private URL colorMapURL;
        private URL iconSetConfigURL;
        /**
         * Debug flag for forcing generated maps always
         */
        private static boolean forceGenerated = false;

        public URL getCurrentColorMapURL() {
            return colorMapURL;
        }

        public URL getCurrentIconSetURL() {
            return iconSetConfigURL;
        }

        public void setColorKey(String key) {

            if (myName.equals(key)) {  // If key matches product, no need to store it
                myColorMapKey = null;
            } else {            // Reflectivity -> Blue_Wave (override)
                myColorMapKey = key;
            }
            setColorMap(null); // needs to reload  FIXME: check it changed?
            myLoadedXML = false;
        }

        public String getColorKey() {
            if (myColorMapKey == null) {
                return myName;
            } else {
                return myColorMapKey;
            }
        }

        public float getMinColorKeyValue() {
            return myMin;
        }

        public void setMinColorKeyValue(float v) {
            myMin = v;
        }

        public float getMaxColorKeyValue() {
            return myMax;
        }

        public void setMaxColorKeyValue(float v) {
            myMax = v;
        }

        /**
         * Load any xml files that pertain to this particular product
         */
        public void loadProductXMLFiles(boolean force) {

            if (force) {
                myLoadedXML = false;
            }
            if (myLoadedXML == false) {
                loadSymbologyFromXML();
                loadIconSetConfigFromXML();
                if (!forceGenerated) {
                    loadColorMapFromXML();
                }
                myLoadedXML = true;
            }
        }

        /**
         * Force load a color map from xml and make a new color map from it
         */
        private void loadColorMapFromXML() {

            // Look for a file matching product name....
            W2ColorMap map = Util.load("colormaps/" + getColorKey(), W2ColorMap.class);
            if (map != null) {
                ColorMap aColorMap = new ColorMap();
                aColorMap.initToW2ColorMap(map, ProductTextFormatter.DEFAULT_FORMATTER);
                myColorMap = aColorMap;
            } else {
                // Try to look up in point database....
                // Not found...so try to generate...
                float minValue = -100;
                float maxValue = 100;
                // Gotta be SET somewhere...
                // DataTypeMetric m = p.getDataTypeMetric();
                // if (m != null) {
                //     minValue = m.getMinValue();
                //     maxValue = m.getMaxValue();
                //}
                ColorMap aColorMap = new ColorMap();
                //PointColorMap pMap = PointColorMap.theIDLList.getByName("Blue_Waves");
                PointColorMap pMap = PointColorMap.theIDLList.getByName("Blue_Red");

                aColorMap.initToTag(pMap, minValue, maxValue, "Dimensionless", ProductTextFormatter.DEFAULT_FORMATTER);
                myColorMap = aColorMap;
            }

            /*
             URL u = W2Config.getURL("colormaps/" + myName);
             colorMapURL = u;
             Tag_colorMap tag = new Tag_colorMap();
             if (tag.processAsRoot(u)) {
             myColorMapTag = tag;
             ColorMap aColorMap = new ColorMap();
             aColorMap.initFromTag(tag, ProductTextFormatter.DEFAULT_FORMATTER);
             myColorMap = aColorMap;
             }*/
        }

        /**
         * The new symbology file. This will include all the old stuff from
         * color maps, etc. ColorMaps are really a value --> color lookup which
         * could occur differently for datatable, etc. We need to refactor this
         * get away from the old display way.
         */
        private void loadSymbologyFromXML() {
            // If we don't have a symbology...
            if (mySymbology == null) {
                Symbology s = Util.load("symbology/" + getName() + ".xml", Symbology.class);
                if (s == null) {
                    s = new Symbology();
                } else {
                    log.debug("Loaded Symbology for " + getName());
                }

                // Put some test stuff in there....
                s.use = Symbology.SINGLE;

                StarSymbol test = new StarSymbol();
                Categories cs = s.getCategories();
                Category c = new Category();
                c.addSymbol(test); // Add a Star
                c.addSymbol(new ImageSymbol());
                c.addSymbol(new PolygonSymbol());
                cs.addCategory(c);

                //URL u2 = W2Config.getURL("symbology/" + getName() + "2.xml");
                //Util.save(s, u2.getFile(), s.getClass());
                // ...then make one
                mySymbology = s;
            }
        }

        /**
         * Force load an icon configuration file
         */
        private void loadIconSetConfigFromXML() {
            URL u = W2Config.getURL("icons/" + getColorKey());
            iconSetConfigURL = u;

            IconSetConfig tag = Util.load("icons/" + getColorKey(), IconSetConfig.class);
            if (tag != null) {
                myIconSetConfig = tag;

                // We are going to use the color map of the polygon for 
                // the moment.  The color of the polygon fill is the key.
                try {  // since any subtag might be null.  We have no map then
                    // W2ColorMap c = tag.polygonTextConfig.polygonConfig.colorMap;
                    //// ColorMap aColorMap = new ColorMap();
                    //  aColorMap.initToW2ColorMap(c, ProductTextFormatter.DEFAULT_FORMATTER);
                    // myColorMap = aColorMap;
                    // myColorMapTag = c;
                } catch (Exception e) {
                    // Any of it null, etc..ignore it...
                } finally {
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

        /**
         * Return if loaded. Some stuff like the color key layer will check this
         * so that it doesn't cause a thrash load of EVERY colormap by calling
         * getColorMap
         *
         * @return
         */
        public boolean isLoaded() {
            return myLoadedXML;
        }

        /**
         * Get color map. Will attempt to load the color map if not already
         * loaded
         *
         * @return the ColorMap, or null
         */
        public ColorMap getColorMap() {
            loadProductXMLFiles(false);
            return myColorMap;
        }

        public void setColorMap(ColorMap theColorMap) {
            myColorMap = theColorMap;
        }

        public Symbology getSymbology() {
            loadSymbologyFromXML();
            return mySymbology;
        }

        public void setSymbology(Symbology s) {
            mySymbology = s;
        }

        public IconSetConfig getIconSetConfig() {
            loadProductXMLFiles(false);
            return myIconSetConfig;
        }

        public long getTimeWindowSeconds() {
            return myTimeWindowSeconds;
        }
    }

    /**
     * SAME FUNCTION AS ColorMap (xml util class) FIXME parse the text of a
     * color number field into an integer value
     *
     * @param textOfNumber the raw text of number, such as 0xFF or 45
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

    public static Singleton create() {
        instance = new ProductManager();
        return instance;
    }

    /**
     * @return the singleton for the manager
     */
    public static ProductManager getInstance() {
        if (instance == null) {
            log.debug("Product Manager must be created by SingletonManager");
        }
        return instance;
    }

    public boolean hasColorMap(String productDatatypeName) {
        // ColorMap aColorMap = myColorMaps.get(name);
        //return (aColorMap != null);
        ProductDataInfo d = getProductDataInfo(productDatatypeName);
        return (d.getColorMap() != null);
    }

    public ColorMap getColorMap(Product p) {

        ProductDataInfo d = getProductDataInfo(p.getDataType());
        ColorMap c = d.getColorMap();

        return c;
    }

    public Symbology getSymbology(Product p) {
        ProductDataInfo d = getProductDataInfo(p.getDataType());
        Symbology s = d.getSymbology();

        return s;
    }

    public void setColorKey(Product p, String key) {
        ProductDataInfo d = getProductDataInfo(p.getDataType());
        d.setColorKey(key);
    }

    public void setSymbology(Product p, Symbology s) {
        ProductDataInfo d = getProductDataInfo(p.getDataType());
        d.setSymbology(s);
    }

    public String getColorKey(Product p) {
        ProductDataInfo d = getProductDataInfo(p.getDataType());
        return d.getColorKey();
    }

    public float getMinColorKeyValue(Product p) {
        ProductDataInfo d = getProductDataInfo(p.getDataType());
        return d.getMinColorKeyValue();
    }

    public void setMinColorKeyValue(Product p, float v) {
        ProductDataInfo d = getProductDataInfo(p.getDataType());
        d.setMinColorKeyValue(v);
    }

    public float getMaxColorKeyValue(Product p) {
        ProductDataInfo d = getProductDataInfo(p.getDataType());
        return d.getMaxColorKeyValue();
    }

    public void setMaxColorKeyValue(Product p, float v) {
        ProductDataInfo d = getProductDataInfo(p.getDataType());
        d.setMaxColorKeyValue(v);
    }

    /**
     * Store a new colormap, with option to force store it over an old one.
     *
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
            //aURL = W2Config.getURL("colorDatabase.xml");
            ColorDatabase map = Util.load("colorDatabase.xml", ColorDatabase.class);
            myColorDefs = map;
        } catch (Exception c) {
            log.error("Error loading name to color database...ignoring");
        }
    }

    /**
     * Modify a Tag_colorMap, enhancing color names with actually color
     * information from the color database.
     */
    public void updateNamesToColors(W2ColorMap map) {
        if (myColorDefs != null) {
            List<W2ColorBin> bins = map.colorBins;
            if (bins != null) {
                for (W2ColorBin b : bins) {
                    List<W2Color> colors = b.colors;
                    if (colors != null) {
                        for (W2Color c : colors) {
                            if (c.name != null) { // If missing, leave color alone
                                ColorDef t = myColorDefs.get(c.name);
                                if (t != null) {
                                    c.r = t.red();
                                    c.g = t.green();
                                    c.b = t.blue();
                                    c.a = t.alpha();
                                }
                            }
                        }

                    }

                }
            }
        }
    }

    /**
     * Return named color from database, fallback to pure white
     */
    public Color getNamedColor(String name) {
        Color c = null;
        boolean success = false;
        if (name != null) {
            ColorDef t = myColorDefs.get(name);
            if (t != null) {
                c = new Color(t.red(), t.green(), t.blue(), t.alpha());
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

                theProduct = makeProduct(anIndex, init);
                if (theProduct != null) {
                    theProduct.setCacheKey(productCacheKey);

                    // First, check cache size and trim to maxsize -1 before adding new product
                    // Problem with this is if cache size can change on the fly we need to trim
                    // to the new lower size actually.  This only works with new cache == old
                    myProductCache.put(productCacheKey, theProduct);
                } else {
                    log.error("Wasn't able to create the product for data.  Nothing will show");
                }

                // Product already found in cache.  Raise it in the LRU to top
            } else {
                theProduct.updateDataTypeIfLoaded();
            }
        }
        return (theProduct);
    }

    /**
     * GUI uses to pull current stack item. We'll probably need to synchronize
     * later.
     */
    // public Product getProductAt(int i){
    //     return myProductCache.get(i);
    // }
    // Internal create product method
    private Product makeProduct(String anIndex, IndexRecord init) {
        Product p = null;
        /* Don't load just from making a Product object 
         DataRequest dr = null;
         if (init != null) {
         try {
         dr = BuilderFactory.createDataRequest(init);
         p = new Product(dr, anIndex, init);
         } catch (Exception e) {
         log.error("Exception loading data..." + e.toString());
         }
         }
         */
        p = new Product(anIndex, init);
        //p.startLoading();
        return p;
    }

    public void clearProductCache() {
        myProductCache.clear();
        //  myLRUStack.clear();
    }

    /**
     * Get the current volume of the top selected product in the product
     * selector
     */
    public static ProductVolume getCurrentVolumeProduct(boolean virtual) {
        ProductVolume volume = null;
        ProductFeature pf = ProductManager.getInstance().getTopProductFeature();
        if (pf != null) {
            Product p = pf.getProduct();
            if (p != null) {
                volume = p.getProductVolume(virtual);
            }
        }
        // Use a 'fake' volume is null...
        if (volume == null) {
            volume = new ProductVolume(); // steal the root class for now.
        }
        return volume;
    }

    /**
     * Get the current volume of the top selected product in the product
     * selector
     */
    public static ProductVolume getCurrentVolumeProduct(String key, boolean virtual) {
        /*
         * ProductHandlerList phl =
         * ProductManager.getInstance().getProductOrderedSet(); ProductVolume
         * volume = null; if (phl != null) { ProductHandler tph =
         * phl.getProductHandler(key); if (tph != null) { Product product =
         * tph.getProduct(); if (product != null) { volume =
         * product.getProductVolume(virtual); } } } // Use a 'fake' volume is
         * null... if (volume == null) { volume = new ProductVolume(); // steal
         * the root class for now. } return volume;
         *
         */
        ProductVolume volume = null;
        ProductFeature pf = ProductManager.getInstance().getProductFeature(key);
        if (pf != null) {
            Product p = pf.getProduct();
            if (p != null) {
                volume = p.getProductVolume(virtual);
            }
        }
        // Use a 'fake' volume is null...
        if (volume == null) {
            volume = new ProductVolume(); // steal the root class for now.
        }
        return volume;

    }

    /**
     * We check each product handler we currently have to see if any of them can
     * handle this product. Otherwise, we ask the product to create a new
     * instance of the handler for it. This gets added to our set and popped to
     * top This is a generic load without gui updates.
     *
     * @param aProduct
     */
    public ProductFeature loadProduct(Product aProduct) {
        if (aProduct == null) {
            return null;
        }

        FeatureList toAdd = FeatureList.theFeatures;

        boolean found = false;
        ArrayList<ProductFeature> list = toAdd.getFeatureGroup(ProductFeature.class);

        Iterator<ProductFeature> iter = list.iterator();
        ProductFeature theHandler = null;

        while (iter.hasNext()) {
            ProductFeature current = iter.next();
            if (current.canHandleThisProduct(aProduct)) {
                theHandler = current;
                found = true;
                break;
            }
        }

        if (!found) {
            ProductFeature newOne = new ProductFeature(toAdd, aProduct);
            toAdd.addFeature(newOne);
            theHandler = newOne;
            log.debug("Created NEW ProductFeature for product");
        }

        // Move handler to this product
        theHandler.setProduct(aProduct);
        aProduct.startLoading();  //Initialize loading if not already...
        return theHandler;
    }

    /**
     * Called from a ProductSelectCommand
     */
    public void selectProductFeature(String key) {
        selectProductFeature(getProductFeature(key));
    }

    public void selectProductFeature(ProductFeature theSelection) {

        // Move to 'top' visually
        FeatureList.theFeatures.setDrawLast(theSelection);

        // Select the item in lists
        FeatureList.theFeatures.setSelected(theSelection);
    }
}
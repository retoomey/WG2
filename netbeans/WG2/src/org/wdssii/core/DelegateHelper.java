package org.wdssii.core;

import java.util.TreeMap;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * This is my design pattern (Robert Toomey)
 *
 * It creates delegate objects by reflection in a lazy and fallback fashion.
 *
 * Example: We have a class such as "Product" that lazy loads an object such as
 * "RadialSet" Once loaded, delegate objects can be created such as
 * "RadialSetRenderer", "RadialSetNavigator" Also, until loaded, either the
 * delegate will be null, or it can fall back to a 'base' such as
 * "ProductNavigator"
 *
 * @author Robert Toomey
 */
public abstract class DelegateHelper {

    private final static Logger LOG = LoggerFactory.getLogger(DelegateHelper.class);
    /**
     * The cached map of named delegateHelpers to objects
     */
    protected TreeMap<String, Object> myHelperObjects = new TreeMap<String, Object>();

    /**
     * Get helper class from cache for reuse
     */
    private Object getHelperClass(String name) {
        return myHelperObjects.get(name);
    }

    /**
     * Store helper class in cache for reuse
     */
    private void setHelperClass(String name, Object helper) {
        myHelperObjects.put(name, helper);
    }

    /**
     * Return the prefix base class for finding helpers by reflection For
     * example, in Product we return null then "RadialSet" when lazy loaded
     */
    public abstract String getPrefixName();

    /**
     * Return base class for finding helpers by reflection when useBaseClass is
     * true. The default is the simple name of the class.
     */
    public String getBaseName() {
        return getClass().getSimpleName();
    }

     protected Object getHelperObject(String classSuffix, boolean useBaseClass,
            boolean cache, String root, String extrainfo) 
     {
         return getHelperObject("", classSuffix, useBaseClass, cache, root, extrainfo);
     }
    /**
     * Get a helper object from cache
     *
     * @param classSuffix
     * @param useBaseClass Fall back to 'base' class if special class missing.
     * For example, no "RadialSetNavigator" will use "ProductNavigator"
     * @param cache Store an instance per object, for example RadialSetRenderer
     * is cached per product
     * @param root
     * @param extrainfo
     * @return
     */
    protected Object getHelperObject(String classPrefix, String classSuffix, boolean useBaseClass,
            boolean cache, String root, String extrainfo) {
        Object helper;

        // Is it cached?
        helper = getHelperClass(classSuffix + ":" + extrainfo);

        // Fixme: Should check that fallback class doesn't need to be
        // replaced with specialized class...
        if (helper == null) {

            String baseClass = classPrefix+getPrefixName();

            /**
             * You can return null until 'ready', this will continue to recheck
             */
            if (baseClass != null) {
                helper = createClassFromName(baseClass, root, classSuffix);

                // Fall back class.
                if ((helper == null) && (useBaseClass == true)) {
                    helper = createClassFromName(getBaseName(), root, classSuffix);
                }
            }

            // Store object in cache
            if ((helper != null) && cache) {
                setHelperClass(classSuffix + ":" + extrainfo, helper);
            }
        }
        return helper;
    }

    /**
     * Create a helper object class from a valid DataType
     */
    protected Object createClassFromName(String dataName, String rootpath, String suffix) {

        Object newClass = null;

        String createIt = rootpath + "." + dataName + suffix;

        Class<?> c;

        try {
            c = Class.forName(createIt);
            newClass = c.newInstance();
            //LOG.info("Generated " + createIt);
        } catch (Exception e) {
            // LOG.warn("Type " + dataName + " doesn't have a " + suffix + " it seems");
            //LOG.warn(e.toString());
        }

        return newClass;
    }
}

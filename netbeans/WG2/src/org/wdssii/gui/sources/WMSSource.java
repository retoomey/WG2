package org.wdssii.gui.sources;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerStyle;
import gov.nasa.worldwind.util.WWUtil;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.GUIPlugInPanel;
import org.wdssii.gui.features.Feature;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.WorldwindStockFeature;
import org.wdssii.gui.views.SourcesURLLoadDialog;
import org.wdssii.index.IndexFactory;

/**
 * Source that handles a Web Map Service
 *
 * @author Robert Toomey
 */
public class WMSSource extends Source {

    private static Logger log = LoggerFactory.getLogger(WMSSource.class);
    protected volatile boolean myConnecting = false;
    protected volatile boolean myConnected = false;

    /**
     * Return the feature we use to stick WMS into...
     */
    private static class WWFilter implements FeatureList.FeatureFilter {

        @Override
        public boolean matches(Feature f) {
            if (f instanceof WorldwindStockFeature) {
                return true;
            }
            return false;
        }
    }

    /**
     * Get the feature we use to stick WMS into...
     */
    public WorldwindStockFeature getWMSFeature() {
        WorldwindStockFeature f = FeatureList.theFeatures.getTopMatch(new WWFilter());
        if (f != null) {
            return f;
        }
        return null;
    }

    /**
     * Return a copy of the current completely loaded layers...note they may
     * still be loading more.
     */
    public ArrayList<LayerInfo> getLayers() {
        ArrayList<LayerInfo> newList = new ArrayList<LayerInfo>();
        synchronized (myListLock) {
            for (LayerInfo l : layerInfos) {
                newList.add(l);
            }
        }
        return newList;
    }
    protected final Object myListLock = new Object();
    protected final ArrayList<LayerInfo> layerInfos = new ArrayList<LayerInfo>();

    protected static class LayerInfo {

        protected WMSCapabilities caps;
        protected AVListImpl params = new AVListImpl();
        protected Object myComponent;
        
        protected String getTitle() {
            return params.getStringValue(AVKey.DISPLAY_NAME);
        }

        protected String getName() {
            return params.getStringValue(AVKey.LAYER_NAMES);
        }

        protected String getAbstract() {
            return params.getStringValue(AVKey.LAYER_ABSTRACT);
        }
        
        protected Object getComponent(){
            return myComponent;
        }
        
        protected void setComponent(Object component){
            myComponent = component;
        }
    }

    public WMSSource(String niceName, URL aURL) {
        super(niceName, aURL);

    }

    @Override
    public synchronized boolean isConnecting() {
        return myConnecting;
    }

    public synchronized void setConnecting(boolean flag) {
        myConnecting = flag;
    }

    @Override
    public synchronized boolean isConnected() {
        return myConnected;
    }

    /**
     * Create a new historical index around path, or null on failure. Note: the
     * gui calls this in a separate thread, so synchronize if needed. The GUI
     * knows not to access this IndexWatcher until connect completes.
     */
    @Override
    public synchronized boolean connect() {

        //setConnecting(true);  If we do this here, GUI doesn't get chance to update
        if (!isConnecting()) {  // If called by someone other than GUI, just turn it on now.
            setConnecting(true);
        }

        boolean success = false;

        WMSCapabilities caps;

        try {
            URI u = this.getURL().toURI();
            caps = WMSCapabilities.retrieve(u);
            caps.parse();
        } catch (Exception e) {
            log.error("Exception connecting to URL " + getURL() + e.toString());
            return false;
        }

        // Gather up all the named layers and make a world wind layer for each.
        final List<WMSLayerCapabilities> namedLayerCaps = caps.getNamedLayers();
        if (namedLayerCaps == null) {
            log.error("No layers on WMS server");
            // actually we don't care
            return true;
        }

        try {
            for (WMSLayerCapabilities lc : namedLayerCaps) {
                Set<WMSLayerStyle> styles = lc.getStyles();
                if (styles == null || styles.isEmpty()) {
                    LayerInfo layerInfo = createLayerInfo(caps, lc, null);
                    synchronized (myListLock) {  // Add the finished layer info
                        layerInfos.add(layerInfo);
                    }
                } else {
                    for (WMSLayerStyle style : styles) {
                        LayerInfo layerInfo = createLayerInfo(caps, lc, style);
                        synchronized (myListLock) {
                            layerInfos.add(layerInfo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error reading WMS layers " + e.toString());
            return false;
        }

        success = true;

        //  setConnecting(false);  GUI will set..hummm
        myConnected = success;

        return success;
    }

    public Object getLayerFromName(String name) {
        LayerInfo out = null;
        Object component = null;
        synchronized (myListLock) {
            for (LayerInfo i : layerInfos) {
                if (i.getName().equals(name)) {
                    out = i;
                }
            }
        }
        if (out != null) {
            component = out.getComponent();
            if (component == null){
                component = createComponent(out.caps, out.params);
                out.setComponent(component);
            }        
        }
        return component;  // Humm creates EACH time called... FIXME?
    }

    protected static Object createComponent(WMSCapabilities caps, AVList params) {
        AVList configParams = params.copy(); // Copy to insulate changes from the caller.

        // Some wms servers are slow, so increase the timeouts and limits used by world wind's retrievers.
        configParams.setValue(AVKey.URL_CONNECT_TIMEOUT, 30000);
        configParams.setValue(AVKey.URL_READ_TIMEOUT, 30000);
        configParams.setValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, 60000);

        try {
            String factoryKey = getFactoryKeyForCapabilities(caps);    
            gov.nasa.worldwind.Factory factory = (gov.nasa.worldwind.Factory) WorldWind.createConfigurationComponent(factoryKey);
            return factory.createFromConfigSource(caps, configParams);
        } catch (Exception e) {
            // Ignore the exception, and just return null.
        }

        return null;
    }

    protected static String getFactoryKeyForCapabilities(WMSCapabilities caps) {
        boolean hasApplicationBilFormat = false;

        Set<String> formats = caps.getImageFormats();
        for (String s : formats) {
            if (s.contains("application/bil")) {
                hasApplicationBilFormat = true;
                break;
            }
        }

        return hasApplicationBilFormat ? AVKey.ELEVATION_MODEL_FACTORY : AVKey.LAYER_FACTORY;
    }

    protected LayerInfo createLayerInfo(WMSCapabilities caps, WMSLayerCapabilities layerCaps, WMSLayerStyle style) {
        // Create the layer info specified by the layer's capabilities entry and the selected style.

        LayerInfo linfo = new LayerInfo();
        linfo.caps = caps;
        linfo.params = new AVListImpl();
        linfo.params.setValue(AVKey.LAYER_NAMES, layerCaps.getName());
        if (style != null) {
            linfo.params.setValue(AVKey.STYLE_NAMES, style.getName());
        }
        String abs = layerCaps.getLayerAbstract();
        if (!WWUtil.isEmpty(abs)) {
            linfo.params.setValue(AVKey.LAYER_ABSTRACT, abs);
        }

        linfo.params.setValue(AVKey.DISPLAY_NAME, makeTitle(caps, linfo));

        return linfo;
    }

    protected static String makeTitle(WMSCapabilities caps, LayerInfo layerInfo) {
        String layerNames = layerInfo.params.getStringValue(AVKey.LAYER_NAMES);
        String styleNames = layerInfo.params.getStringValue(AVKey.STYLE_NAMES);
        String[] lNames = layerNames.split(",");
        String[] sNames = styleNames != null ? styleNames.split(",") : null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lNames.length; i++) {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            String layerName = lNames[i];
            WMSLayerCapabilities lc = caps.getLayerByName(layerName);
            String layerTitle = lc.getTitle();
            sb.append(layerTitle != null ? layerTitle : layerName);

            if (sNames == null || sNames.length <= i) {
                continue;
            }

            String styleName = sNames[i];
            WMSLayerStyle style = lc.getStyleByName(styleName);
            if (style == null) {
                continue;
            }

            sb.append(" : ");
            String styleTitle = style.getTitle();
            sb.append(styleTitle != null ? styleTitle : styleName);
        }

        return sb.toString();
    }

    /**
     * Used by GUI to set status to 'connecting' and update GUI before starting
     * connection
     */
    @Override
    public synchronized boolean aboutToConnect(boolean start) {
        setConnecting(start);
        return true;
    }

    /**
     * The small helper object for making this source
     */
    public static class Factory extends SourceFactory {

        /**
         * @return true if we handle this file type
         */
        @Override
        public boolean canHandleFileType(File f) {

            boolean canHandle;
            String t = f.getName().toLowerCase();
            // FIXME: Many get this from all of the Wdssii Index objects
            canHandle = ((t.endsWith(".xml"))
                    || (t.endsWith(".xml.gz"))
                    || (f.isDirectory())); // will use fam to watch for fml files
            return canHandle;
        }

        /**
         * @return type information on what files we can handle
         */
        @Override
        public Set<String> getHandledFileDescriptions() {
            TreeSet<String> d = new TreeSet<String>();
            d.add("WMS");
            return d;
        }

        @Override
        public GUIPlugInPanel createParamsGUI(SourcesURLLoadDialog d) {
            return new IndexSourceParamsGUI(d);
        }

        /**
         * Return true if we can create a source from this URL
         */
        @Override
        public boolean canCreateFromURL(URL aURL) {
            boolean canCreate = false;
            if (aURL != null) {
                boolean isIndex = IndexFactory.checkURLForIndex(aURL);
                if (isIndex) {
                    canCreate = true;
                }
            }
            return canCreate;
        }

        @Override
        public String getDialogDescription() {
            return "WMS";
        }
    }

    /**
     * Create a new GUI for this feature
     */
    @Override
    public SourceGUI createNewControls() {
        return new WMSSourceGUI(this);
    }

    @Override
    public String getSourceDescription() {

        return "Web Map Service Description";
    }

    /**
     * Get the shown type name for this source The GUI uses this to show what
     * type we think we are
     */
    @Override
    public String getShownTypeName() {
        return "WebMapService";
    }
}

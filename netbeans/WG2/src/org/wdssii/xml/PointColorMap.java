package org.wdssii.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a PointColorMap is a list of Point tags that define bins of a color map This
 * is the ParaView format
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "colormap")
public class PointColorMap extends ColorMapDef {

    private final static Logger LOG = LoggerFactory.getLogger(PointColorMap.class);
    /**
     * The IDL list, if available
     */
    public static PointColorMapList theIDLList = null;
    @XmlAttribute
    public String space;
    @XmlAttribute
    public String name;
    @XmlElement(name = "point")
    public List<Point> points = new ArrayList<Point>();

    /**
     * The list item we hold
     */
    @XmlRootElement(name = "point")
    public static class Point {

        @XmlAttribute
        public float r = 1.0f;
        @XmlAttribute
        public float g = 1.0f;
        @XmlAttribute
        public float b = 1.0f;
        @XmlAttribute
        public float o = 0.0f;
        @XmlAttribute
        public float x = 0.0f;
    }

    /**
     * Simple list of ourselves, doesn't really need own file
     */
    @XmlRootElement(name = "doc")
    public static class PointColorMapList {

        //XmlAttribute(name="version") <doc version="
        //float versionID;
        @XmlElement(name = "colormap")
        public List<PointColorMap> colormaps = new ArrayList<PointColorMap>();
        private Map<String, PointColorMap> myLookup = null;

        private void createLookup() {
            if (myLookup == null) {
                myLookup = new TreeMap<String, PointColorMap>();
                for (PointColorMap m : colormaps) {
                    if (m.name != null) {
                        myLookup.put(m.name, m);
                        break;
                    }
                }
            }
        }

        /**
         * O(N)...could map this. Lookup a map in our stock maps by name. This
         * shouldn't be called much
         */
        public PointColorMap getByName(String name) {
            PointColorMap aMap = null;
            if (name != null) {
                for (PointColorMap m : colormaps) {
                    if ((m.name != null) && m.name.equalsIgnoreCase(name)) {
                        aMap = m;
                        break;
                    }
                }
            }
            if (aMap == null) {
                aMap = PointColorMap.getCandidate2();
            }
            return aMap;
        }
    }

    /**
     * Load the special stock maps we stole from ParaView. Open source is great,
     * eh?
     */
    public static void loadStockMaps() {
        PointColorMapList config;

        /**
         * The IDL maps
         */
        theIDLList = Util.load("All_idl_cmaps.xml", PointColorMapList.class);
        // Now try to read the stuff outta it....
        try {
            for (PointColorMap c : theIDLList.colormaps) {
                int count = c.points.size();
                //  LOG.debug("Got this info " + c.space + ", " + c.name + ", " + count);
                Point aPoint = c.points.get(1);
                // LOG.debug("First is " + aPoint.r + ", " + aPoint.g + ", " + aPoint.b);

            }
        } catch (Exception e) {
            LOG.debug("ERROR DURING TEST " + e);
        }

    }

    public static PointColorMap getCandidate2() {
        PointColorMap c = new PointColorMap();

        // double so I don't have to type f everywhere, lol
        double[] x = {0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1.0};
        double[] reds = {0.0196078, 0.129412, 0.26745, 0.572549, 0.819608, 0.968627, 0.992157, 0.956863, 0.839216, 0.698039, 0.403922};
        double[] greens = {0.188235, 0.4, 0.576471, 0.772549, 0.898039, 0.968627, 0.858824, 0.647059, 0.376471, 0.0941176, 0.0};
        double[] blues = {0.380392, 0.67451, 0.764706, 0.870588, 0.941176, 0.968627, 0.780392, 0.509804, 0.301961, 0.168627, 0.121569};
        for (int i = 0; i < x.length; i++) {
            Point p = new Point();
            p.x = (float) (x[i]);
            p.r = (float) (reds[i]);
            p.g = (float) (greens[i]);
            p.b = (float) (blues[i]);
            c.points.add(p);
        }
        return c;
    }
}

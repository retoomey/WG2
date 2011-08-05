package org.wdssii.core;

import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * A collection of radar information snagged from various sources
 * 
 * Kind of a hack class, this acts as a web spider, gathering information
 * on radars off the web.
 * 
 * @author Robert Toomey
 */
public class RadarInfo {

    /** The public conus page with the div tags containing the little rectangles */
    public final String CONUS_SHTML = "http://wdssii.nssl.noaa.gov/web/wdss2/products/radar/systems/w2vcp.shtml";
    
    /** The public csv file containing vcp info, latency info */
    public final String RADAR_CSV = "http://wdssii.nssl.noaa.gov/web/wdss2/products/radar/systems/vcp/radartable.csv";
    
    /** Information for a single radar */
    public static class ARadarInfo {

        public String id; // Such as "Albuquerque, NM";
        public String style; // Such as "position:absolute; width:18px;..."
        public int left;
        public int top;
        public int width = 18;
        public int height = 18;

        public void setStyle(String text) {
            style = text;

            // Format of style something like:
            // position:absolute; width:18px; height:18px; z-index:2; left: 385px; top: 143px;         
            String noSpace = text.replaceAll(" ", "");
            String[] list = noSpace.split(";");
            // --> "position:absolute", "width:18px", ....
            for (String s : list) {
                String[] nameValue = s.split(":");
                // "position:absolute" --> "position" "absolute"
                if (nameValue.length == 2) {

                    // Could stick each name/value into a map here:
                    // myMap.put(nameValue[0], nameValue[1]);
                    // But we just want the left/top...
                    String v = nameValue[1];
                    try {
                        if (nameValue[0].equalsIgnoreCase("left")) {
                            v = v.replaceAll("px", "");
                            left = Integer.parseInt(v);
                        } else if (nameValue[0].equalsIgnoreCase("top")) {
                            v = v.replaceAll("px", "");
                            top = Integer.parseInt(v);
                        }
                    } catch (NumberFormatException e) {
                        // flag location as bad?
                    }
                }
            }
        }

        public String getRadarName(String text) {
            // Text of the form:"../QCNN/KABR/index.shtml"
            // We want the radar name 'KABR'
            String[] fields = text.split("/");
            if (fields.length == 4) {
                return fields[2];
            }
            return "";
        }

        public int getLeft() {
            return left;
        }

        public int getTop() {
            return top;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public boolean hitTest(int x, int y) {
            boolean hit = false;
            if ((x >= left) && (x <= left + width)) {
                if ((y >= top) && (y <= top + height)) {
                    hit = true;
                }
            }
            return hit;
        }
    }
    private Map<String, ARadarInfo> myRadarInfos = new TreeMap<String, ARadarInfo>();

    public Map<String, ARadarInfo> getRadarInfos() {
        return myRadarInfos;
    }

    public void gatherRadarInfo() {

        try {
            // Try to parse the page...get the div tags with the radar info
            URL bURL = new URL(CONUS_SHTML);
            // bURL.openStream();
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader parser = factory.createXMLStreamReader(bURL.openStream());
            processConusPage(parser);
        } catch (Exception e) {
        }
    }

    // Can read the page at :
    // CONUS_SHTML
    // Assumes:
    // <html> as start...
    //  (n) <div id=, 
    // Not sure this should be a 'Tag' subclass, since it's a hack
    // (forcing it to read HTML instead of XHTML)
    //
    // The solution to this hack is to get the left/top added to the
    // radartable.csv file.  Need to talk to some people to get this done...
    //  http://wdssii.nssl.noaa.gov/web/wdss2/products/radar/systems/vcp/radartable.csv
    // It would be nice if the radartable.csv file contained the left/top
    // or if we knew the exact lat/lon dimensions of the image...
    /** Process this tag as a document root.  Basically skip any information
     * until we get to our tag.  In STAX, the first event is not a start
     * tag typically.
     * @param p the stream to read from
     * @return true if tag was found and processed
     */
    private boolean processConusPage(XMLStreamReader p) {
        boolean found = false;
        boolean keepGoing = true;
        while (keepGoing) {
            try {
                while (p.hasNext()) {
                    int event = p.next();
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            String tag = p.getLocalName();
                            // Found a div tag....
                            if ("div".equals(tag)) {

                                ARadarInfo div = new ARadarInfo();
                                // Get the div attributes...
                                Map<String, String> m = new TreeMap<String, String>();
                                processAttributes(p, m);
                                div.id = (String) m.get("id");
                                div.setStyle(m.get("style"));

                                // Next tag should be '<a'...
                                p.nextTag();
                                tag = p.getLocalName();
                                Map<String, String> m2 = new TreeMap<String, String>();
                                processAttributes(p, m2);
                                String radar = div.getRadarName((String) m2.get("href"));

                                if (radar != null) {

                                    // FIXME: mod instead of new always?
                                    myRadarInfos.put(radar, div);
                                }
                                found = true;
                            }
                        }
                    }
                }
            } catch (XMLStreamException ex) {
                // Ok, this can happen because HTML doesn't always have END TAGS.
                // and isn't properly XHTML formated.  Fine, we only care about
                // the <div> tags and the attributes anyway, so keep going...
                try {
                    if (!p.hasNext()) {
                        keepGoing = false;
                    }
                } catch (XMLStreamException z) {
                    // We're out of luck, end it..
                    keepGoing = false;
                }
            }
        }
        return found;
    }

    protected static void processAttributes(XMLStreamReader p, Map<String, String> buffer) {
        int count = p.getAttributeCount();
        for (int i = 0; i < count; i++) {
            QName attribute = p.getAttributeName(i);
            String name = attribute.toString();
            String value = p.getAttributeValue(i);
            buffer.put(name, value);
        }
    }

    /** Find radar at given (x,y) (Using the CONUS image from web) */
    public String findRadarAt(int x, int y) {
        String newHit = "";
        for (Map.Entry<String, ARadarInfo> entry : myRadarInfos.entrySet()) {
            //Gonna have to draw em...
            ARadarInfo d = entry.getValue();
            String key = entry.getKey();
            if (d.hitTest(x, y)) {
                newHit = key;
                break;
            }
        }
        return newHit;
    }
}

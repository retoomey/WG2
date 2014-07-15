package org.wdssii.gui;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        // Information gathered from radarcsv.txt
        public float latencySecs;
        public int unknown;
        public int vcp = -1;

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

        // Our legend.  FIXME: need text I think too
        public Color getColor() {
            // Color based on latency...
            if (latencySecs <= 5 * 60) {
                return Color.GREEN;
            }
            if (latencySecs <= 10 * 60) {
                return Color.YELLOW;
            }
            if (latencySecs <= 30 * 60) {
                return Color.RED;
            }
            if (latencySecs <= 60 * 60) {
                return new Color(139, 0, 204); // Deep purple
            }
            if (latencySecs <= 8 * 60 * 60) {
                return new Color(223, 0, 255); // Psychedelic purple 
            }
            return Color.WHITE;
        }

        public int getVCP() {
            return vcp;
        }

        public String getVCPString() {
            return String.valueOf(vcp);
        }

        public boolean hitTest(int x, int y) {
            boolean hit = false;
            Rectangle2D r = getRect();
            if ((x >= r.getX()) && (x <= r.getX() + r.getWidth())) {
                if ((y >= r.getY()) && (y <= r.getY() + r.getHeight())) {
                    hit = true;
                }
            }
            return hit;
        }

        public Rectangle2D getRect() {
            int halfWidth = width / 2;
            int halfHeight = height / 2;
            Rectangle2D r = new Rectangle(left - halfWidth, top - halfHeight, width, height);
            return r;
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

            bURL = new URL(RADAR_CSV);
            InputStream a = bURL.openStream();
            processRadarCSV(a);
        } catch (Exception e) {
            // Lots of exception catching and no action.  This is usually
            // considered bad code, but the point of the GUI is to not freak
            // out when things go bad.  We know that the radar info may be
            // partial or incomplete based on web access, etc.
        }
    }

    private void processRadarCSV(InputStream i) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(i));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            // input line of form:
            // 73.415 18000 36.25 -86.56 KOHX 21 
            // latency, unknown, lat, lon, name, vcp
            String[] fields = inputLine.split(" ");
            if (fields.length > 5) {
                String name = fields[4];
                ARadarInfo info = myRadarInfos.get(name);
                if (info != null) {
                    info.latencySecs = Float.parseFloat(fields[0]);
                    info.vcp = Integer.parseInt(fields[5]);
                }
            }
        }
        in.close();
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
     * @return number of found radar div tags
     */
    private int processConusPage(XMLStreamReader p) {
        int found = 0;

        try {
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

                                    // Kurt added div tags with images..these
                                    // have no 'id' field
                                    if (div.id != null) {
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
                                        found++;
                                    }
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
                    } catch (Exception z) {
                        // We're out of luck, end it..
                        keepGoing = false;
                    }
                }
            }
        } catch (Exception e) {
            // any exception just finish...keep what we got.
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

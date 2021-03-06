package org.wdssii.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * SourceBookmarks handles access to a list of radar data sources from 
 * a URL XML formatted file. (This is the XML page created by script for
 * the status of realtime/archive radars in the lab)
 * 
 * @author Robert Toomey
 */
public class SourceBookmarks {

    /**
     * Holder of an individual source bookmark
     * 
     * @author Robert Toomey
     * 
     */
    public static class BookmarkURLSource {

        public String name;
        public String group;
        public String type;
        public String location;
        public String path;
        public String time; // Time date string or 'missing' or anything
        public Date date; // Actual date object if time parsable
        public String selections;
    }

    /** Holder of data from a URL containing source data information.
    This could be in a non-gui class.
     */
    public static class BookmarkURLData {

        /** The set of bookmarks */
        public ArrayList<BookmarkURLSource> data = new ArrayList<BookmarkURLSource>();
        /** Set of groups in the bookmarks, used for filtering, such
        as 'Realtime', 'Archive'
         */
        public TreeSet<String> groups = new TreeSet<String>();
    }

    public static BookmarkURLData getBookmarksFromURL(URL aURL) {
    	final Logger LOG = LoggerFactory.getLogger(BookmarkURLData.class);
    	
        BookmarkURLData b = new BookmarkURLData();
        try {

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

            InputStream stream = aURL.openStream();
            Document doc = docBuilder.parse(stream);

            doc.getDocumentElement().normalize();
            // System.out.println("Root element of the doc is "
            //         + doc.getDocumentElement().getNodeName());

            // Read in the FULL list. We will filter later by group
            NodeList indexesXML = doc.getElementsByTagName("index");

            for (int i = 0; i < indexesXML.getLength(); i++) {
                Element anIndex = (Element) indexesXML.item(i);
                BookmarkURLSource data = new BookmarkURLSource();
                data.name = anIndex.getAttribute("name");
                data.group = anIndex.getAttribute("group");

                data.location = anIndex.getAttribute("location");
                data.path = anIndex.getAttribute("path");
                String aTime = anIndex.getAttribute("time");
                try{
                    long l = Long.parseLong(aTime)*1000;
                    Date d = new Date(l);
                    data.date = d;
                    data.time = d.toString();
                }catch(Exception e){
                    // Ok...just use string instead of date...
                    data.date = new Date(0); // Oldest date
                    data.time = aTime;
                }
                //data.time = anIndex.getAttribute("time");
                data.selections = anIndex.getAttribute("selections");

                // The no group bug. Hack around it until script is fixed, if
                // ever
                if (!data.group.equalsIgnoreCase("nogroup")) {
                    b.data.add(data);
                    if (data.group != null) {
                        b.groups.add(data.group);
                    }
                }
            }
            Collections.sort(b.data, new Comparator<BookmarkURLSource>() {

                @Override
                public int compare(BookmarkURLSource arg0, BookmarkURLSource arg1) {
                    return (arg0.name.compareTo(arg1.name));
                }
            });
            
        } catch (SAXException e) { // FIXME: figure out each exception what to
            LOG.error("SAX XML parse exception reading the bookmark xml file");
        } catch (ParserConfigurationException e) {
        } catch (MalformedURLException e) {
            LOG.error("Exception reading URL " + e.toString());
        } catch (IOException io) {
            LOG.error("IO Exception reading URL " + io.toString());
        }
        return b;
    }

    /** Generate a list of fake bookmarks for testing any GUI interface */
    public static BookmarkURLData getFakeBookmarks(int markCount, int groupCount) {

        BookmarkURLData b = new BookmarkURLData();
        Date d = new Date();
        for (int i = 0; i < markCount; i++) {
            BookmarkURLSource data = new BookmarkURLSource();
            data.name = "name" + i;
            data.group = "group " + i % groupCount;
            data.location = "location" + i;
            data.path = "path" + i;
            data.time = "time" + i;
            data.date = d;
            data.selections = "selections" + i;
            b.data.add(data);
        }
        for (int j = 0; j < groupCount; j++) {
            b.groups.add("group " + j);
        }
        return b;
    }
}

package org.wdssii.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * PointSet stores a collection of points and the data with it for the polygon editor
 * 
 * Points have 'polygon' group numbers which can be used or ignored.  They
 * will be sorted increasing with possible missing values (2..4...7...8..9)
 * 
 * @author Robert Toomey
 *
 */
@XmlRootElement(name = "pointset")
public class PointSet {
	
    @XmlElement(name = "point")
    public List<Point> points = new ArrayList<Point>();
    
    /**
     * The list item we hold
     */
    @XmlRootElement(name = "point")
    public static class Point {

        @XmlAttribute
        public double latitude = 0.0f;
        @XmlAttribute
        public double longitude = 0.0f;
        @XmlAttribute
        public int polygon = 0;
        @XmlAttribute
        public String note;
        
        public Point(double lat, double lon, int p, String n){
         	latitude = lat;
        	longitude = lon;
        	polygon = p;
        	note = n;
        }
        
        /** for JAXB */
        public Point(){
        	
        }
    }

    public void add(double lat, double lon, int polygon, String note){
    	Point p = new Point(lat, lon, polygon, note);
    	points.add(p);
    }
    
    /** for Jaxb */
    public PointSet(){
    
    }
}

package org.wdssii.xml;

import java.util.ArrayList;
import javax.xml.stream.XMLStreamReader;

/**
 *  Tag which has the following format:
 * 
 * <pre>
 * {@code
 *  <colorMap min=float, max=float>
 * (n) <colorBin>
 * </colorMap>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_colorMap extends Tag {

    // ----------------------------------------------------------------------
    // Reflection <colorMap upperBound= name=
    // Attributes
    public float min = 0;   // Min and Max range of point data for dynamic maps
    public float max = 256;
    public ArrayList<Tag_colorBin> colorBins = new ArrayList<Tag_colorBin>();
    public Tag_unit unit = new Tag_unit();
    // End Reflection
    // ----------------------------------------------------------------------
     public ArrayList<Tag_Point> Points = new ArrayList<Tag_Point>();
    // End Reflectionpoints
    // ----------------------------------------------------------------------
    
    /** Point array for a 'matlab' style of color map.. */
    public static class Tag_Point extends Tag {
        public float x;
        public String o;
        public float r;
        public float g;
        public float b;
        
        public Tag_Point(){
            int a = 1;
            
        }
    }
    
    /** Creating a color bin tag with the stuff from
     * http://www.paraview.org/ParaView3/index.php/Default_Color_Map
     * Eventually I'll read these from xml files...this is an experiment.
     * I'm creating a tag so I know the rest of code is ready for a file instead
     * @return 
     */
    public static Tag_colorMap getCandidate2(){
        Tag_colorMap c = new Tag_colorMap();
        
        // double so I don't have to type f everywhere, lol
        double[] x={0, .1, .2, .3, .4, .5, .6, .7, .8, .9, 1.0};
        double[] reds = {0.0196078, 0.129412, 0.26745,  0.572549, 0.819608, 0.968627, 0.992157, 0.956863, 0.839216, 0.698039, 0.403922 };
        double[] greens={0.188235,  0.4,      0.576471, 0.772549, 0.898039, 0.968627, 0.858824, 0.647059, 0.376471, 0.0941176, 0.0};
        double[] blues ={0.380392,  0.67451,  0.764706, 0.870588, 0.941176, 0.968627, 0.780392, 0.509804, 0.301961, 0.168627, 0.121569 };
        for(int i = 0; i< x.length;i++){
            Tag_Point p = new Tag_Point();
            p.x = (float)(x[i]);
            p.r = (float)(reds[i]);
            p.g = (float)(greens[i]);
            p.b = (float)(blues[i]);
            c.Points.add(p);
        }
        return c;
    }
}


package org.wdssii.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The WDSSII color map format.. These were stored in files by name of the
 * product
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "colormap")
public class W2ColorMap extends ColorMapDef {

    private static Logger log = LoggerFactory.getLogger(W2ColorMap.class);
    @XmlElement(name = "colorbin")
    public List<W2ColorBin> colorBins = new ArrayList<W2ColorBin>();
    @XmlElement(name = "unit")
    public W2Unit unit;

    @XmlRootElement(name = "unit")
    public static class W2Unit {

        @XmlAttribute
        public String name = "";
    }

    /**
     * The color bin we hold
     */
    @XmlRootElement(name = "colorbin")
    public static class W2ColorBin {

        @XmlRootElement(name = "color")
        public static class W2Color {

            @XmlAttribute
            public Integer r = 0;  // Have to use Integer to use our hex wrapper
            @XmlAttribute
            public Integer g = 0;
            @XmlAttribute
            public Integer b = 0;
            @XmlAttribute
            public Integer a = 255;
            @XmlAttribute
            public String name = null;
        }
        @XmlAttribute(name="upperbound")
        public Float upperBound;
        @XmlAttribute
        public String name;
        @XmlElement(name = "color")
        public List<W2Color> colors = new ArrayList<W2Color>();
        @XmlElement(name = "unit")
        public W2Unit unit;
    }
}

package org.wdssii.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * The WDSSII color map format.. These were stored in files by name of the
 * product
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "colormap")
public class W2ColorMap extends ColorMapDef {

    private final static Logger LOG = LoggerFactory.getLogger(W2ColorMap.class);
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

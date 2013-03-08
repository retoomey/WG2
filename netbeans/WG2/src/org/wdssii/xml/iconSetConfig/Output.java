package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Tag which has the following format:
 *
 * A Really bad tag name for a subtag of mesonetConfig, but I'm stuck with
 * legacy data files...so here it is
 *
 * <pre>
 * {@code
 * <output>
 *
 * </output>
 * }
 * </pre>
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "output")
public class Output {

    @XmlElement(name = "windbarb")
    public WindBarb windBarb;
    @XmlElement(name = "airtemperature")
    public AirTemperature airTemperature;
    @XmlElement(name = "dewtemperature")
    public DewTemperature dewTemperature;
    @XmlElement(name = "precipitation")
    public Precipitation precipitation;
    @XmlElement(name = "label")
    public Label label;

    @XmlRootElement(name = "airtemperature")
    public static class AirTemperature {

        @XmlAttribute(name = "unit")
        public String unit;
        @XmlAttribute(name = "color")
        public String color;
        @XmlAttribute(name = "format")
        public String format;
        @XmlAttribute(name = "textheight")
        public int textHeight;
    }

    @XmlRootElement(name = "dewtemperature")
    public static class DewTemperature {

        @XmlAttribute(name = "unit")
        public String unit;
        @XmlAttribute(name = "color")
        public String color;
        @XmlAttribute(name = "format")
        public String format;
        @XmlAttribute(name = "textheight")
        public int textHeight;
    }

    @XmlRootElement(name = "precipitation")
    public static class Precipitation {

        @XmlAttribute(name = "unit")
        public String unit;
        @XmlAttribute(name = "color")
        public String color;
        @XmlAttribute(name = "format")
        public String format;
        @XmlAttribute(name = "textheight")
        public int textHeight;
    }

    @XmlRootElement(name = "label")
    public static class Label {

        @XmlAttribute(name = "unit")
        public String unit;
        @XmlAttribute(name = "color")
        public String color;
        @XmlAttribute(name = "format")
        public String format;
        @XmlAttribute(name = "textheight")
        public int textHeight;
    }
}

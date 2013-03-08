package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Tag which has the following format:
 *
 * <pre>
 * {@code
 * <windBarb speedUnit= barbRadius= color= crossHairRadius= hMargin= vMargin=>
 * }
 * </pre>
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "windbarb")
public class WindBarb {

    @XmlAttribute(name = "speedunit")
    public String speedUnit;
    @XmlAttribute(name = "barbradius")
    public int barbRadius = 10;
    @XmlAttribute(name = "color")
    public String color;
    @XmlAttribute(name = "crosshairradius")
    public int crossHairRadius = 5;
    @XmlAttribute(name = "hmargin")
    public int hMargin = 5;
    @XmlAttribute(name = "vmargin")
    public int vMargin = 5;
    @XmlElement(name = "superunit")
    public SuperUnit superUnit = new SuperUnit();
    @XmlElement(name = "baseunit")
    public BaseUnit baseUnit = new BaseUnit();
    @XmlElement(name = "halfunit")
    public HalfUnit halfUnit = new HalfUnit();

    @XmlRootElement(name = "superunit")
    public static class SuperUnit {

        @XmlAttribute(name = "value")
        public int value;
        @XmlAttribute(name = "tolerance")
        public int tolerance;
    }

    @XmlRootElement(name = "baseunit")
    public static class BaseUnit {

        @XmlAttribute(name = "value")
        public int value;
        @XmlAttribute(name = "tolerance")
        public int tolerance;
    }

    @XmlRootElement(name = "halfunit")
    public static class HalfUnit {

        @XmlAttribute(name = "value")
        public int value;
        @XmlAttribute(name = "tolerance")
        public int tolerance;
    }
}

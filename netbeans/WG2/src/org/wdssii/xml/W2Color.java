package org.wdssii.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * W2Color is shared by ColorBin and ColorDef
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "color")
public class W2Color {

    @XmlAttribute
    public Integer r = 0;  // Have to use Integer to use our hex wrapper
    @XmlAttribute
    public Integer g = 0;
    @XmlAttribute
    public Integer b = 0;
    @XmlAttribute
    public Integer a = 255;
    @XmlAttribute(required = false)
    public String name = null;
}

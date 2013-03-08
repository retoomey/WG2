package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.wdssii.xml.W2ColorMap;

/**
 * Tag which has the following format:
 *
 * <pre>
 * {@code
 * <textConfig textField=, dcColumn=, dcUnit=>
 * </textConfig>
 * }
 * </pre>
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "textconfig")
public class TextConfig {

    @XmlAttribute(name = "textfield")
    public String textField;
    @XmlAttribute(name = "dccolumn")
    public String dcColumn;
    @XmlAttribute(name = "dcunit")
    public String dcUnit;
    
    // Seen xmlattribute color="name" where color is from color databse..
    
    @XmlElement(name="colormap",required=false)
    public W2ColorMap colorMap = new W2ColorMap();
}

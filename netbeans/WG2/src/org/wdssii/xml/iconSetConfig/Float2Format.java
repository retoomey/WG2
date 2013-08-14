package org.wdssii.xml.iconSetConfig;

import java.util.IllegalFormatException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Float2Format has two formats where
 *
 * if (num < threshhold){ format1 }else{ format2 }
 *
 * a more advanced number text formatter might be needed later, but this seems
 * to cover the usage in the display
 *
 * @author Robert Toomey
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "float2format")
public class Float2Format extends TextFormat {

    /**
     * The threshold for changing precision
     */
    @XmlAttribute(name = "threshold")
    public float threshold = .05f;
    @XmlAttribute(name = "lowformat")
    public String lowformat;

    /**
     * Format a list or args using stored format
     */
    public String format(float value, Object... args) {
        String output;
        try {
            if (value <= threshold) {
                output = String.format(format, args);
            } else {
                output = String.format(lowformat, args);
            }
        } catch (IllegalFormatException e) {  // how to handle.?
            output = "";
        }
        return output;
    }
}

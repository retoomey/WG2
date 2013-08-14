package org.wdssii.xml.iconSetConfig;

import java.util.IllegalFormatException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * TextFormat contains a savable java/c style format string
 * Example: "%5.2f testing", etc..
 * 
 * @author Robert Toomey
 */
@XmlRootElement(name = "textformat")
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({Float2Format.class})
public class TextFormat {

    @XmlAttribute(name = "format")
    public String format;
    
    /** Format a list or args using stored format */
    public String format(Object ... args){
        String output;
        try {
            output = String.format(format, args);
        }catch (IllegalFormatException e){  // how to handle.?
            output="";
        }
        return output;
    }
}

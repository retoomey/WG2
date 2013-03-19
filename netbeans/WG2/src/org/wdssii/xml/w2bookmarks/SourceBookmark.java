package org.wdssii.xml.w2bookmarks;

import java.util.Date;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "index")
public class SourceBookmark {

    @XmlAttribute(name = "name")
    public String name;
    @XmlAttribute(name = "group")
    public String group;
    @XmlAttribute(name = "type")
    public String type;
    @XmlAttribute(name = "location")
    public String location;
    @XmlAttribute(name = "path")
    public String path;
    @XmlAttribute(name = "time")
    public String time; // Time date string or 'missing' or anything
    @XmlAttribute(name = "selections")
    public String selections;
    public Date date; // Store actual date object if time parsable
}

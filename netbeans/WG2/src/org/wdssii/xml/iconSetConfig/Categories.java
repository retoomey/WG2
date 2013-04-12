package org.wdssii.xml.iconSetConfig;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Categories link value in a table column(s) to a symbol
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "categories")
@XmlAccessorType(XmlAccessType.NONE)
public class Categories {

    /** The attribute column of the category */
    @XmlAttribute(name = "column", required = true)
    public String column;
    
    /** The attribute for multiple column lookup */
    @XmlAttribute(name = "column2", required = false)
    public String column2 = null;
    
    /** The attribute for multiple column lookup */
    @XmlAttribute(name = "column3", required = false)
    public String column3= null;
    
    @XmlElement(name = "category", required = true)
    public List<Category> list; // = new ArrayList<Category>();

    public void addCategory(Category c){
        if (list == null){ list = new ArrayList<Category>(); }
        list.add(c);
    }
}

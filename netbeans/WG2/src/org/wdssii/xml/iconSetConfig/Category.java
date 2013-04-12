package org.wdssii.xml.iconSetConfig;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A Category links a generated value from the categories column(s)
 * to a particular symbol.
 * 
 * @author Robert Toomey
 */
@XmlType(name = "category")
@XmlAccessorType(XmlAccessType.NONE)
public class Category {
    
    /** The attribute column value */
    @XmlAttribute(name = "value")
    public String value;

    /** The symbols for this category */
    @XmlElement
    public List<Symbol> symbols;
    
    /** Add symbol to our symbol list */
    public void addSymbol(Symbol s){
        if (symbols == null){
            symbols = new ArrayList<Symbol>();
        }
        symbols.add(s);
    }
}

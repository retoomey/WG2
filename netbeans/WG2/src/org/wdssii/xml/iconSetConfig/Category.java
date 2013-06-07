package org.wdssii.xml.iconSetConfig;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A Category links a generated value from the categories column(s) to a
 * particular symbol.
 *
 * @author Robert Toomey
 */
@XmlType(name = "category")
@XmlAccessorType(XmlAccessType.NONE)
public class Category {

    /**
     * The attribute column value
     */
    @XmlAttribute(name = "value")
    public String value;
    /**
     * The symbols for this category
     */
    @XmlElement
    public List<Symbol> symbols;

    /**
     * Add symbol to our symbol list
     */
    public void addSymbol(Symbol s) {
        if (symbols == null) {
            symbols = new ArrayList<Symbol>();
        }
        symbols.add(s);
    }

    public Category() {
    }
    
    /** Convenience */
    public Category(String v, Symbol symbol){
        value = v;
        addSymbol(symbol);
    }

    public Category(Category c) {
        value = c.value;
        if (c.symbols != null) {
            this.symbols = new ArrayList<Symbol>();
            for (Symbol sym : c.symbols) {
                this.symbols.add(sym.copy());
            }
        }
    }
    
    @Override
    public String toString(){
        String s = "Category "+this.hashCode()+" "+symbols+ " ";
        if (symbols != null){
            s += symbols.size()+" ";
            if (symbols.size() > 0){
                s += " symbol1: "+symbols.get(0);
            }        
        }
        return s;
    }
}

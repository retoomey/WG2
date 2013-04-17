package org.wdssii.xml.iconSetConfig;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Holds the symbols for the 'single' for all symbology
 *
 * For now at list, just a list of symbols FIXME: Could do without it I guess
 * just stick the list in symbology, gut telling me to make an xml group for it.
 *
 * @author Robert Toomey
 */
@XmlType(name = "single")
@XmlAccessorType(XmlAccessType.NONE)
public class Single {

    /**
     * The symbols for this single symbol
     */
    @XmlElement
    public List<Symbol> symbols;

    public Single() {
    }

    public Single(Single s) {
        if (s.symbols != null) {
            this.symbols = new ArrayList<Symbol>();
            for (Symbol sym : s.symbols) {
                this.symbols.add(sym.copy());
            }
        }
    }

    /**
     * Get the first symbol
     */
    public Symbol getSymbol() {
        Symbol s = null;
        if (this.symbols != null) {
            if (!this.symbols.isEmpty()) {
                s = this.symbols.get(0);
            }
        }
        return s;
    }

    /* Set the 'first' symbol */
    public void setSymbol(Symbol s) {
        if (this.symbols == null) {
            this.symbols = new ArrayList<Symbol>();
            this.symbols.add(s);
        } else {
            if (this.symbols.isEmpty()) {
                this.symbols.add(s);
            } else {
                this.symbols.set(0, s);
            }
        }
    }
}

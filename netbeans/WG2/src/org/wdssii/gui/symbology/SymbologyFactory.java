package org.wdssii.gui.symbology;

import org.wdssii.gui.symbology.CategoryUniqueValues;
import org.wdssii.gui.symbology.SingleSymbol;
import org.wdssii.gui.symbology.SymbologyGUI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * Creating SymbologyGUIs from name or Symbology
 *
 * @author Robert Toomey
 */
public final class SymbologyFactory {

    /** Static utilities, prevent creation */
    private SymbologyFactory() {
        
    }
    private final static Logger LOG = LoggerFactory.getLogger(SymbologyFactory.class);

    /**
     * A list of classes matching the Symbology array
     */
    private static final Class theEditors[] = {
        SingleSymbol.class,
        CategoryUniqueValues.class
    };

 
    /**
     * From list selection, get symbologyGUI
     */
    public static SymbologyGUI getSymbologyByName(String name) {
        SymbologyGUI s = null;

        final int aSize = Symbology.getTypeLength();
        for (int i = 0; i < aSize; i++) {
            String candidate = Symbology.getType(i);
            if (candidate.equalsIgnoreCase(name)) {
                try {
                    s = (SymbologyGUI) theEditors[i].newInstance();
                } catch (Exception e) {
                    // This 'should not' happen unless the array at top
                    // does not match the array length in the xml Symbology.
                }
                break;
            }
        }
        return s;
    }

    /**
     * From a symbology get symbologyGUI
     */
    public static SymbologyGUI getSymbologyGUIFor(Symbology s) {
        SymbologyGUI sgui = null;
        if (s != null) {
            
             try {
                  sgui = (SymbologyGUI) theEditors[s.use].newInstance();
                } catch (Exception e) {
                    // We might be too 'old'  
                    LOG.debug("Can't create SymbologyGUI for this Symbology value "+s.use);
                    LOG.debug("Possibly this version of the program is older than the symbology xml file?");
                }  
        }
        return sgui;
    }

    /**
     * List of possible Symbology we can use
     * 
     * FIXME: Filter the global list based on given datatype
     */
    public static List<String> getSymbologyNameList() {

        final ArrayList<String> list = new ArrayList<String>();

        final int aSize = Symbology.getTypeLength();
        for (int i = 0; i < aSize; i++) {
            list.add(Symbology.getType(i));
        }
        return list;
    }
}

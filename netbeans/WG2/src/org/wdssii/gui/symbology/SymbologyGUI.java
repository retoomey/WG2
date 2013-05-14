package org.wdssii.gui.symbology;

import javax.swing.JScrollPane;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.wdssii.datatypes.AttributeTable;
import org.wdssii.gui.SwingGUIPlugInPanel;
import org.wdssii.xml.iconSetConfig.Symbology;

/**
 * One of the 'list' of Symbology subpanel types
 *
 * @author Robert Toomey
 */
public abstract class SymbologyGUI extends SwingGUIPlugInPanel {
    
    private SymbologyGUIListener myListener;
    
    public static interface SymbologyGUIListener {
        public void symbologyChanged();
    }
    
    /** Symbology object */
    protected Symbology mySymbology = null;
    
    /** Symbology has access to attribute table, will this be enough
     or will it need to be more general such as DataType?*/
    protected AttributeTable myAttributeTable = null;
    
    public SymbologyGUI() {
        //setupComponents();
    }
    
    public void addListener(SymbologyGUIListener l){
        myListener = l;  // Just one for now
    }
    
    public void notifyChanged(){
        if (myListener !=null){
            myListener.symbologyChanged();
        }
    }

    /** Get the symbology.use type that we edit for, each will have a unique number */
    public abstract int getType();
    
    /** Return the string used to define us in a list */
    public String getDisplayName() {
        return Symbology.getType(getType());
    }
    
    /**
     * Set up the components. We haven't completely automated this because you
     * never know what little change you need that isn't supported.
     */
    public void setupComponents() {
        
        // Default encloses root within a scroll pane
        JScrollPane s = new JScrollPane();
        s.setViewportView(this);
        setRootComponent(s);
        setLayout(new MigLayout(new LC(), null, null));
    }

    public void useSymbology(Symbology symbology, AttributeTable current) {
       mySymbology = symbology;
       myAttributeTable = current;
    }

}

package org.wdssii.xml;

import java.util.ArrayList;
import javax.xml.stream.XMLStreamReader;

/**
 *  Our <array> tag which has the following format:
 * 
 * <pre>
 * {@code
 * <array length="n">
 * (n) <T>
 * </array>
 * }
 * </pre>
 * 
 * @author Robert Toomey
 */
public class Tag_array<T> extends Tag {

    /** Factory to create a 'T' for us, since we have generic not template.*/
    public static abstract class ArraySubtagFactory {
        public abstract Object make();
        public abstract boolean processTag(Object o, XMLStreamReader p);
    }
    
    /** The stuff */
    public ArrayList<T> data = new ArrayList<T>();
    
    private ArraySubtagFactory factory = null;
    private Object theStuff = null;

    public Tag_array(ArraySubtagFactory F) {
        factory = F;
        theStuff = F.make();
    }

    /** Process all child tabs within our tag */
    @Override
    public void processChildren(XMLStreamReader p) {
        if (factory.processTag(theStuff, p)){
            // Humm could put this code in the factory?
            data.add((T)theStuff);  // Your factory better return the right type
            theStuff = factory.make();
        }
    }
}

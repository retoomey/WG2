package org.wdssii.datatypes.builders.xml;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.geom.Location;
import org.wdssii.xml.Tag_datatype;

/** Base class for reading/writing a DataType with XML using STAX
 * Shared basic tags are here for subclasses to use.
 * Reading is much more complicated than writing.
 * 
 * Every datatype we write out will have a datatype header with meta-data
 * 
 * We return a new object only when we have to, otherwise we fill in
 * the fields of a passed object (this helps speed during parsing which makes the GUI more responsive)
 * 
 * @author Robert Toomey
 *
 */
public abstract class DataTypeXML {

    public static class DataTypeXMLHeader {

        /** datatype name such as 'Mesonet' */
        public String datatype = null;
        /** The stref location */
        public Location location = new Location(0, 0, 0);
        /** The stref time  */
        public Date time = null;
        /** A map of attr name to value */
        public Map<String, String> attriNameToValue = new HashMap<String, String>();
        /**A map of attr name to units */
        public Map<String, String> attriNameToUnits = new HashMap<String, String>();
    }

    /** Holder class for unit/value attributes */
    public static class UnitValueXML {

        public String unit;
        public String value;
    }
    /** Global buffer to avoid newing on the fly */
    protected UnitValueXML uvXML = new UnitValueXML();

    /** Create a DataType from an XMLStreamReader.  Subclasses should override with
     * covariance 
     * @param p	the XMLStreamReader with the open XML
     * @return the new DataType or null on failure
     * @throws XMLStreamException
     */
    public abstract DataType createFromXML(XMLStreamReader p) throws XMLStreamException;
 
    public void TagToMemento(Tag_datatype t, DataTypeMemento d){
        d.typeName = t.name;
        d.startTime = t.stref.time;
        d.originLocation = t.stref.location;
        d.attriNameToValue = t.attrValues;
        d.attriNameToUnits = t.attrUnits;
    }
}

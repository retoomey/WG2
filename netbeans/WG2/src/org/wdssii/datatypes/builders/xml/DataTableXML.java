package org.wdssii.datatypes.builders.xml;

import java.util.ArrayList;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.wdssii.datatypes.DataTable;
import org.wdssii.datatypes.DataTable.Column;
import org.wdssii.datatypes.DataTable.DataTableMemento;
import org.wdssii.xml.Tag_datacolumn;
import org.wdssii.xml.Tag_datatable;

/** This creates a DataTable from XML data
 * 
 * @author Robert Toomey
 *
 */
public class DataTableXML extends DataTypeXML {

    // XML tags/names.  Make constants to ensure read/write use the same tag names
    protected final String XML_DATACOLUMN = "datacolumn";
    protected final String XML_DATATYPE = "datatype";
    protected final String XML_DATA = "data";
    // Memory while parsing an XML file.
    private ArrayList<Column> myWorkingColumns = null;

    /** Initialize a data table from an XMLStreamReader 
     * @throws XMLStreamException
     * Overrides with covariance */
    @Override
    public DataTable createFromXML(XMLStreamReader p) throws XMLStreamException {
        DataTable table = null;
        
         // Read in the document basically.
        Tag_datatable t = new Tag_datatable();
        t.processTag(p);
        
        // Create a memento and get data out of the tag we want...
        DataTableMemento c = new DataTableMemento();
        
        // Read top level datatype info into memento...
        TagToMemento(t.datatype, c);
        
        // This is SO wasteful of memory...need to compact this puppy
        ArrayList<Tag_datacolumn> cols = t.data.datacolumn;
        myWorkingColumns = new ArrayList<Column>();
        for(Tag_datacolumn d:cols){ 
           Column aCol = new Column(d.name, d.units, d.values);
           myWorkingColumns.add(aCol);
        }
        
        // Extra fields....
        c.columns = myWorkingColumns;
        
        table = new DataTable(c);
        return table;
    }

    protected void readXML_data(XMLStreamReader p) throws XMLStreamException {
        System.out.println("DataTable got the data tag...");
        // <data>....
        //  (n) <datacolumn name="DEWP" units="DegreeC"> 
        // </data>
        String startTag = p.getLocalName();
        while (p.hasNext()) {
            p.next();
            String tag = null;
            if (isEndTag(p, startTag)) {
                break;
                // Process any datacolumn tags
            } else if ((tag = haveStartTag(p)) != null) {
                if (XML_DATACOLUMN.equals(tag)) {
                    readXML_datacolumn(p);
                }
            }
        }
    }

    protected void readXML_datacolumn(XMLStreamReader p) throws XMLStreamException {
        System.out.println("DataTable got the data tag...");

        String columnName = "UNKNOWN";
        String columnUnits = "UNKNOWN";

        // <datacolumn name="name" units="units">
        int count = p.getAttributeCount();
        for (int i = 0; i < count; i++) {
            QName attribute = p.getAttributeName(i);
            String name = attribute.toString();
            String value = p.getAttributeValue(i);
            if ("name".equals(name)) {
                columnName = value;
            } else if ("units".equals(name)) {
                columnUnits = value;
            }
        }

        // Store each value in an array to start... this is 2*O(numrows)
        // We will try to 'smart' compress the data into String, int or double for the final column...
        ArrayList<String> items = new ArrayList<String>();

        // Look for <item value = "23.4"/>
        String startTag = p.getLocalName();
        while (p.hasNext()) {
            p.next();
            String tag = null;
            if (isEndTag(p, startTag)) {
                break;
            } else if ((tag = haveStartTag(p)) != null) {
                if ("item".equals(tag)) {

                    // Handle <item value="10.2"/>
                    count = p.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        QName attribute = p.getAttributeName(i);
                        String n = attribute.toString();
                        String v = p.getAttributeValue(i);
                        if ("value".equals(n)) {
                            //System.out.print(v+",");
                            items.add(v);
                        }
                    }
                    //System.out.println();
                }
            }
        }
        // FIXME: need to check here for integrity or failure (it's not very strong code yet)
        // Create the column object now that we're done
        Column newitems = new Column(columnName, columnUnits, items);
        myWorkingColumns.add(newitems);
    }
}

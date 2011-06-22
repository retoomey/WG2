package org.wdssii.datatypes.builders.xml;

import java.util.ArrayList;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.wdssii.datatypes.DataTable;
import org.wdssii.datatypes.DataTable.Column;
import org.wdssii.datatypes.DataType.DataTypeMemento;

/** The XML routines for DataTable.  This can read/write XML data for a DataTable
 * Uses Stax to read/write DataTable
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
        // Two top tags:
        // <datatable>
        //  (1) <datatype>  which is metadata on the table
        //  (1) <data> which is the column values
        // </datatable>
        DataTable table = null;
        DataTypeMemento header = null;
        try {
            myWorkingColumns = new ArrayList<Column>();
            String tag = null;
            String startTag = p.getLocalName();
            while (p.hasNext()) {
                p.next();
                if (isEndTag(p, startTag)) {
                    break;
                } else if ((tag = haveStartTag(p)) != null) {
                    if (XML_DATATYPE.equals(tag)) {
                        header = readXML_datatype(p);
                    } else if (XML_DATA.equals(tag)) {
                        readXML_data(p);
                    }
                }
            }
            System.out.println("Working columns " + myWorkingColumns);
            if (myWorkingColumns != null) {
                System.out.println("Number of columns is " + myWorkingColumns.size());
            }
            table = new DataTable(header, myWorkingColumns);

            System.out.println("XML CREATED TABLE LOCATION IS " + header.originLocation);
            System.out.println("--->TYPENAME " + header.typeName);
        } catch (Exception e) {
            // Recover on any exception by returning a null table
            System.out.println("XML PARSING ERROR " + e.toString());
        }
        // Clear work (in case we get reused)
        myWorkingColumns = null;

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

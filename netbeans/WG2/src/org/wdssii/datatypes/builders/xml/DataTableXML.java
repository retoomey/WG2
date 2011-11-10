package org.wdssii.datatypes.builders.xml;

import java.util.ArrayList;

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
}

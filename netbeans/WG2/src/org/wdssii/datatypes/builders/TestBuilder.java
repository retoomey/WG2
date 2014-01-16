package org.wdssii.datatypes.builders;

import java.util.ArrayList;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.datatypes.PPIRadialSet.PPIRadialSetMemento;
import org.wdssii.datatypes.builders.test.DataTypeTest;
import org.wdssii.datatypes.builders.test.LatLonHeightGridTest;
import org.wdssii.datatypes.builders.test.RadialSetTest;
import org.wdssii.index.IndexRecord;
import org.wdssii.index.TestIndex;

/**
 *
 * @author Robert Toomey
 */
public class TestBuilder extends Builder {

    private ArrayList<DataTypeTest> myTests = new ArrayList<DataTypeTest>();
    
    public TestBuilder() {
        super("test");
        // Introduce tests we want in a test index
        // Annoying no easy way to just add the test folder classes.
        //myTests.add(new RadialSetTest());
        myTests.add(new LatLonHeightGridTest());
    }

    @Override
    public DataType createDataType(IndexRecord rec, WdssiiJobMonitor w) {
        PPIRadialSetMemento m = new PPIRadialSetMemento();
        return new PPIRadialSet(m);
    }

    @Override
    public DataRequest createDataRequest(IndexRecord rec) {
        DataRequest dr = new DataRequest();
        
        // FIXME: need something in record to tell us what test object to use
        //RadialSetTest t = new RadialSetTest();
        LatLonHeightGridTest t = new LatLonHeightGridTest();
        DataType d = t.createTest(rec, false);
        dr.setReady(d);
        return dr;
    }

    /** Create the fake records for the index */
    public void createFakeRecords(TestIndex index) {

        for(DataTypeTest d:myTests){
            d.createFakeRecords(index);
        }
    }

}

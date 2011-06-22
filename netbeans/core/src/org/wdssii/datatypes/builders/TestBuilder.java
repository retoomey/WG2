package org.wdssii.datatypes.builders;

import java.util.ArrayList;
import java.util.Date;
import org.wdssii.datatypes.DataRequest;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.RadialSet;
import org.wdssii.datatypes.RadialSet.RadialSetMemento;
import org.wdssii.datatypes.builders.test.DataTypeTest;
import org.wdssii.datatypes.builders.test.RadialSetTest;
import org.wdssii.index.IndexRecord;
import org.wdssii.index.TestIndex;

/**
 *
 * @author Robert Toomey
 */
public class TestBuilder implements Builder {

    private ArrayList<DataTypeTest> myTests = new ArrayList<DataTypeTest>();
    
    public TestBuilder() {
        
        // Introduce tests we want in a test index
        // Annoying no easy way to just add the test folder classes.
        myTests.add(new RadialSetTest());
    }

    @Override
    public DataType createObject(IndexRecord rec) {
        RadialSetMemento m = new RadialSetMemento();
        return new RadialSet(m);
    }

    @Override
    public DataRequest createObjectBackground(IndexRecord rec) {
        DataRequest dr = new DataRequest();
        RadialSetTest t = new RadialSetTest();
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

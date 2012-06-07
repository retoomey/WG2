package org.wdssii.index;

import java.net.URL;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.DataUnavailableException;
import org.wdssii.datatypes.builders.Builder;
import org.wdssii.datatypes.builders.BuilderFactory;
import org.wdssii.datatypes.builders.TestBuilder;

/**
 * Test index is designed to create a fake index that
 * has a fake entry for all of the DataTypes that we handle.  This
 * is for testing in the GUI.  Guess you could use it to test
 * algorithms as well
 * 
 * @author Robert Toomey
 */
public class TestIndex extends Index {

    private static Logger log = LoggerFactory.getLogger(TestIndex.class);

    @Override
    public Index newInstance(URL path, URL full, TreeMap<String, String> params, Set<IndexRecordListener> listeners)
            throws DataUnavailableException {
        return new TestIndex(path, listeners);
    }

    /** meant for prototype factory use only. */
    public TestIndex() {
        super(null, null);
    }

    @Override
    public void update() {
    }

    public TestIndex(URL aURL, Set<IndexRecordListener> listeners)
            throws DataUnavailableException {

        super(Index.getParent(aURL), listeners);

        // Ok create all the 'test' objects by reflection...
        Builder aBuilder = BuilderFactory.getBuilder("test");
        if (aBuilder instanceof TestBuilder) {
            TestBuilder tester = (TestBuilder)(aBuilder);
            tester.createFakeRecords(this);
                     
        }
    }

    @Override
    public boolean checkURL(String protocol, URL url, URL fullurl, TreeMap<String, String> paramMap) {
        return true;
    }

    @Override
    public void loadInitialRecords(){}
}

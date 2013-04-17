package org.wdssii.datatypes.builders.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.datatypes.LatLonHeightGrid;
import org.wdssii.datatypes.LatLonHeightGrid.LatLonHeightGridMemento;
import org.wdssii.index.Index;
import org.wdssii.index.IndexRecord;
import org.wdssii.storage.Array3D;
import org.wdssii.storage.Array3DfloatRAM;

/**
 *  Create a collection of dynamically created test RadialSets
 * 
 * @author Robert Toomey
 */
public class LatLonHeightGridTest extends DataTypeTest {

    /** The fake elevations we create */
    //  private String[] myFakeElevs = new String[]{"00.50", "01.45",
    //      "02.40", "03.35", "04.30", "05.25", "06.20", "07.50",
    //      "08.70", "10.00", "12.00", "14.00", "16.70", "19.50"};
    /** Number of heights in the LatLonHeightGrid */
    private int myNumberHeights = 30;
    /** The seconds per fake data */
    private int mySecondsPerElev = 30;
    /** The number of volumes to make */
    private int myNumVolumes = 2;

    @Override
    public DataType createTest(IndexRecord sourceRecord, boolean sparse) {
        LatLonHeightGridMemento m = new LatLonHeightGridMemento();
        m.datametric = LatLonHeightGrid.createDataMetric();
        this.fill(sourceRecord, m, sparse);
        return new LatLonHeightGrid(m);
    }

    /** Fill a memento */
    @Override
    public void fill(IndexRecord sourceRecord, DataTypeMemento m, boolean sparse) {
        super.fill(sourceRecord, m, sparse);

        m.attriNameToValue.put("Unit", "dbZ");

        int num_heights = myNumberHeights;
        int num_lons = 50;
        int num_lats = 50;

        // Create 3D array of LatLonHeightGrid
        int toggle = 0;
        Array3D<Float> values = new Array3DfloatRAM(num_heights, num_lats, num_lons, 0.0f);
        if (m.datametric != null) {
            m.datametric.beginArray3D();
        }
        ArrayList<Float> theHeights = new ArrayList<Float>();
        float currentHeightKms = 1000;
        for (int height = 0; height < num_heights; height++) {
            for (int lat = 0; lat < num_lats; lat++) {
                for (int lon = 0; lon < num_lons; lon++) {
                    float value = 80.0f;
                    switch (toggle++) {
                        case 1:
                            value = 20.0f;
                            break;
                        case 2:
                            value = 50.0f;
                            break;
                        case 3:
                            value = 70.0f;
                            break;
                        case 4:
                            value = 35.0f;
                            break;
                        case 5:
                            value = 15.0f;
                            toggle = 0;
                            break;
                    }
                    values.set(height, lat, lon, value);
                    if (m.datametric != null) {
                        m.datametric.updateArray3D(height, lat, lon, value);
                    }
                }
            }
            theHeights.add(currentHeightKms);
            currentHeightKms += 200.0;
        }
        if (m instanceof LatLonHeightGridMemento) {
            LatLonHeightGridMemento ll = (LatLonHeightGridMemento) (m);
            ll.data = values;
            ll.latResDegs = .01f;
            ll.lonResDegs = .01f;
            ll.numLats = 50;
            ll.numLons = 50;
            ll.heightsMeters = theHeights;
        }
    }

    @Override
    public void createFakeRecords(Index index) {
        //String indexLocation = index.getIndexLocation();

        // Subtract mySecondsPerElev from current time for total number
        // of records we're making
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -(mySecondsPerElev * myNumVolumes));
        Date d = cal.getTime();

        //String[] paramList = new String[]{"test"};  // Use the test builder
        String product = "MergerReflectivityQC ";
        String timeString = IndexRecord.getStringFromDate(d) + " ";
        for (int v = 0; v < myNumVolumes; v++) {

           // IndexRecord rec = IndexRecord.createIndexRecord(d, paramList, timeString + product + "full3D", indexLocation);
            //index.addRecord(rec);
            cal.add(Calendar.SECOND, mySecondsPerElev);
            d = cal.getTime();
            timeString = IndexRecord.getStringFromDate(d) + " ";

        }
    }
}

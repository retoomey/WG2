package org.wdssii.datatypes.builders.test;

import java.util.Calendar;
import java.util.Date;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeMemento;
import org.wdssii.datatypes.Radial;
import org.wdssii.datatypes.RadialSet;
import org.wdssii.datatypes.RadialSet.RadialSetMemento;
import org.wdssii.geom.CPoint;
import org.wdssii.geom.CVector;
import org.wdssii.index.Index;
import org.wdssii.index.IndexRecord;
import org.wdssii.storage.Array1Dfloat;
import org.wdssii.storage.Array2Dfloat;
import org.wdssii.storage.Array2DfloatAsTiles;

/**
 *  Create a collection of dynamically created test RadialSets
 * 
 * @author Robert Toomey
 */
public class RadialSetTest extends DataTypeTest {

    /** The fake elevations we create */
    private String[] myFakeElevs = new String[]{"00.50", "01.45",
        "02.40", "03.35", "04.30", "05.25", "06.20", "07.50",
        "08.70", "10.00", "12.00", "14.00", "16.70", "19.50"};
    /** The seconds per fake elevation */
    private int mySecondsPerElev = 30;
    /** The number of volumes to make */
    private int myNumVolumes = 2;

    @Override
    public DataType createTest(IndexRecord sourceRecord, boolean sparse) {
        RadialSetMemento m = new RadialSetMemento();
        m.datametric = RadialSet.createDataMetric();
        this.fill(sourceRecord, m, sparse);
        return new RadialSet(m);
    }

    /** Fill a memento */
    @Override
    public void fill(IndexRecord sourceRecord, DataTypeMemento m, boolean sparse) {
        super.fill(sourceRecord, m, sparse);

        m.attriNameToValue.put("Unit", "dbZ");

        // No idea yet....problem is we'll need 'settings' for
        // how to actually make the RadialSet...
        String[] selections = sourceRecord.getSelections();
        int num_radials = 360;
        int num_gates = 200;
        float elev = new Float(selections[2]);
        float distToFirstGate = 0.023210982f;
        float nyquist = 33.01f;

        // Create 2D array of RadialSet
        int toggle = 0;
        Array2Dfloat values = new Array2DfloatAsTiles(num_radials, num_gates, 0.0f);
        if (m.datametric != null) {
            m.datametric.beginArray2D();
        }
        for (int i = 0; i < num_radials; ++i) {
            for (int j = 0; j < num_gates; ++j) {
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
                values.set(i, j, value);
                if (m.datametric != null) {
                    m.datametric.updateArray2D(i, j, value);
                }
            }
        }
        if (m instanceof RadialSetMemento) {
            RadialSetMemento r = (RadialSetMemento) (m);

            r.elevation = elev;
            r.rangeToFirstGate = distToFirstGate / 1000;
            r.radarLocation = r.originLocation.getCPoint();
            r.myUz = r.radarLocation.minus(new CPoint(0, 0, 0)).unit();
            r.myUx = new CVector(0, 0, 1).crossProduct(r.myUz).unit();
            r.myUy = r.myUz.crossProduct(r.myUx);

            r.radials = new Radial[num_radials];
            float az = 0;
            float bw = 1.0f;
            float as = 1.0f;
            float gw = 1.0f;
            float ny = nyquist;
            float deltaAz = 360 / num_radials;

            for (int i = 0; i < num_radials; ++i) {
                // This wraps around the column of the 2D array, _not_ a copy
                Array1Dfloat col = values.getCol(i);
                if (col != null) {
                    if (r.maxGateNumber < col.size()) {
                        r.maxGateNumber = col.size();
                    }
                }
                r.radials[i] = new Radial(az, bw, as, gw, ny, col, i);
                az += deltaAz;
            }
        }
    }

    @Override
    public void createFakeRecords(Index index) {
        String indexLocation = index.getIndexLocation();

        // Subtract mySecondsPerElev from current time for total number
        // of records we're making
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -(mySecondsPerElev * myFakeElevs.length * myNumVolumes));
        Date d = cal.getTime();

        String[] paramList = new String[]{"test"};  // Use the test builder
        String product = "Reflectivity ";
        String timeString = IndexRecord.getStringFromDate(d) + " ";
        for (int v = 0; v < myNumVolumes; v++) {
            for (String e : myFakeElevs) {

           //     IndexRecord rec = IndexRecord.createIndexRecord(d, paramList, timeString + product + e, indexLocation);
            //    index.addRecord(rec);
                cal.add(Calendar.SECOND, mySecondsPerElev);
                d = cal.getTime();
                timeString = IndexRecord.getStringFromDate(d) + " ";
            }
        }
    }
}

package org.wdssii.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.wdssii.index.HistoricalIndex;
import org.wdssii.index.IndexRecord;

import junit.framework.TestCase;

public class XMLIndexTest extends TestCase {

    private File file = new File("testindex.xml.gz");
    private HistoricalIndex index;

    @Override
    protected void setUp() throws Exception {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new GZIPOutputStream(new FileOutputStream(file))));
        writer.println("<codeindex>"
                + "<item>"
                + "<time fractional=\"0.593000\"> 990376534 </time>"
                + "<params>netcdf {indexlocation} Reflectivity 00.50 20010520-163534.netcdf.gz </params>"
                + "<selections>20010520-163534 Reflectivity 00.50 </selections>"
                + "</item>"
                + "<item>"
                + "<time fractional=\"0.730000\"> 990376608 </time>"
                + "<params>netcdf {indexlocation} Velocity 00.50 20010520-163648.netcdf.gz </params>"
                + "<selections>20010520-163648 Velocity 00.50 </selections>"
                + "</item>"
                + "<item>"
                + "<time fractional=\"0.499000\"> 990376689 </time>"
                + "<params>netcdf {indexlocation} Reflectivity 01.50 20010520-163809.netcdf.gz </params>"
                + "<selections>20010520-163809 Reflectivity 01.50 </selections>"
                + "</item>"
                + "<item>"
                + "<time fractional=\"0.637000\"> 990376763 </time>"
                + "<params>netcdf {indexlocation} Velocity 01.50 20010520-163923.netcdf.gz </params>"
                + "<selections>20010520-163923 Velocity 01.50 </selections>"
                + "</item>" + "</codeindex>");
        writer.close();
        index = new HistoricalIndex("xml:" + file.getAbsolutePath(), 0);
        file.delete();
    }

    public void testGetDataTypes() {
        Set<String> dataTypes = index.getDataTypes();
        List<String> expected = new ArrayList<String>();
        expected.add("Reflectivity");
        expected.add("Velocity");
        assertEquals(dataTypes.size(), expected.size());
        assertTrue(dataTypes.containsAll(expected));
    }

    public void testGetFirstRecordByTime() {
        String dt = "Reflectivity";
        IndexRecord rec = index.getFirstRecordByTime(dt, "00.50");
        assertEquals(rec.getDataType(), dt);
        assertEquals(rec.getTime().getTime(), new Date(990376534L * 1000 + 593).getTime());
    }

    public void testGetLastRecordByTime() {
        String dt = "Reflectivity";
        IndexRecord rec = index.getLastRecordByTime(dt);
        assertEquals(rec.getDataType(), dt);
        assertEquals(rec.getTime().getTime(), new Date(990376689L * 1000 + 499).getTime());
    }

    public void testRelativePath() throws java.io.IOException {
        String dir = file.getCanonicalFile().getParent();
        String dt = "Reflectivity";
        IndexRecord rec = index.getLastRecordByTime(dt);
        String[] params = rec.getParams();
        boolean found = false;
        for (String param : params) {
            if (param.equals(dir)) {
                found = true;
            }
        }
        assertTrue(found);
    }
}

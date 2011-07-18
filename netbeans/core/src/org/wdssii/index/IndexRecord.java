package org.wdssii.index;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.wdssii.util.StringUtil;

/**
 * @author lakshman
 * 
 */
public class IndexRecord {

    private Date time;
    private String[] selections;
    private String[][] paramsArray;
    private String sourceName = null;
    private static Format myFormatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
    private URL myURL = null;
    private boolean myTryToMakeURL = true;

    public void setSourceName(String s) {
        sourceName = s;
    }

    /** returns a source name if one was set. */
    public String getSourceName() {
        return sourceName;
    }

    public IndexRecord(Date time, String[] selections, String[][] params) {
        this.time = time;
        if (selections.length != 3) {
            throw new IllegalArgumentException("Require 3 selections");
        }
        this.selections = selections;

        paramsArray = params;
    }

    public IndexRecord(Date time, String[] selections, String[] params) {
        this(time, selections, new String[][]{params});
    }

    @Deprecated
    private String[] getParams(int index) {
        return paramsArray[index];
    }
    
    @Deprecated
    private String[] getParams() {
        return getParams(0);
    }

    public String[] getSelections() {
        return selections;
    }

    public Date getTime() {
        return time;
    }

    public int getNumParams() {
        return (paramsArray.length);
    }

    /** Return a string such as 'netcdf' that tells what builder to use
     * to make the datatype for this record
     * @return a builder name for builder factory
     */
    public String getBuilderName() {
        String builderName = getParams()[0];
        // Legacy support.  We now use URI for data location
        if (builderName.equals("http")) {
            builderName = "webindex";
        }
        return builderName;
    }

    /** Convert from our xml date format to a real Date */
    public static Date getDateFromString(String timeString, String frac) {
        long tm_long = 1000 * Long.parseLong(timeString.trim());
        if (frac != null) {
            double ftm = Double.parseDouble(frac);
            tm_long += (int) Math.round(1000 * ftm);
        }
        Date time = new Date(tm_long);
        return time;
    }

    /** Convert from date into our xml date format */
    public static String getStringFromDate(Date in) {
        String s = myFormatter.format(in);
        return s;
    }

    public static IndexRecord createIndexRecord(Date time, String[] paramsList, String[] paramsChanges, String selectionsString, String indexLocation) {
        // time
        // Date time = getDateFromString(timeString, timeFrac);

        // selections
        List<String> selectionList = StringUtil.split(selectionsString.trim());
        if (selectionList.size() == 2) {
            selectionList.add("");
        }
        String[] selections = selectionList.toArray(new String[0]);

        // params
        List<List<String>> paramsArray = new ArrayList<List<String>>();
        List<String> p1 = null, p2 = null;
        for (int i = 0; i < paramsList.length; ++i) {
            List<String> p = StringUtil.split(paramsList[i].trim());
            if (i == 0) {
                p1 = new ArrayList<String>(p);
                p2 = p;
                paramsArray.add(p);
                continue;
            }
            String changes = paramsChanges[i];
            if (changes == null || changes == "") {
                paramsArray.add(p);
                continue;
            }
            StringTokenizer st = new StringTokenizer(changes);
            int seq = 0;
            p2 = new ArrayList<String>(p1);
            while (st.hasMoreTokens()) {
                int pos = (new Integer(st.nextToken())).intValue();
                p2.set(pos, p.get(seq++));
            }
            paramsArray.add(p2);
        }

        String[][] aparams = new String[paramsArray.size()][];
        for (int j = 0; j < paramsArray.size(); ++j) {
            List<String> params = paramsArray.get(j);
            for (int i = 0; i < params.size(); ++i) {
                if (params.get(i).equals("{indexlocation}")) {
                    params.set(i, indexLocation);
                }
            }
            aparams[j] = params.toArray(new String[0]);
        }

        return new IndexRecord(time, selections, aparams);
    }

    /** Set the URL for the file/stuff that this record ultimately points to
     */
    public void setDataLocationURL(URL aURL) {
        myURL = aURL;
    }

    /** Get the data location url from the first param of the record.
     * We're not handling multiple params yet
     * @return the URL of the data location, or null.
     */
    public URL getDataLocationURL() {

        if ((myURL == null) && myTryToMakeURL) {
            myURL = tryCreateURLFromParams();
            myTryToMakeURL = false;
        }
        return myURL;
    }

    /** Create the data location url from the first param of the record.
     * We're not handling multiple params yet
     * @return the URL of the data location, or null.
     */
    private URL tryCreateURLFromParams() {
        String[] params = getParams();

        // Params 0 are of this form for a regular index:
        // 0 - builder name such as 'netcdf'
        // 1 - indexLocation such as C:/KTLX/
        // 2 - Product such as 'Reflectivity'
        // 3 - Choice such as '05.25'
        // 4 - short file such as '1999_ktlx.netcdf.gz'
        StringBuilder path = new StringBuilder(params[1]);
        for (int i = 2; i < params.length; ++i) {
            path.append('/').append(params[i]);
        }

        URL url = null;
        try {
            url = new URL(path.toString());
        } catch (MalformedURLException e) {
        }
        return url;
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("IndexRecord: ");
        for (int i = 0; i < selections.length; ++i) {
            b.append(selections[i]);
        }
        b.append(" at " + time);
        return b.toString();
    }

    public String toXMLString() {
        StringBuffer b = new StringBuffer();
        b.append("<item>\n");
        double fraction = time.getTime() % 1000 / (double) 1000;
        b.append("<time fractional=\"" + String.valueOf(fraction) + "\">"
                + String.valueOf(time.getTime() / 1000) + "</time>\n");
        for (int i = 0; i < paramsArray.length; i++) {
            b.append("<params>");
            String[] al = paramsArray[i];
            for (int j = 0; j < al.length; j++) {
                b.append(al[j]).append(" ");
            }
            b.append("</params>\n");
        }
        b.append("<selections>");
        for (int j = 0; j < selections.length; j++) {
            b.append(selections[j]).append(" ");
        }
        b.append("</selections>\n");
        b.append("</item>\n");
        return (b.toString());
    }

    public String getDataType() {
        return selections[1];
    }

    /** An empty string if this record has no subtype */
    public String getSubType() {
        return selections[2];
    }

    public String getTimeStamp() {
        return selections[0];
    }
}

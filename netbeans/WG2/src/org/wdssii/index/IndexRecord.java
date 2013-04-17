package org.wdssii.index;

import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.datatypes.builders.Builder;
import org.wdssii.datatypes.builders.BuilderFactory;
import org.wdssii.util.StringUtil;

/**
 * @author lakshman
 * 
 */
public class IndexRecord {

    private final static Logger LOG = LoggerFactory.getLogger(IndexRecord.class);
	
    // Data format for all index records
    private static Format myFormatter = new SimpleDateFormat("yyyyMMdd-HHmmss");

    /** The timestamp of this record */
    private Date time;

    /** The hierarchy of selections used to choose this record */
    private String[] selections;

    /** The sourcename... FIXME: probably shouldn't use this it's flaky */
    private String sourceName = null;

    /** the URL of the data file */
    private URL myURL = null;

    /** the builder for creating the datatype for this record */
    private Builder myBuilder;

    public void setSourceName(String s) {
        sourceName = s;
    }

    /** returns a source name if one was set. */
    public String getSourceName() {
        return sourceName;
    }

    public IndexRecord(Date time, Builder b, String[] selections, URL location) {
        this.time = time;
	this.myBuilder = b;
	this.myURL = location;
        if (selections.length != 3) {
            throw new IllegalArgumentException("Require 3 selections");
        }
        this.selections = selections;
    }

    public String[] getSelections() {
        return selections;
    }

    public Date getTime() {
        return time;
    }

    public Builder getBuilder() {
	    return myBuilder;
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
    
    /** Convert from our xml date format to a real Date */
    public static Date getDateFromString(String timeString, double ftm) {
        long tm_long = 1000 * Long.parseLong(timeString.trim());
        tm_long += (int) Math.round(1000 * ftm);
        Date time = new Date(tm_long);
        return time;
    }
    

    /** Convert from date into our xml date format */
    public static String getStringFromDate(Date in) {
        String s = myFormatter.format(in);
        return s;
    }

    public static IndexRecord createIndexRecord(Date time, String builderName, String builderParams,  String selectionsString, String indexLocation) {

        // selections (used to categorize record within groups)
        List<String> selectionList = StringUtil.split(selectionsString.trim());
        if (selectionList.size() == 2) {
            selectionList.add("Default");
        }
        String[] selections = selectionList.toArray(new String[0]);

	// params (buildername) + unknown params
	// Only the builder knows how to handle the rest of params, the first
	// param is the buildername
	Builder b = BuilderFactory.getBuilder(builderName);
        if (b == null) {
            LOG.error("Can't create record, no builder named " + builderName);
            return null;
        }
        URL aURL = b.createURLForParams(builderParams, indexLocation);

        return new IndexRecord(time, b, selections, aURL);
    }

    /** Set the URL for the file/stuff that this record ultimately points to
     */
    public void setDataLocationURL(URL aURL) {
        myURL = aURL;
    }

    /** Get the data location url from the first param of the record.
     * We're not handling multiple params yet, if ever.
     * We pass in the builder because our params are in different order/
     * meaning depending upon our builder, which is annoying.
     * 
     * @return the URL of the data location, or null.
     */
    public URL getDataLocationURL(Builder builder) {
	   return myURL;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("IndexRecord: ");
        for (int i = 0; i < selections.length; ++i) {
            b.append(selections[i]);
	    b.append(" ");
        }
        b.append(" at " + time);
        return b.toString();
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

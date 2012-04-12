package org.wdssii.xml.index;

import java.util.Date;
import org.wdssii.xml.Tag;

/**
 *
 * @author Robert Toomey
 */
public class Tag_time extends Tag {
    public double fractional=0.0f;   
    
    /** Not reflection called, created when validateTag is called */
    public Date date;
    
    /** At the end of a time tag, store as a date */
    @Override
    public void validateTag(){
        
        // Calculate date here, this way xml handles the format
        long tm_long = 1000 * Long.parseLong(getText().trim());
        releaseText();
        tm_long += (int) Math.round(1000 * fractional);
        date = new Date(tm_long);   
    }
}

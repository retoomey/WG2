package org.wdssii.xml.wdssiiConfig;

import java.util.ArrayList;
import org.wdssii.xml.Tag;

/**
 *
 * @author Robert Toomey
 */
public class Tag_charts extends Tag {
    
    // ----------------------------------------------------------------------
    // Reflection <charts selected=
    // Attributes
    public String selected="VSlice";
    // Subtags
    public ArrayList<Tag_chart> charts = new ArrayList<Tag_chart>();
    
    /** Holds one chart info in xml */
    public static class Tag_chart extends Tag {
        // Reflection
        public String name;
        public boolean show = false;
        public String gName;
        
        @Override
        public void validateTag(){
           if (gName == null){
               if (name != null){  // Use the raw name if gui name missing
                   gName = name;
               }else{
                   gName = "Bad chart info in xml";
               }
           }
        }
    }
}

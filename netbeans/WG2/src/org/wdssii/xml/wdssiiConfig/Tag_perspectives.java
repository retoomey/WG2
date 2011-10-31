package org.wdssii.xml.wdssiiConfig;

import java.util.ArrayList;
import org.wdssii.xml.Tag;

/**
 *
 * @author Robert Toomey
 */
public class Tag_perspectives extends Tag {

    public String show = "Basic";
    public ArrayList<Tag_perspective> perspectives = new ArrayList<Tag_perspective>();

    public static class Tag_perspective extends Tag {

        public String className;
        public boolean loadOnStartup = false;
    }
}

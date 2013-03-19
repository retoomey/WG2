package org.wdssii.xml.w2bookmarks;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "w2rlist")
public class SourceBookmarkList {

    @XmlElement(name = "index")
    public List<SourceBookmark> list = new ArrayList<SourceBookmark>();
}

package org.wdssii.xml.w2bookmarks;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/*
 * Old tag which has the following format:
 *
 * <pre>
 * {@code
 *  <w2rindex>
 *   <w2rlist>
 *     <index .... >
 *   </w2rlist>
 *  </w2rindex>
 * }
 * </pre>
 */
@XmlRootElement(name="w2rindex")
public class W2Bookmarks {
    
    @XmlAttribute(name="w2rlist")
    public SourceBookmarkList list;
}

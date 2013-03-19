package org.wdssii.xml.config;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Tag which has the following format:
 *
 * <pre>
 * {@code
 *  <w2pref>
 *    <sources>
 *    </sources>
 *  </w2pref>
 * }
 * </pre>
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "w2pref")
public class W2Pref {
    @XmlElement(name = "sources")
    public Sources sources;
}

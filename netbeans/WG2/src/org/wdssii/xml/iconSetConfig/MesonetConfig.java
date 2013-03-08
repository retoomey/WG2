package org.wdssii.xml.iconSetConfig;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Tag which has the following format:
 *
 * <pre>
 * {@code
 * <mesonetConfig>
 *  <dataColumn>
 * </mesonetConfig>
 * }
 * </pre>
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "mesonetconfig")
public class MesonetConfig {

    @XmlElement(name = "datacolumn")
    public DataColumn dataColumn = new DataColumn();
    @XmlElement(name = "windbarb")
    public WindBarb windBarb = new WindBarb();
    @XmlElement(name = "output")
    public Output output = new Output();
}
